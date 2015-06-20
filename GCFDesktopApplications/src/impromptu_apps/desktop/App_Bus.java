package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import java.util.ArrayList;

import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;

public class App_Bus extends DesktopApplicationProvider
{
	public static final double MIN_DISTANCE_IN_KM = 0.25;
	
	// Stores Coordinates
	private ArrayList<Location> locations;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 */
	public App_Bus(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"BUSSTOP",
				"Bus Stop Application",
				"Gives you relevant bus stop information given your current location.",
				"TRANSPORTATION",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://files.softicons.com/download/web-icons/awt-travel-blue-icons-by-awt-media/png/200x200/AWT-Bus.png",				   // LOGO
				120,
				commMode,
				ipAddress,
				port);
		
		locations = new ArrayList<Location>();
		
		populateLocations();
	}
	
	private void populateLocations()
	{
		// Populates coordinates
		locations.add(new Location("Ardmore Blvd", 40.424654, -79.859158, "http://truetime.portauthority.org/bustime/wireless/html/eta.jsp?route=69&direction=INBOUND&id=7787&showAllBusses=on"));
		locations.add(new Location("Forbes Ave (Near NSH)", 40.443608, -79.945573, "http://truetime.portauthority.org/bustime/wireless/html/eta.jsp?route=67&direction=OUTBOUND&id=7116&showAllBusses=on"));
		locations.add(new Location("Forbes Ave (Near University Center)", 40.44449351, -79.7425490200, "http://truetime.portauthority.org/bustime/wireless/html/eta.jsp?route=67&direction=OUTBOUND&id=7117&showAllBusses=on"));
	}
	
	private Location getNearestLocation(JSONContextParser parser, double threshold)
	{
		Location bestLocation = null;
		double   bestDistance = Double.MAX_VALUE;
		
		for (Location location : locations)
		{
			double distance = this.getDistance(parser, location.latitude, location.longitude);
			
			if (distance < threshold && distance < bestDistance)
			{
				bestLocation = location;
				bestDistance = distance;
			}
		}
		
		return bestLocation;
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String  		  context 		  = CommMessage.getValue(subscription.getParameters(), "context");
		JSONContextParser parser  		  = new JSONContextParser(JSONContextParser.JSON_TEXT, context);
		Location 	      closestLocation = getNearestLocation(parser, MIN_DISTANCE_IN_KM);
		
		if (closestLocation != null)
		{
			return new String[] { "WEBSITE=" + closestLocation.url, "FORCE=true" };
		}
		else
		{
			return new String[] { "WEBSITE=http://truetime.portauthority.org/bustime/wireless/html/", "FORCE=true" };
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
		JSONContextParser parser 		  = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		Location 		  nearestLocation = getNearestLocation(parser, MIN_DISTANCE_IN_KM);
		
		return nearestLocation != null;
	}
	
	public String getDescription(String userContextJSON)
	{
		JSONContextParser parser 		  = new JSONContextParser(JSONContextParser.JSON_TEXT, userContextJSON);
		Location 		  nearestLocation = getNearestLocation(parser, MIN_DISTANCE_IN_KM);
		
		if (nearestLocation != null)
		{
			return nearestLocation.description;
		}
		else
		{
			return description;
		}
	}
	
	private class Location
	{
		public String description;
		public String url;
		public double latitude;
		public double longitude;
		
		public Location(String description, double latitude, double longitude, String url)
		{
			this.description = description;
			this.latitude    = latitude;
			this.longitude   = longitude;
			this.url 		 = url;
		}
	}
}
