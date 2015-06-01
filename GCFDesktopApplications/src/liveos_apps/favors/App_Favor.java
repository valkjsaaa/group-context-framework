package liveos_apps.favors;

import java.util.ArrayList;
import java.util.Date;

import liveos_apps.DesktopApplicationProvider;

import cern.colt.Arrays;

import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.desktopframework.toolkit.SQLToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.liveos.ApplicationSettings;
import com.adefreitas.messages.ComputeInstruction;
import com.google.gson.JsonObject;

public class App_Favor extends DesktopApplicationProvider
{	
	// Properties
	private int		   		timestamp;
	private String	   		id;
	private String 	   		description;
	private String	   		telephone;
	private String	   		photoURL;
	private double	   		latitude;
	private double	   		longitude;
	ArrayList<String>  		sensors;
	private String	   		status;
	private FavorDispatcher dispatcher;
	
	// SQL Database Query Tool
	private SQLToolkit sqlToolkit;
	
	// Use Metrics
	private Date	dateCreated;
	private int     views;
	private boolean completed;
	
	/**
	 * Constructor
	 */
	public App_Favor(String id, FavorDispatcher dispatcher, int timestamp, String description, String photoURL, double latitude, double longitude, String status, String[] tags, 
			GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port, SQLToolkit sqlToolkit)
	{
		// Populates Favor with DEFAULT Values.  They will get overridden by the methods below (getDescription() . . .)
		super(	groupContextManager, 
				id, 
				"Favor Request", 
				"description", 
				"FAVOR",
				new String[] { }, 
				new String[] { }, 
				"http://25.media.tumblr.com/tumblr_kvewyajk5c1qzxzwwo1_500.jpg",
				30,
				commMode, 
				ipAddress, 
				port);
		
		this.timestamp   = timestamp;
		this.id			 = id;
		this.description = description;
		this.telephone   = telephone;
		this.photoURL    = photoURL;
		this.latitude    = latitude;
		this.longitude   = longitude;
		this.status		 = status;
		this.dispatcher  = dispatcher;
		this.completed   = false;
		this.sqlToolkit  = sqlToolkit;
		this.sensors 	 = new ArrayList<String>();
		this.dateCreated = new Date();
		
		for (String tag : tags)
		{
			sensors.add(tag);
		}
	}
		
	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=" + "http://gcf.cmu-tbank.com/apps/favors/favordetails.php?timestamp=" + timestamp };
	}

	/**
	 * Logic for when a User Subscribes
	 */
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		System.out.println(newSubscription.getDeviceID() + " has subscribed.");
		
		views++;
	}
	
	/**
	 * Logic for when a User Unsubscribes
	 */
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		super.onSubscriptionCancelation(subscription);
		
		System.out.println(subscription.getDeviceID() + " has unsubscribed.");
	}
	
	/**
	 * Determines Whether or Not to Send Application Data
	 */
	@Override
	public boolean sendAppData(String bluewaveContext)
	{        
		JSONContextParser parser      = new JSONContextParser(JSONContextParser.JSON_TEXT, bluewaveContext);
		JsonObject 		  locationObj = parser.getJSONObject("location");
		double			  distance    = this.getDistance(parser, latitude, longitude);
				
		// This will make the text output pretty.  Trust me.
		//System.out.println(parser);
		
		// HINT:  ALWAYS CHECK TO SEE IF A DEVICE HAS THE JSON TAG
		// There are older GCF devices in the environment that have not received the new code
		if (locationObj != null)
		{
			String source = locationObj.has("SENSOR") ? locationObj.get("SENSOR").getAsString() : "IMPROMPTU_APP_DIRECTORY";
			//System.out.println("Sensed By:  " + source);
			//System.out.println("In Sensors: " + sensors.contains(source));
		}
		
		// Debug Text
		//System.out.println("Distance: " + distance + " ");
				
		return !completed;
	}

	/**
	 * Handles Compute Instructions Received from Clients
	 */
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
		
		if (instruction.getCommand().equalsIgnoreCase("COMPLETE_TASK"))
		{
			dispatcher.markComplete(timestamp);
			completed = true;
		}
	}
	
	/**
	 * This method returns the name of the application
	 * You can return a flat string, or have a dynamic name based on the user
	 */
	public String getName(String userContextJSON)
	{
		String name = "REQUEST: " + description.substring(0, Math.min(description.length()-1, 100));
		
		if (description.length() > 109)
		{
			name += " . . . ";
		}
		
		return name;
	}
	
	/**
	 * This method returns the description of the application
	 * You can return a 
	 */
	public String getDescription(String userContextJSON)
	{
		String result = "Requested on " + dateCreated + "\n\n";
		
		if (this.getSubscriptions().length > 0)
		{
			result += "[" + this.getSubscriptions().length + " users looking at this favor]";
		}
		else
		{
			result += views + " views total.";
		}
		
		return result;
	}
	
	public int getLifetime(String userContextJSON)
	{
		if (!completed)
		{
			return 60;
		}
		else
		{
			return 0;
		}
	}
	
	private boolean tagsMatch(JSONContextParser parser)
	{
		JsonObject preferencesObj = parser.getJSONObject("preferences");
		
		if (preferencesObj != null && preferencesObj.has("roles"))
		{
			String[] userRoles = preferencesObj.get("roles").getAsString().split(",");
			
			for (String userRole : userRoles)
			{
				if (sensors.contains(userRole))
				{
					return true;
				}
			}
		}
		
		return false;
	}
}
