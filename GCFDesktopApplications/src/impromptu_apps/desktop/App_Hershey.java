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

public class App_Hershey extends DesktopApplicationProvider
{
	// Stores Coordinates
	private HashMap<String, Point2D.Double> coordinates = new HashMap<String, Point2D.Double>();
	
	public App_Hershey(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"HER",
				"Hershey Park Application",
				"Shows you the app for the Hershey Park.",
				"SHOPPING",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://www.schlesingerassociates.com/images/hershy_kiss_icon.png", // LOGO
				60,
				commMode,
				ipAddress,
				port);
		
		// Populates coordinates
		// NOTE:  I am using X, Y for Longitude, Latitude (the reverse of what you normally think)
		coordinates.put("Hershey Park", new Point2D.Double(40.2881,-76.6569));
		coordinates.put("Home", new Point2D.Double(40.434090, -79.853565));
		coordinates.put("NSH", new Point2D.Double(40.443608, -79.945573));
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
		
		return new String[] { "WEBSITE=http://www.hersheypark.com/" };
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
		return nearLocation(parser, 2.0);
	}
}
