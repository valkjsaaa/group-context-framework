package com.adefreitas.beacon.apps;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.android.impromptu.*;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.adefreitas.magicappserver.GCFApplication;

public class App_AndroidProjectorProxy extends AndroidApplicationProvider
{	
	public static final String   CONTEXT_TYPE  	      = "TEMPLATE_APP";
	public static final String   DEFAULT_TITLE 	      = "Template App";
	public static final String   DEFAULT_DESCRIPTION  = "This description is what people will see unless you overload it using getDescription().";
	public static final String   DEFAULT_CATEGORY     = "CATEGORY GOES HERE";
	public static final String[] CONTEXTS_REQUIRED    = new String[] { };
	public static final String[] PREFERENCES_REQUIRED = new String[] { };
	public static final String   DEFAULT_LOGO_PATH    = "http://png-4.findicons.com/files/icons/2711/free_icons_for_windows8_metro/128/video_projector.png";
	public static final int      DEFAULT_LIFETIME	  = 60;
	
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
	public App_AndroidProjectorProxy(GCFApplication application, GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port, String channel)
	{
		super(groupContextManager, 
				"STI_DIGITAL_PROJECTOR", 
				"Digital Projector",
				"Controls the Projector Located in the HCII Commons Conference Room",
				"Devices", 
				CONTEXTS_REQUIRED, 
				PREFERENCES_REQUIRED, 
				DEFAULT_LOGO_PATH, 
				DEFAULT_LIFETIME,				
				commMode, ipAddress, port, channel);
		
		this.setProxyDeviceID("IMPROMPTU_STI_MAC");
	}
		
	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String ui  = "<html>";
		ui		  += "<title>Template Application</title>";
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
		// TODO:  Specify the EXACT conditions when this app should appear
		return true;
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
