package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import java.awt.geom.Point2D;
import java.util.HashMap;


import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.messages.CommMessage;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_Starbucks extends DesktopApplicationProvider
{
	// Stores Coordinates
	private HashMap<String, Point2D.Double> coordinates = new HashMap<String, Point2D.Double>();
	
	public App_Starbucks(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STARB",
				"Starbucks App",
				"Launches the Starbucks App (or installs it) to let you pay for your coffee.",
				"SHOPPING",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://2.bp.blogspot.com/-gUEhBpnLdCU/UJibeAya_8I/AAAAAAAAAb4/PXEpLCxJsuI/s1600/Starbucks_Corporation_Logo_2011.svg.png", // LOGO
				120,
				commMode,
				ipAddress,
				port);
		
		// Populates coordinates
		// NOTE:  I am using X, Y for Longitude, Latitude (the reverse of what you normally think)
		coordinates.put("Mall", new Point2D.Double(40.430981,-79.794758));
		coordinates.put("Waterfront", new Point2D.Double(40.407435, -79.917222));
		coordinates.put("Monroeville West", new Point2D.Double(40.429615, -79.810339));
		coordinates.put("Monroeville (Miracle Mile)", new Point2D.Double(40.437900, -79.768467));
		coordinates.put("Forbes/Craig", new Point2D.Double(40.44456700, -79.9485571));
		coordinates.put("Forbes Tower", new Point2D.Double(40.440932, -79.957643));
		coordinates.put("UPMC Shadyside", new Point2D.Double(40.455807, -79.939505));
		coordinates.put("Copeland St", new Point2D.Double(40.451680, -79.934961));
		coordinates.put("Squirrel Hill", new Point2D.Double(40.430686, -79.923124));
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
	
	private String getNearestLocation(JSONContextParser parser)
	{
		String bestName     = "";
		double bestDistance = Double.MAX_VALUE;
		
		for (String locationName : coordinates.keySet())
		{
			Point2D.Double location = coordinates.get(locationName);
			double 		   distance = this.getDistance(parser, location.x, location.y);
			
			if (distance < bestDistance)
			{
				bestName     = locationName;
				bestDistance = distance;
			}
		}
		
		return bestName;
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String  		  context 		  = CommMessage.getValue(subscription.getParameters(), "context");
		JSONContextParser parser  		  = new JSONContextParser(JSONContextParser.JSON_TEXT, context);
		String 			  closestLocation = getNearestLocation(parser);
		
		return new String[] { "PACKAGE=com.starbucks.mobilecard" };
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	@Override
	public boolean sendAppData(String json)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		return nearLocation(parser, 0.25);
	}
}
