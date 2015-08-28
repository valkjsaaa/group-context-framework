package com.adefreitas.beacon.apps;

import android.widget.LinearLayout;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.android.impromptu.*;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.adefreitas.magicappserver.GCFApplication;

public class App_ControlScreen extends AndroidApplicationProvider
{	
	public static final String   CONTEXT_TYPE  	      = "SCREEN_CONTROL";
	public static final String   DEFAULT_TITLE 	      = "Tablet Screen Control";
	public static final String   DEFAULT_DESCRIPTION  = "Lets you control the color of the screen.";
	public static final String   DEFAULT_CATEGORY     = "DEV TOOLS";
	public static final String[] CONTEXTS_REQUIRED    = new String[] { };
	public static final String[] PREFERENCES_REQUIRED = new String[] { };
	public static final String   DEFAULT_LOGO_PATH    = "http://icons.iconarchive.com/icons/pelfusion/long-shadow-media/512/Mobile-Tablet-icon.png";
	public static final int      DEFAULT_LIFETIME	  = 120;
	
	private LinearLayout view;
	
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
	public App_ControlScreen(GCFApplication application, LinearLayout view, GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				CONTEXT_TYPE + "_" + groupContextManager.getDeviceID(), 
				DEFAULT_TITLE + " (" + groupContextManager.getDeviceID() + ")", 
				DEFAULT_DESCRIPTION, 
				DEFAULT_CATEGORY, 
				CONTEXTS_REQUIRED, 
				PREFERENCES_REQUIRED, 
				DEFAULT_LOGO_PATH, 
				DEFAULT_LIFETIME,				
				commMode, ipAddress, port);
		
		this.view = view;
	}
		
	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{		
		// You can specify a UI as one of the following:
		// UI=<RAW HTML GOES HERE>
		// WEBSITE=<URL GOES HERE>
		// PACKAGE=<GOOGLE PLAY STORE PACKAGE NAME GOES HERE>
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/screen_controls/controls.php" };
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
		if (instruction.getCommand().equalsIgnoreCase("color"))
		{
			String value = instruction.getPayload("VALUE");
			
			if (value.equalsIgnoreCase("WHITE"))
			{
				view.setBackgroundColor(0xFFFFFFFF);
			}
			else if (value.equalsIgnoreCase("RED"))
			{
				view.setBackgroundColor(0xFFCC3333);
			}
			else if (value.equalsIgnoreCase("GREEN"))
			{
				view.setBackgroundColor(0xFF33CC33);
			}
			else if (value.equalsIgnoreCase("BLUE"))
			{
				view.setBackgroundColor(0xFF3333CC);
			}
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
}
