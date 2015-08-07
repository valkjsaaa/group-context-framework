package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.desktop.toolkit.SQLToolkit;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_BluewaveDebug extends DesktopApplicationProvider
{
	public static final String   CONTEXT_TYPE  	      = "BLUEWAVE_TEST";
	public static final String   DEFAULT_TITLE 	      = "Bluewave Debug Tool";
	public static final String   DEFAULT_DESCRIPTION  = "Allows you to manually insert or replace your context via Bluewave.";
	public static final String   DEFAULT_CATEGORY     = "DEV TOOLS";
	public static final String[] CONTEXTS_REQUIRED    = new String[] { };
	public static final String[] PREFERENCES_REQUIRED = new String[] { };
	public static final String   LOGO_PATH			  = "http://gcf.cmu-tbank.com/apps/icons/bluewave.jpeg";
	public static final int      DEFAULT_LIFETIME	  = 120;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 */
	public App_BluewaveDebug(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, CONTEXT_TYPE, DEFAULT_TITLE, DEFAULT_DESCRIPTION, DEFAULT_CATEGORY, CONTEXTS_REQUIRED, PREFERENCES_REQUIRED, LOGO_PATH, DEFAULT_LIFETIME,				
				commMode, ipAddress, port);
	}

	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
//		String ui  = "<html><title>Bluewave Test</title>";
//		ui 		  += "<h3>Parameter Name:</h3>";
//		ui		  += "<input type=\"text\" name-\"name\"/>";
//		ui 		  += "<h3>Parameter Value:</h3>";
//		ui 		  += "<textarea id=\"txtValue\" rows=\"5\" cols=\"30\"></textarea>";
//		ui	      += "<p><input value=\"SUBMIT\" type=\"button\" height=\"200\" width=\"400\"" +
//				      "onclick=\"" +
//				      "device.toast('Thanks for Complaining!'); " +
//				      "device.finish();" +
//				      "\"/></p>";
//								
		// You can specify a UI as one of the following:
		// UI=<RAW HTML GOES HERE>
		// WEBSITE=<URL GOES HERE>
		// PACKAGE=<GOOGLE PLAY STORE PACKAGE NAME GOES HERE>
		return new String[] { "WEBSITE=" + "http://gcf.cmu-tbank.com/apps/bluewave_debug/index.html" };
	}

	/**
	 * Event Called When a Client Connects
	 */
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
	}
	
	/**
	 * Event Called with a Client Disconnects
	 */
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		super.onSubscriptionCancelation(subscription);
	}
	
	/**
	 * Determines Whether or Not to Send Application Data
	 */
	@Override
	public boolean sendAppData(String userContext)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, userContext);
		return this.hasEmailAddress(parser, new String[] { "adrian.defreitas@gmail.com", "akshaya.kar@gmail.com", "gcf.user.2@gmail.com", "gcf.user.3@gmail.com", "gcf.user.4@gmail.com" });
//		double distanceFromPittsburgh = this.getDistance(parser, 40.4397, -79.9764);
//		
//		System.out.print("Distance = " + distanceFromPittsburgh + "km ");
//		
//		return distanceFromPittsburgh < 20.0;
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
