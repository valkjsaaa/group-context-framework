package com.adefreitas.beacon.apps;

import android.widget.Toast;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.android.impromptu.*;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.adefreitas.magicappserver.GCFApplication;

public class App_GameConnectMeFactory extends AndroidApplicationProvider
{	
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
	public App_GameConnectMeFactory(GCFApplication application, GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(  groupContextManager, 
				"GAME_CMF_" + groupContextManager.getDeviceID(), 
				"Connect Me Factory", 
				"A simple game built on the phaser framework.", 
				"GAME",
				new String[] { }, // Contexts
				new String[] { }, // Preferences
				"http://74.111.161.33/gcf/universalremote/magic/dots.png",
				60,
				commMode, 
				ipAddress, 
				port);
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
		
		return new String[] { "WEBSITE=http://grimpanda.com/games/sandbox/connectme-sfx/"};
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

}
