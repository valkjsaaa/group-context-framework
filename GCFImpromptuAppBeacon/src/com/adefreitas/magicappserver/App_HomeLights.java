package com.adefreitas.magicappserver;

import java.util.Calendar;

import android.widget.Toast;

import com.adefreitas.androidframework.toolkit.HttpToolkit;
import com.adefreitas.androidliveos.AndroidApplicationProvider;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_HomeLights extends AndroidApplicationProvider
{	
	private boolean porchLightsOn    = false;
	private boolean basementLightsOn = false;
	
	private HttpToolkit toolkit;
	
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
	public App_HomeLights(GCFApplication application, GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(	groupContextManager, 
				"HOME_LIGHT", 
				"Home Lighting System", 
				"Controls the Porch and Basement Lighting System (Thank You Phillips Hue!).",
				"AUTOMATION",
				new String[] { }, 
				new String[] { }, 
				"http://108.32.88.8/gcf/universalremote/magic/lights.png",
				30,
				commMode, 
				ipAddress, 
				port);
		
		toolkit = new HttpToolkit(application);
		
		// Enables Light Automation (with specified evening/morning times)
		enableLightAutomation(18, 6);
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

		System.out.println(ui);
		
		//return new String[] { "UI=" + ui };
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/lights/index.html" };
	}

	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		//Toast.makeText(this.getApplication(), newSubscription.getDeviceID() + " has subscribed.", Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		super.onSubscriptionCancelation(subscription);
		
		//Toast.makeText(this.getApplication(), subscription.getDeviceID() + " has unsubscribed.", Toast.LENGTH_LONG).show();
	}
	
	/**
	 * Determines Whether or Not to Send Application Data
	 */
	@Override
	public boolean sendAppData(String bluewaveContext)
	{
		return true;
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
			if (instruction.getPayload()[0].equalsIgnoreCase("on"))
			{
				setLight(1, true);
				setLight(2, true);
			}
			else
			{
				setLight(1, false);
				setLight(2, false);
			}
			
//			System.out.println("PORCH LIGHTS: " + porchLightsOn);
//			
//			if (porchLightsOn)
//			{
//				setLight(1, true);
//				setLight(2, true);
//			}
//			else
//			{
//				setLight(1, false);
//				setLight(2, false);
//			}
		}
		else if (instruction.getCommand().equalsIgnoreCase("basement"))
		{
			if (instruction.getPayload()[0].equalsIgnoreCase("on"))
			{
				setLight(3, true);
			}
			else
			{
				setLight(3, false);
			}
			
//			System.out.println("LIVING ROOM LIGHTS: " + porchLightsOn);
//			
//			if (basementLightsOn)
//			{
//				setLight(3, true);
//			}
//			else
//			{
//				setLight(3, false);
//			}
		}
		
		//this.sendMostRecentReading();
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
		
		toolkit.put(url, json, callback);
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
