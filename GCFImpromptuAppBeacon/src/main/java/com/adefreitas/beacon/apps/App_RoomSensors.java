package com.adefreitas.beacon.apps;

import android.util.Log;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.android.ContextReceiver;
import com.adefreitas.gcf.android.bluewave.JSONContextParser;
import com.adefreitas.gcf.android.impromptu.AndroidApplicationProvider;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.adefreitas.gcf.messages.ContextData;
import com.adefreitas.gcf.messages.ContextRequest;
import com.adefreitas.magicappserver.GCFApplication;

public class App_RoomSensors extends AndroidApplicationProvider implements ContextReceiver
{	
	public static final String   CONTEXT_TYPE  	      = "ROOM_SENSORS";
	public static final String   DEFAULT_TITLE 	      = "Room Sensor";
	public static final String   DEFAULT_DESCRIPTION  = "View the sensors in this room.";
	public static final String   DEFAULT_CATEGORY     = "SENSORS";
	public static final String[] CONTEXTS_REQUIRED    = new String[] { };
	public static final String[] PREFERENCES_REQUIRED = new String[] { };
	public static final String   DEFAULT_LOGO_PATH    = "http://icons.iconarchive.com/icons/icons-land/vista-map-markers/256/Map-Marker-Marker-Outside-Azure-icon.png";
	public static final int      DEFAULT_LIFETIME	  = 120;
	
	public boolean running;
	public String roomName;
	public GCFApplication application;
	
	private double temp = 0.0;
	
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
	public App_RoomSensors(GCFApplication application, GroupContextManager groupContextManager, String roomName, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				CONTEXT_TYPE + "_" + roomName, 
				DEFAULT_TITLE, 
				DEFAULT_DESCRIPTION, 
				DEFAULT_CATEGORY, 
				CONTEXTS_REQUIRED, 
				PREFERENCES_REQUIRED, 
				DEFAULT_LOGO_PATH, 
				DEFAULT_LIFETIME,				
				commMode, ipAddress, port);
		
		this.roomName = roomName;
		this.running  = false;
		this.application = application;
		
		this.getGroupContextManager().sendRequest("TEMP", ContextRequest.MULTIPLE_SOURCE, new String[] {}, 10000, new String[] {"CHANNEL=dev/" + this.getGroupContextManager().getDeviceID()});
		application.setContextReceiver(this);
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
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/roomsensor/index.php?room=" + roomName + "&temp=" + temp};
	}

	/**
	 * Event Called When a Client Connects
	 */
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		if (this.getNumSubscriptions() == 1)
		{
			this.getGroupContextManager().sendRequest("TEMP", ContextRequest.MULTIPLE_SOURCE, new String[] {}, 10000, new String[] {"CHANNEL=dev/" + this.getGroupContextManager().getDeviceID()});
			application.setContextReceiver(this);
		}
	}
	
	/**
	 * Event Called with a Client Disconnects
	 */
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		super.onSubscriptionCancelation(subscription);
		
		if (this.getNumSubscriptions() == 0)
		{
			this.getGroupContextManager().cancelRequest("TEMP");
			application.removeContextReceiver(this);
		}
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

	
	@Override
	public void onContextData(ContextData data) 
	{
		if (data.getContextType().equals("TEMP"))
		{
			double oldTemp = this.temp;
			
			this.temp = Double.parseDouble(data.getPayload("TEMP"));
			Log.d(LOG_NAME, "Received Temperature Data: " + this.temp);
			
			if (this.temp != oldTemp)
			{
				this.sendContext();
			}
		}
	}

	
	@Override
	public void onBluewaveContext(JSONContextParser parser) 
	{
		// TODO Auto-generated method stub
	}
}
