package com.adefreit.gcfbluetoothrssi;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import com.adefreitas.gcf.Settings;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.android.*;
import com.adefreitas.gcf.android.bluewave.*;
import com.adefreitas.gcf.android.toolkit.*;
import com.adefreitas.gcf.messages.ContextData;

/**
 * This is a Sample Application Class Used to Abstract GCF's Core Functions
 * 
 * Instructions for Use:
 * 		1.  Change the Package Name to Your Application's Name!
 * 		2.  UPDATE AndroidManifest.xml with the Package Name
 * @author Adrian de Freitas
 */
public class GCFApplication extends Application
{	
	// Application Constants
	public static final String  LOG_NAME 		 = "GCF_APPLICATION"; 
	public static final String  PREFERENCES_NAME = "com.example.gcfandroidtemplate.preferences";
	public static final boolean DEBUG 			 = true;

	// GCF Constants (Connection to App Server)
	public static final CommMode COMM_MODE  = CommMode.MQTT;
	public static final String   IP_ADDRESS = Settings.DEV_MQTT_IP;
	public static final int      PORT 	    = Settings.DEV_MQTT_PORT;
	
	// GCF Variables
	public String defaultConnectionKey;
	public AndroidGroupContextManager gcm;
	
	// Application Preferences
	private SharedPreferences sharedPreferences;
	
	// Intent Filters
	private IntentFilter   			  filter;
	private ApplicationIntentReceiver intentReceiver;
	
	/**
	 * One-Time Application Initialization Method
	 * Runs when the Application First Turns On
	 */
	@Override
	public void onCreate() 
	{
		super.onCreate();
		
		// Gets the Device Name (or generates a friendly one)
		String deviceID = Settings.getDeviceName(android.os.Build.SERIAL);
		
		// Initializes Values and Data Structures
		// NOTE:  You don't need all of these, but they are helpful utilities!
		this.sharedPreferences = this.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
		
		// Create Intent Filter and Receiver
		// NOTE:  You can modify this section if you have other intents that you want to listen for
		this.intentReceiver = new ApplicationIntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED);
		this.filter.addAction(AndroidCommManager.ACTION_CHANNEL_SUBSCRIBED);
		this.filter.addAction(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED);
		this.filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
		this.registerReceiver(intentReceiver, filter);
			
	    // Creates the Group Context Manager
	    gcm = new AndroidGroupContextManager(this, deviceID, false);
		gcm.connect(COMM_MODE, IP_ADDRESS, PORT);
	    
		// Creates Context Providers
		gcm.getBluewaveManager().startLEScan(30000);
		
		// TODO: Initialize Your App's Data Structures
	}

	/**
	 * Returns the Group Contest Manager
	 * @return
	 */
	public AndroidGroupContextManager getGroupContextManager()
	{
		return gcm;
	}
	
	/**
	 * Returns the Bluewave Manager
	 * @return
	 */
	public BluewaveManager getBluewaveManager()
	{
		if (gcm != null)
		{
			return gcm.getBluewaveManager();
		}
		
		return null;
	}
		
    // Preference Methods -----------------------------------------------------------------------	
	public SharedPreferences getSharedPreferences()
	{
		return sharedPreferences;
	}
		
	// Intent Receiver --------------------------------------------------------------------------
	public class ApplicationIntentReceiver extends BroadcastReceiver
	{		
		@Override
		public void onReceive(Context context, Intent intent) 
		{				
			if (intent.getAction().equals(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED))
			{
				onCommThreadConnected(context, intent);
			}
			else if (intent.getAction().equals(AndroidCommManager.ACTION_CHANNEL_SUBSCRIBED))
			{
				onChannelSubscribed(context, intent);
			}
			else if (intent.getAction().equals(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED))
			{
				onContextDataReceived(context, intent);
			}
			else if (intent.getAction().equals(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED))
			{
				onOtherUserContextReceived(context, intent);
			}
			else
			{
				Log.e("", "Unexpected Action: " + intent.getAction());
			}
		}
			
		private void onCommThreadConnected(Context context, Intent intent)
		{
			String ipAddress = intent.getStringExtra(AndroidCommManager.EXTRA_IP_ADDRESS);
			int    port      = intent.getIntExtra(AndroidCommManager.EXTRA_PORT, -1);
		}
		
		private void onChannelSubscribed(Context context, Intent intent)
		{
			String channel = intent.getStringExtra(AndroidCommManager.EXTRA_CHANNEL);
		}
		
		private void onContextDataReceived(Context context, Intent intent)
		{
			// Extracts the values from the intent
			String   contextType = intent.getStringExtra(ContextData.CONTEXT_TYPE);
			String   deviceID    = intent.getStringExtra(ContextData.DEVICE_ID);
			String[] values      = intent.getStringArrayExtra(ContextData.PAYLOAD);
		}
		
		private void onOtherUserContextReceived(Context context, Intent intent)
		{
			// This is the Raw JSON from the Device
			String json = intent.getStringExtra(BluewaveManager.EXTRA_OTHER_USER_CONTEXT);
			
			// Creates a Parser
			JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		
			// TODO:  Analyze the Context Here (or pass it off to another part of the application)
		}
	}
}
