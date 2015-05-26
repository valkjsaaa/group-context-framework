package liveos_apps;

import java.awt.geom.Point2D;
import java.util.HashMap;


import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;

public class App_Target extends DesktopApplicationProvider
{
	// Stores Coordinates
	private HashMap<String, Point2D.Double> coordinates = new HashMap<String, Point2D.Double>();
	
	public App_Target(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"TARGET_AD",
				"Target Application",
				"Gives you the weekly ad for this store.",
				"SHOPPING",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://www.mcm.org/uploads/images/logos/TargetLogo.jpg", // LOGO
				120,
				commMode,
				ipAddress,
				port);
		
		// Populates coordinates
		// NOTE:  I am using X, Y for Longitude, Latitude (the reverse of what you normally think)
		coordinates.put("Monroeville Target", new Point2D.Double(40.433955,-79.772115));
		coordinates.put("Penn Ave. Target", new Point2D.Double(40.461107,-79.921803));
		coordinates.put("Waterfront Target", new Point2D.Double(40.410604, -79.910367));
//		coordinates.put("Home", new Point2D.Double(40.434090, -79.853565));
//		coordinates.put("NSH", new Point2D.Double(40.443608, -79.945573));
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
		
		return new String[] { "WEBSITE=http://m.weeklyad.target.com/" };
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
