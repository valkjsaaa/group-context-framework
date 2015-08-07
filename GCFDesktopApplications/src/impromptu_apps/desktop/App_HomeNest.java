package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import java.awt.geom.Point2D;
import java.util.Calendar;
import java.util.HashMap;


import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.HttpToolkit;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_HomeNest extends DesktopApplicationProvider
{	
	private boolean porchLightsOn    = false;
	private boolean basementLightsOn = false;
	
	// Stores Coordinates
	private HashMap<String, Point2D.Double> coordinates = new HashMap<String, Point2D.Double>();
	
	/**
	 * Constructor
	 * @param application
	 * @param groupContextManager
	 * @param contextType
	 * @param name
	 * @param description
	 * @param contextsRequired
	 * @param preferencesToRequest
	 * @param logoPath
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 */
	public App_HomeNest(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super( groupContextManager, 
				"HOME_LIGHT", 
				"Home Lighting System", 
				"Controls the Porch and Basement Lighting System (Thank You Phillips Hue!).",
				"AUTOMATION",
				new String[] { }, 
				new String[] { }, 
				"http://icons.iconarchive.com/icons/cornmanthe3rd/plex-android/96/lightbulb-icon.png",
				300,
				commMode, 
				ipAddress, 
				port);
		
		// Enables Light Automation (with specified evening/morning times)
		enableLightAutomation(21, 5);
		
		coordinates.put("Home", new Point2D.Double(40.434090, -79.853565));
	}
	
	private boolean nearLocation(JSONContextParser parser, double km)
	{
		double bestDistance = Double.MAX_VALUE;
		
		for (String locationName : coordinates.keySet())
		{
			Point2D.Double location = coordinates.get(locationName);
			double 		   distance = this.getDistance(parser, location.x, location.y);
			
			if (distance < bestDistance)
			{
				bestDistance = distance;
			}
		}
		
		return bestDistance <= km;
	}
	
	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String ui  = "<html>";
		ui		  += "<title>Home Lighting System</title>";
						
		ui += "<h4>Porch Lights";
		ui += "(Status: " + porchLightsOn + ")"; 
		ui += "</h4>";
		ui += "<div><input value=\"Toggle\" type=\"button\" style=\"height:50px; font-size:20px\"";
		ui += "  onclick=\"";
		ui += "    device.sendComputeInstruction('PORCH', []);";
		ui += "    device.toast('Toggling Porch Lights');";
		ui += "\"/></div>";	
		
		ui += "<h4>Living Room Light";
		ui += "(Status: " + basementLightsOn + ")"; 
		ui += "</h4>";
		ui += "<div><input value=\"Toggle\" type=\"button\" style=\"height:50px; font-size:20px\"";
		ui += "  onclick=\"";
		ui += "    device.sendComputeInstruction('BASEMENT', []);";
		ui += "    device.toast('Toggling Living Room Lights');";
		ui += "\"/></div>";		
		
		return new String[] { "UI=" + ui };
	}

	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
	}
	
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		super.onSubscriptionCancelation(subscription);
	}
	
	/**
	 * Determines Whether or Not to Send Application Data
	 */
	@Override
	public boolean sendAppData(String json)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		return nearLocation(parser, 0.25);
	}
	
	/**
	 * Handles Compute Instructions Received from Clients
	 */
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
		
		String json = "";
		
		if (instruction.getCommand().equalsIgnoreCase("porch"))
		{
			if (instruction.getPayload().length > 0)
			{
				if (instruction.getPayload(0).equalsIgnoreCase("on"))
				{
					setLight(1, true);
					setLight(2, true);
				}
				else
				{
					setLight(1, false);
					setLight(2, false);
				}
			}
			else
			{
				if (porchLightsOn)
				{
					setLight(1, true);
					setLight(2, true);
				}
				else
				{
					setLight(1, false);
					setLight(2, false);
				}
				
				porchLightsOn = !porchLightsOn;
			}
		}
		else if (instruction.getCommand().equalsIgnoreCase("basement"))
		{
			if (instruction.getPayload().length > 0)
			{
				if (instruction.getPayload(0).equalsIgnoreCase("on"))
				{
					setLight(3, true);
				}
				else
				{
					setLight(3, false);
				}
			}
			else
			{
				if (basementLightsOn)
				{
					setLight(3, true);
				}
				else
				{
					setLight(3, false);
				}	
				
				basementLightsOn = !basementLightsOn;
			}
		}
		
		this.sendContext();
	}

	/**
	 * Sets a Light Status
	 * @param lightID
	 * @param on
	 */
	private void setLight(int lightID, boolean on)
	{
		String url      = String.format("http://192.168.1.25/api/gcfdeveloper/lights/%d/state", lightID);
		String json     = String.format("{\"on\":%s, \"sat\":0, \"bri\":255,\"hue\":0}", on);
		String callback = String.format("APP_LIGHTS_CHANGED_%d", lightID);
		
		HttpToolkit.put(url, json);
	}
	
	private void enableLightAutomation(final int EVENING, final int MORNING)
	{
		Thread t = new Thread()
		{
			public void run()
			{
				try
				{
					while (true)
					{
						Calendar calendar = Calendar.getInstance();
						
						if (calendar.get(Calendar.HOUR_OF_DAY) >= EVENING || calendar.get(Calendar.HOUR_OF_DAY) <= MORNING)
						{
							
							porchLightsOn = true;
							setLight(1, true);
							setLight(2, true);
						}
						else
						{
							porchLightsOn = false;
							setLight(1, false);
							setLight(2, false);
						}
					
						sleep(60000);	
					}
					
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		};
		
		t.start();
	}
}
