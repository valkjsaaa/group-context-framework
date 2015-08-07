package impromptu_apps.desktop;

import java.util.HashMap;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_Troubleshooting extends DesktopApplicationProvider
{	
	public static final String   CONTEXT_TYPE  	      = "TROUBLE";
	public static final String   DEFAULT_TITLE 	      = "Troubleshooting App";
	public static final String   DEFAULT_DESCRIPTION  = "Impromptu is having trouble analyzing your context.  Click here for more details.";
	public static final String   DEFAULT_CATEGORY     = "IMPROMPTU";
	public static final String[] CONTEXTS_REQUIRED    = new String[] { };
	public static final String[] PREFERENCES_REQUIRED = new String[] { };
	public static final String   DEFAULT_LOGO_PATH    = "http://goo.gl/k2xs5p";
	public static final int      DEFAULT_LIFETIME	  = 60;
	
	// Keeps Track of the Number of Times a Problematic Context has arrived
	private HashMap<String, Integer> problemCount;
	private final int PROBLEM_THRESHOLD = 3;
	
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
	public App_Troubleshooting(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
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
		
		problemCount = new HashMap<String, Integer>();
	}
		
	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/troubleshooting/index.php" };
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
		JSONContextParser parser   = new JSONContextParser(JSONContextParser.JSON_TEXT, bluewaveContext);
		String 			  deviceID = this.getDeviceID(parser);
		boolean 		  send 	   = false;
		
		if (!problemCount.containsKey(deviceID))
		{
			problemCount.put(deviceID, 0);
		}
		
		if (parser.getJSONObject("location") == null || 
			!parser.getJSONObject("location").has("LATITUDE") || 
			!parser.getJSONObject("location").has("LONGITUDE"))
		{	
			problemCount.put(deviceID, problemCount.get(deviceID) + 1);
		}
		else
		{
			problemCount.put(deviceID, 0);
		}
		
		return problemCount.get(deviceID) > PROBLEM_THRESHOLD;
	}

	/**
	 * Handles Compute Instructions Received from Clients
	 */
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
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
}
