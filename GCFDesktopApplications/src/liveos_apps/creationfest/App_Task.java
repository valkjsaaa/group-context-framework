package liveos_apps.creationfest;

import java.util.ArrayList;
import java.util.Date;

import liveos_apps.DesktopApplicationProvider;

import cern.colt.Arrays;

import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.liveos.ApplicationSettings;
import com.adefreitas.messages.ComputeInstruction;
import com.google.gson.JsonObject;

public class App_Task extends DesktopApplicationProvider
{	
	// Constants
	private static final String UPDATE_URL  = "http://gcf.cmu-tbank.com/apps/creationfest/getRecentProblems.php?timestamp=";

	// Properties
	private int		   timestamp;
	private String	   id;
	private String 	   description;
	private String	   telephone;
	private String	   photoURL;
	private double	   latitude;
	private double	   longitude;
	ArrayList<String>  roles;
	private String	   status;
	private TaskDispatcher dispatcher;
	
	// Use Metrics
	private int     views;
	private boolean completed;
	
	/**
	 * Constructor
	 */
	public App_Task(String id, TaskDispatcher dispatcher, int timestamp, String description, String telephone, String photoURL, double latitude, double longitude, String status, String[] tags, 
			GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(	groupContextManager, 
				id, 
				"Task", 
				"You are in range of a reported problem.  Click for more details.", 
				"TASK",
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
		
		this.roles = new ArrayList<String>();
		for (String tag : tags)
		{
			roles.add(tag);
		}
	}
		
	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=" + "http://gcf.cmu-tbank.com/apps/polymertest/starter/description.php?timestamp=" + timestamp };
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
		JSONContextParser parser   = new JSONContextParser(JSONContextParser.JSON_TEXT, bluewaveContext);
		double			  distance = this.getDistance(parser, latitude, longitude);
		
		// Debug Text
		System.out.print("Distance: " + distance + " ");
		
		return tagsMatch(bluewaveContext) && !completed && this.getDistance(parser, latitude, longitude) < 100.0;
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
	
	public String getName(String userContextJSON)
	{
		return description.substring(0, Math.min(description.length()-1, 40));
	}
	
	public String getDescription(String userContextJSON)
	{
		String result = "You are near a potential problem.  Click for more information.\n\n";
		
		if (this.getSubscriptions().length > 0)
		{
			result += "[" + this.getSubscriptions().length + " users looking at this task]";
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

	public boolean tagsMatch(String userContextJSON)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, userContextJSON);
		JsonObject preferencesObj = parser.getJSONObject("preferences");
		
		if (preferencesObj != null && preferencesObj.has("roles"))
		{
			String[] userRoles = preferencesObj.get("roles").getAsString().split(",");
			
			for (String userRole : userRoles)
			{
				if (roles.contains(userRole))
				{
					return true;
				}
			}
		}
		
		return false;
	}
}
