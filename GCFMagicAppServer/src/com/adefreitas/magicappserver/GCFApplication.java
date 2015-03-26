package com.adefreitas.magicappserver;

import java.util.ArrayList;

import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.androidbluewave.BluewaveManager;
import com.adefreitas.androidbluewave.JSONContextParser;
import com.adefreitas.androidframework.AndroidBatteryMonitor;
import com.adefreitas.androidframework.AndroidGroupContextManager;
import com.adefreitas.androidframework.ContextReceiver;
import com.adefreitas.androidframework.toolkit.CloudStorageToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.ContextType;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.liveos.ApplicationSettings;
import com.adefreitas.messages.ContextData;
import com.adefreitas.providers.NullContextProvider;

public class GCFApplication extends Application
{
	// Application Constants
	public static final String  APP_TICK = "TICK";
	public static final String  LOG_NAME = "GCF_APP"; 
	
	// GCF Communication Settings (BROADCAST_MODE Assumes a Functional TCP Relay Running)
	public static final CommMode COMM_MODE  = CommMode.MQTT;
	public static final String   IP_ADDRESS = Settings.DEV_MQTT_IP;
	public static final int      PORT 	    = Settings.DEV_MQTT_PORT;
	public static final String 	 DEV_NAME   = Settings.getDeviceName(android.os.Build.SERIAL);
		
	// GCF Variables
	public AndroidBatteryMonitor      batteryMonitor;
	public AndroidGroupContextManager groupContextManager;
	
	// Bluewave
	public BluewaveManager bluewaveManager;
	
	// Cloud Storage Settings
	private CloudStorageToolkit cloudToolkit;
	
	// Intent Filters
	private ArrayList<ContextReceiver> contextReceivers;
	private IntentFilter    		   filter;
	private IntentReceiver  		   intentReceiver;
	
	/**
	 * One-Time Application Initialization Method
	 */
	@Override
	public void onCreate() 
	{
		super.onCreate();
		
		// Creates the Group Context Manager, which is Responsible for Context Producing and Sharing
		batteryMonitor 		 = new AndroidBatteryMonitor(this, DEV_NAME, 5);
		groupContextManager  = new AndroidGroupContextManager(this, DEV_NAME, batteryMonitor, false);
			
		// EXPERIMENTAL:  Initializes Bluewave
		bluewaveManager = new BluewaveManager(this, groupContextManager, Settings.getBluewaveFilename(groupContextManager.getDeviceID()));
		//bluewaveManager.startScan();
		bluewaveManager.getPersonalContextProvider().setSharable(false);
		
		// Creates the Cloud Toolkit Helper
		//dropboxToolkit = new DropboxToolkit(this, APP_KEY, APP_SECRET, AUTH_TOKEN);
		
		// Creates an Array of Context Receivers
		this.contextReceivers = new ArrayList<ContextReceiver>();
		
		// Creates Applications
		configureAppProviders();
		
		// Create Intent Filter and Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED);
		this.filter.addAction(AndroidGroupContextManager.ACTION_GCF_OUTPUT);
		this.filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
		this.registerReceiver(intentReceiver, filter);
	}
	
	/**
	 * Returns the Group Contest Manager
	 * @return
	 */
	public AndroidGroupContextManager getGroupContextManager()
	{
		return groupContextManager;
	}
	
	/**
	 * Retrieves the Current Cloud Storage Toolkit
	 * @return
	 */
	public CloudStorageToolkit getCloudToolkit()
	{
		if (cloudToolkit == null)
		{
			Log.e(LOG_NAME, "Cloud code not instantiated.  Check GCFApplication.java");
		}
		
		return cloudToolkit;
	}
	
