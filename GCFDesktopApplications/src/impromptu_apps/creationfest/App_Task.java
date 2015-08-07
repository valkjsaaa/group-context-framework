package impromptu_apps.creationfest;

import impromptu_apps.DesktopApplicationProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.google.gson.JsonObject;

public class App_Task extends DesktopApplicationProvider
{	
	// Constants
	private static final String UPDATE_URL   = "http://gcf.cmu-tbank.com/apps/creationfest/getRecentProblems.php?timestamp=";
	private static final double MIN_DISTANCE = 0.50;
	
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
	ArrayList<String> deviceIDs;
	private int       views;
	private boolean   completed;
	
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
				"NEARBY PROBLEMS",
				new String[] { }, 
				new String[] { }, 
				"https://cdn2.iconfinder.com/data/icons/ios-7-style-metro-ui-icons/128/Flurry_Google_Maps.png",
				30,
				commMode, 
				ipAddress, 
				port,
				"TASK");
		
		this.dispatcher  = dispatcher;
		this.completed   = false;
		this.views 		 = 0;
		this.deviceIDs   = new ArrayList<String>();
		
		update(id, timestamp, description, telephone, photoURL, latitude, longitude, status, tags);
	}
		
	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=" + "http://gcf.cmu-tbank.com/apps/creationfest/viewProblem.php?timestamp=" + timestamp };
	}

	/**
	 * Logic for when a User Subscribes
	 */
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
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
		String			  deviceID = this.getDeviceID(parser);
		double			  distance = this.getDistance(parser, latitude, longitude);
		
		boolean distanceCheck = distance < MIN_DISTANCE;
		boolean roleCheck     = tagsMatch(bluewaveContext);
		
		// Debug Text
		System.out.print("Distance: " + distanceCheck + "; " + "Role: " + roleCheck + "; ");

		// TODO:  Your Code Goes Here
		
		
		
		
		boolean send = !completed && roleCheck && distanceCheck;
		
		// Keeps track of which devices this task has been offered to
		if (send && !deviceIDs.contains(deviceID))
		{
			deviceIDs.add(deviceID);
		}
		
		return send;
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
			this.log("TASK_COMPLETE", "USER=" + instruction.getDeviceID() + ", VIEWS=" + views + ", UNIQUE_DEVICES=" + this.deviceIDs.size());
			dispatcher.markComplete(timestamp);
			completed = true;
		}
	}
	
	public String getName(String userContextJSON)
	{
		return description.substring(0, Math.min(description.length(), 40));
	}
	
	public String getDescription(String userContextJSON)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, userContextJSON);
		
		String result = String.format("Distance: %1.2fkm\n", this.getDistance(parser, latitude, longitude));
		result += "Roles: " + Arrays.toString(roles.toArray(new String[0]));		
		result += "\n";
		
		if (this.getSubscriptions().length > 0)
		{
			result += this.getSubscriptions().length + " users looking at this.";
		}
		else
		{
			result += views + " total views.";
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

	public void update(String id, int timestamp, String description, String telephone, String photo, double latitude, double longitude, String status, String[] tags)
	{
		this.id = id;
		this.timestamp = timestamp;
		this.description = description;
		this.telephone = telephone;
		this.photoURL = photo;
		this.latitude = latitude;
		this.longitude = longitude;
		this.status = status;
		
		this.roles = new ArrayList<String>();
		for (String tag : tags)
		{
			roles.add(tag);
		}
	}
}
