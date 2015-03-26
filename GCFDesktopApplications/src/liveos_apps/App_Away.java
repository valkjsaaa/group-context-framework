package liveos_apps;

import java.awt.geom.Point2D;
import java.util.HashMap;

import bluetoothcontext.toolkit.JSONContextParser;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;

public class App_Away extends DesktopApplicationProvider
{
	// Stores Coordinates
	private HashMap<String, Point2D.Double> coordinates = new HashMap<String, Point2D.Double>();
	
	public App_Away(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"AWAY",
				"Away Application",
				"A test application.  It is available when you are not at home!",
				"DEBUG",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"",				   // LOGO
				30,
				commMode,
				ipAddress,
				port);
		
		// Populates coordinates
		// NOTE:  I am using X, Y for Longitude, Latitude (the reverse of what you normally think)
		coordinates.put("Home", new Point2D.Double(40.4338964, -79.8537098));
		coordinates.put("NSH", new Point2D.Double(40.443608, -79.945573));
		coordinates.put("Monroeville Mall", new Point2D.Double(40.430456, -79.794841));
		coordinates.put("Waterfront", new Point2D.Double(40.407453, -79.916514));
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
//		String ui  = "<html><title>Away From House App</title>";
//		ui 		  += "<h4>Updated: " + new Date() + ".</h4></html>";
//		ui 		  += "<h4>Away from the House Test</h4></html>";
//		ui	      += "<div>This is a test application for when you are away from the house!</div>";
//				
//		String 			  context = CommMessage.getValue(subscription.getParameters(), "context");
//		JSONContextParser parser  = new JSONContextParser(JSONContextParser.JSON_TEXT, context);
//		
//		ui += "<h4>Closest Point</h4>";
//		ui += "<div>" + getNearestLocation(parser) + "</div>";
//				
//		ui += "<h4>Your Context</h4><div>" + context + "</div>";
//		
//		return new String[] { "UI=" + ui };
		
		String 			  context 		  = CommMessage.getValue(subscription.getParameters(), "context");
		JSONContextParser parser  		  = new JSONContextParser(JSONContextParser.JSON_TEXT, context);
		String  		  closestLocation = getNearestLocation(parser);
	
		if (closestLocation.equalsIgnoreCase("Waterfront"))
		{
			return new String[] { "WEBSITE=http://www.waterfrontpgh.com/shop/" };
		}
		else if (closestLocation.equalsIgnoreCase("Home"))
		{
			return new String[] { "WEBSITE=http://www.giantbomb.com/" };
		}
		else if (closestLocation.equalsIgnoreCase("Monroeville Mall"))
		{
			return new String[] { "WEBSITE=http://www.monroevillemall.com/" };
		}
		else if (closestLocation.equalsIgnoreCase("NSH"))
		{
			return new String[] { "WEBSITE=https://login.cmu.edu/idp/Authn/Stateless" };
		}
		
		return new String[] { "UI=The Away App is Confused.  It thinks you are here: " + closestLocation };
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	@Override
	public boolean sendAppData(String json)
	{
		return true;
	}
}
