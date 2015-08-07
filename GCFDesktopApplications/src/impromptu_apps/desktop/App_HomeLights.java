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

public class App_HomeLights extends DesktopApplicationProvider
{		
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
	public App_HomeLights(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super( groupContextManager, 
				"HOME_NEST", 
				"Thermostat Controls", 
				"Controls the thermostat controls in this house.",
				"AUTOMATION",
				new String[] { }, 
				new String[] { }, 
				"https://d3rnbxvnd0hlox.cloudfront.net/images/channels/811416297/icons/on_color_large.png",
				300,
				commMode, 
				ipAddress, 
				port);
		
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
		return new String[] { "PACKAGE=com.nestlabs.android" };
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
		return nearLocation(parser, 0.25) && this.hasEmailAddress(parser, "adrian.defreitas@gmail.com");
	}
	
	/**
	 * Handles Compute Instructions Received from Clients
	 */
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		
	}
}
