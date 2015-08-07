package impromptu_apps.creationfest;

import impromptu_apps.DesktopApplicationProvider;

import java.util.ArrayList;
import java.util.HashMap;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_LocalChat extends DesktopApplicationProvider
{	
	public static final String   CONTEXT_TYPE  	      = "CHAT";
	public static final String   DEFAULT_TITLE 	      = "Local Chat";
	public static final String   DEFAULT_DESCRIPTION  = "This app lets you chat with people who are nearby.";
	public static final String   DEFAULT_CATEGORY     = "COMMUNICATION";
	public static final String[] CONTEXTS_REQUIRED    = new String[] { };
	public static final String[] PREFERENCES_REQUIRED = new String[] { };
	public static final String   DEFAULT_LOGO_PATH    = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/256/chat-2-icon.png";
	public static final int      DEFAULT_LIFETIME	  = 120;
	
	private static final double MAX_DISTANCE_IN_KM = 0.25;
	
	private HashMap<String, JSONContextParser> users;
	private ArrayList<ChatItem> 			   chatLog;
	
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
	public App_LocalChat(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				CONTEXT_TYPE, 
				DEFAULT_TITLE, 
				DEFAULT_DESCRIPTION, 
				DEFAULT_CATEGORY, 
				CONTEXTS_REQUIRED, 
				PREFERENCES_REQUIRED, 
				DEFAULT_LOGO_PATH, 
				DEFAULT_LIFETIME,				
				commMode, ipAddress, port);
		
		users   = new HashMap<String, JSONContextParser>();
		chatLog = new ArrayList<ChatItem>();
	}
		
	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String ui  = "<html>";
		ui		  += "<title>Local Chat</title>";
		ui 		  += "<h3>UI Goes Here</h3>";
		ui        += "</html>";
		
		// You can specify a UI as one of the following:
		// UI=<RAW HTML GOES HERE>
		// WEBSITE=<URL GOES HERE>
		// PACKAGE=<GOOGLE PLAY STORE PACKAGE NAME GOES HERE>
		return new String[] { "UI=" + ui };
	}

	/**
	 * Event Called When a Client Connects
	 */
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		//Toast.makeText(this.getApplication(), newSubscription.getDeviceID() + " has subscribed.", Toast.LENGTH_LONG).show();
	}
	
	/**
	 * Event Called with a Client Disconnects
	 */
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
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, bluewaveContext);
		users.put(this.getDeviceID(parser), parser);
		
		// TODO:  Specify the EXACT conditions when this app should appear
		return getNearbyDevices(this.getDeviceID(parser)).length > 0;
	}

	/**
	 * Handles Compute Instructions Received from Clients
	 */
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{		
		if (instruction.getCommand().equals("CHAT") && users.containsKey(instruction.getDeviceID()))
		{
			double   latitude  = this.getLatitude(users.get(instruction.getDeviceID()));
			double   longitude = this.getLongitude(users.get(instruction.getDeviceID()));
			ChatItem ci        = new ChatItem(instruction.getDeviceID(), instruction.getPayload("TEXT"), latitude, longitude);
			
			chatLog.add(ci);
			this.sendContext();
		}
		else if (instruction.getCommand().equals("USER_NAME"))
		{
			this.sendContext();
		}
	}

	/**
	 * OPTIONAL:  Allows you to Customize the Name of the App on a Per User Basis
	 */
	public String getName(String userContextJSON)
	{
		return name;
	}
	
	/**
	 * OPTIONAL:  Allows you to Customize the Category of the App on a Per User Basis
	 */
	public String getCategory(String userContextJSON)
	{
		return category;
	}
	
	/**
	 * OPTIONAL:  Allows you to Customize the Description of the App on a Per User Basis
	 */
	public String getDescription(String userContextJSON)
	{
		return description;
	}
	
	/**
	 * OPTIONAL:  Allows you to Customize the Lifetime of an App on a Per User Basis
	 */
	public int getLifetime(String userContextJSON)
	{
		return this.lifetime;
	}

	/**
	 * OPTIONAL:  Allows you to Customize the Logo of an App on a Per User Basis
	 */
	public String getLogoPath(String userContextJSON)
	{
		return logoPath;
	}

	private String[] getNearbyDevices(String deviceID)
	{
		ArrayList<String> result = new ArrayList<String>();
		
		if (users.containsKey(deviceID))
		{
			JSONContextParser deviceParser = users.get(deviceID);
			double 			  latitude     = this.getLatitude(deviceParser);
			double 			  longitude    = this.getLongitude(deviceParser);
			
			for (JSONContextParser parser : users.values())
			{
				String otherDeviceID = this.getDeviceID(parser);
				
				if (!otherDeviceID.equals(deviceID))
				{
					double distance = this.getDistance(parser, latitude, longitude);
					
					if (distance <= MAX_DISTANCE_IN_KM)
					{
						result.add(otherDeviceID);
					}
				}
			}	
		}	
		
		return result.toArray(new String[0]);
	}

	private class ChatItem 
	{
		public String name;
		public String text;
		public double latitude;
		public double longitude;
		
		public ChatItem(String name, String text, double latitude, double longitude)
		{
			this.name	   = name;
			this.text	   = text;
			this.latitude  = latitude;
			this.longitude = longitude;
		}
	}
}
