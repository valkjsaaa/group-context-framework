package liveos_apps;

import java.awt.geom.Point2D;
import java.util.HashMap;

import bluetoothcontext.toolkit.JSONContextParser;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;

public class App_Bus extends DesktopApplicationProvider
{
	// Stores Coordinates
	private HashMap<String, Point2D.Double> coordinates = new HashMap<String, Point2D.Double>();
	
	public App_Bus(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"BUSSTOP",
				"Bus Stop Application",
				"Gives you relevant bus stop information given your current location.",
				"TRANSPORTATION",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://findthestop.co.uk/img/bus-stop.jpg",				   // LOGO
				60,
				commMode,
				ipAddress,
				port);
		
		// Populates coordinates
		// NOTE:  I am using X, Y for Longitude, Latitude (the reverse of what you normally think)
		coordinates.put("Home", new Point2D.Double(40.4338964, -79.8537098));
		coordinates.put("NSH", new Point2D.Double(40.443608, -79.945573));
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
		
		if (closestLocation.equals("NSH"))
		{
			return new String[] { "WEBSITE=http://truetime.portauthority.org/bustime/wireless/html/eta.jsp?route=67&direction=OUTBOUND&id=7116&showAllBusses=on", "FORCE=true" };	
		}
		else if (closestLocation.equals("Home"))
		{
			return new String[] { "WEBSITE=http://truetime.portauthority.org/bustime/wireless/html/eta.jsp?route=69&direction=INBOUND&id=7787&showAllBusses=on", "FORCE=true" };	
		}
		else
		{
			return new String[] { "WEBSITE=http://truetime.portauthority.org/bustime/wireless/html/selectdirection.jsp?route=69", "FORCE=true" };
		}
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	@Override
	public boolean sendAppData(String json)
	{
		String bestName     = "";
		double bestDistance = Double.MAX_VALUE;
		
		for (String locationName : coordinates.keySet())
		{
			Point2D.Double location = coordinates.get(locationName);
			double 		   distance = this.getDistance(new JSONContextParser(JSONContextParser.JSON_TEXT, json), location.x, location.y);
			
			if (distance < bestDistance)
			{
				bestName     = locationName;
				bestDistance = distance;
			}
		}
		
		return bestDistance < 0.25;
	}
}
