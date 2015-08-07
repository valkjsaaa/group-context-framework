package com.adefreitas.beacon.inoutboard;

import org.json.JSONObject;

import android.content.Intent;
import android.widget.Toast;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.android.impromptu.*;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.adefreitas.magicappserver.GCFApplication;

public class App_InOutBoardIdentity extends AndroidApplicationProvider
{	
	public static final String ACTION_IDENTITY_UPDATE = "ACTION_UPDATE";
	
	private UserIdentityContextProvider identityProvider;
	
	private GCFApplication application;
	
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
	public App_InOutBoardIdentity(GCFApplication application, UserIdentityContextProvider identityProvider, GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(  groupContextManager, 
				"ID", 
				"Lab Display", 
				"The lab beacon would like to know who you are.", 
				"LAB",
				new String[] { }, // Contexts
				new String[] { }, // Preferences
				"http://www.isc.hbs.edu/Style%20Library/hbs/images/home/icons/large-circles/research-icon.png",
				120,
				commMode, 
				ipAddress, 
				port);
		
		this.identityProvider = identityProvider;
		this.application 	  = application;
	}
		
	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{		
		return new String[] { "WEBSITE=" + "http://gcf.cmu-tbank.com/apps/labdisplay/index.html" };
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
		boolean result = false;
		
		try
		{
			JSONObject parser   = new JSONObject(bluewaveContext);
			String     deviceID = parser.getJSONObject("device").get("deviceID").toString();
			
			result = !identityProvider.hasData(deviceID);
			System.out.println("FOUND DEVICE: " + deviceID + "; USER KNOWN=" + result);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		return result;		
	}

	/**
	 * Handles Compute Instructions Received from Clients
	 */
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
		
		if (instruction.getCommand().equalsIgnoreCase("REGISTER"))
		{
			JSONObject data = new JSONObject();
			
			try
			{
				String name  = instruction.getPayload("name");
				String phone = instruction.getPayload("phone");
				
				if (name != null)
				{
					data.put("name", name);
				}
				
				if (phone != null)
				{
					data.put("phone", phone);
				}
				
				if (data.length() > 0)
				{
					data.put("deviceID", instruction.getDeviceID());
					identityProvider.addEntry(instruction.getDeviceID(), data);
					
					Intent i = new Intent(ACTION_IDENTITY_UPDATE);
					application.sendBroadcast(i);
				}
			}
			catch (Exception ex)
			{
				Toast.makeText(application, "Registration Problem: " + ex.getMessage(), Toast.LENGTH_LONG).show();
			}	
		}
	}

	public String getDescription(String userContextJSON)
	{
		return "The lab beacon [" + this.getGroupContextManager().getDeviceID() + "] would like to know who you are.";
	}
}