	/**
	 * Creates a Toast Notification
	 * @param title
	 * @param subtitle
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void createNotification(Intent intent, String title, String subtitle)
	{
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) 
		{
			 Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			
			 PendingIntent 		 pendingIntent 		 = PendingIntent.getActivity(this, 0, intent, 0);
			 NotificationManager notificationManager = (NotificationManager)this.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
			 Notification 		 note 				 = new Notification.Builder(this)
			 	//.setSmallIcon(com.adefreitas..R.drawable.gcf)
			 	.setContentTitle(title)
			 	.setContentText(subtitle)
			 	.setAutoCancel(true)
			 	.setSound(soundUri)
			 	.setContentIntent(pendingIntent).build();
			 
			 notificationManager.notify(0, note);
		}
		else
		{
			Toast.makeText(this, title + ": " + subtitle, Toast.LENGTH_LONG).show();
		}
	}
	
	// Application Specific Methods -------------------------------------------------------------
	private void configureAppProviders()
	{
		// Connects to the Server
		String connectionKey = groupContextManager.connect(COMM_MODE, IP_ADDRESS, PORT);
		
		// Creates Applications for Specific Devices
		if (groupContextManager.getDeviceID().equals("Device 4"))
		{
			// Creates the Contact Card App
			AndroidApplicationProvider contactCardProvider = new App_ContactCard(this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT);
			groupContextManager.registerContextProvider(contactCardProvider);
			groupContextManager.subscribe(connectionKey, contactCardProvider.getContextType());
		}
		else if (groupContextManager.getDeviceID().equals("Device 1"))
		{
			// Creates the Lights Task App
			AndroidApplicationProvider lightsProvider = new App_HomeLights(this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT);
			groupContextManager.registerContextProvider(lightsProvider);
			groupContextManager.subscribe(connectionKey, lightsProvider.getContextType());
		}
		
		// Creates the Snap To It Decoy
		//groupContextManager.registerContextProvider(new NullContextProvider("SNAP_TO_IT", groupContextManager));
		
		// Creates the Instant Task App
//		AndroidApplicationProvider taskProvider = new App_QuickTask(this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT);
//		groupContextManager.registerContextProvider(taskProvider);
//		groupContextManager.subscribe(connectionKey, taskProvider.getContextType());
		
		// Creates a Game!
//		AndroidApplicationProvider gameProvider = new App_GameConnectMeFactory(this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT);
//		groupContextManager.registerContextProvider(gameProvider);
//		groupContextManager.subscribe(connectionKey, gameProvider.getContextType());
		
		// TODO:  Remove This Someday when Communications are More Reliable!
		//groupContextManager.subscribe(connectionKey, "cmu/gcf_dns");
	}
	
	// Group Context Framework Methods ----------------------------------------------------------	
	public void onBluewaveContext(JSONContextParser parser)
	{
		// Attempts to Extract Connection Information
		JSONObject context = parser.getJSONObject("identity");
		
		if (context != null)
		{
			try
			{
				String   deviceID  		 = parser.getDeviceID();
				CommMode commMode  		 = CommMode.valueOf(context.getString("COMM_MODE")); 
				String   ipAddress 		 = context.getString("IP_ADDRESS");
				int      port            = context.getInt("PORT");
				boolean  snapToItEnabled = parser.getJSONRoot().has("snap-to-it");
				
				String result = "";
				
				if (ipAddress != null)
				{
					for (ContextProvider p : groupContextManager.getRegisteredProviders())
					{
						if (p instanceof AndroidApplicationProvider)
						{
							AndroidApplicationProvider appProvider = (AndroidApplicationProvider)p;
							
							// Determines whether the App Provider Wants this Context
							if (appProvider.sendAppData(parser.toString()))
							{
								result += appProvider.getContextType() + " ";
								
								boolean isConnected   = groupContextManager.isConnected(commMode, ipAddress, port);
								String connectionKey = groupContextManager.connect(commMode, ipAddress, port);
								
								groupContextManager.sendComputeInstruction(
										connectionKey, 
										ApplicationSettings.DNS_CHANNEL, 
										ContextType.PERSONAL, 
										new String[] { deviceID }, 
										"APPLICATION", 
										appProvider.getInformation().toArray(new String[0]));
							}
						}
					}
					
					if (result.length() > 0)
					{
						Toast.makeText(this, "Sending app advertisements [" + result.trim() + "] to: " + deviceID, Toast.LENGTH_LONG).show();
					}
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				Toast.makeText(this, "Problem Analyzing Context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
			}	
		}
	}
	
	public void setContextReceiver(ContextReceiver newContextReceiver)
	{
		if (contextReceivers == null)
		{
			System.out.println("Context Receivers is NULL");
		}
		else
		{
			System.out.println("New Context Receiver is NULL");
		}
		
		if (!contextReceivers.contains(newContextReceiver))
		{
			contextReceivers.add(newContextReceiver);
		}
	}
	
	// Intent Receiver --------------------------------------------------------------------------
	private class IntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED))
			{
				// Extracts the values from the intent
				String   contextType = intent.getStringExtra(ContextData.CONTEXT_TYPE);
				String   deviceID    = intent.getStringExtra(ContextData.DEVICE_ID);
				String   description = intent.getStringExtra(ContextData.DESCRIPTION);
				String[] values      = intent.getStringArrayExtra(ContextData.VALUES);
				
				// Forwards Values to the ContextReceiver for Processing
				for (ContextReceiver contextReceiver : contextReceivers)
				{
					contextReceiver.onContextData(new ContextData(contextType, deviceID, description, values));
				}
			}
			else if (intent.getAction().equals(AndroidGroupContextManager.ACTION_GCF_OUTPUT))
			{
				// Extracts the values from the intent
				String text = intent.getStringExtra(AndroidGroupContextManager.GCF_OUTPUT);
				
				// Forwards Values to the Application for Processing
				for (ContextReceiver contextReceiver : contextReceivers)
				{
					contextReceiver.onGCFOutput(text);
				}
			}
			else if (intent.getAction().equals(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED))
			{
				// This is the Raw JSON from the Device
				String json = intent.getStringExtra(BluewaveManager.OTHER_USER_CONTEXT);
				
				// Creates a Parser
				JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
				
				// Handles Context Internally
				onBluewaveContext(parser);
				
				// Forwards Values to the Application for Processing
				for (ContextReceiver contextReceiver : contextReceivers)
				{
					contextReceiver.onBluewaveContext(parser);
				}
			}
			else
			{
				Log.e("", "Unknown Action: " + intent.getAction());
			}
		}
	}
}
