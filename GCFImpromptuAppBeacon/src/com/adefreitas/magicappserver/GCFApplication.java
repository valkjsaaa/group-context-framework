package com.adefreitas.magicappserver;

import java.util.ArrayList;

import org.json.JSONArray;
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
import com.adefreitas.androidframework.AndroidCommManager;
import com.adefreitas.androidframework.AndroidGroupContextManager;
import com.adefreitas.androidframework.ContextReceiver;
import com.adefreitas.androidframework.toolkit.CloudStorageToolkit;
import com.adefreitas.androidliveos.AndroidApplicationProvider;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.ContextType;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.inoutboard.App_Identity;
import com.adefreitas.inoutboard.UserIdentityContextProvider;
import com.adefreitas.liveos.ApplicationProvider;
import com.adefreitas.liveos.ApplicationSettings;
import com.adefreitas.messages.ContextData;

public class GCFApplication extends Application
{
	// Application Constants
	public static final String  APP_TICK 			   = "TICK";
	public static final String  LOG_NAME 			   = "GCF_APP"; 
	public static final int     SCAN_PERIOD_IN_SECONDS = 60;
	
	// GCF Communication Settings (BROADCAST_MODE Assumes a Functional TCP Relay Running)
	public static final CommMode COMM_MODE  = CommMode.MQTT;
	public static final String   IP_ADDRESS = Settings.DEV_MQTT_IP;
	public static final int      PORT 	    = Settings.DEV_MQTT_PORT;
	public static final String 	 DEV_NAME   = Settings.getDeviceName(android.os.Build.SERIAL);
		
	// GCF Variables
	public AndroidBatteryMonitor      batteryMonitor;
	public AndroidGroupContextManager groupContextManager;
	public String 					  connectionKey;
	
	// GCF Context Providers
	public UserIdentityContextProvider identityProvider;
	
	// Cloud Storage Settings
	private CloudStorageToolkit cloudToolkit;
	
	// Loaded Applications
	private ArrayList<ApplicationProvider> apps;
	
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
			
		// Initializes Bluewave
		groupContextManager.getBluewaveManager().setDiscoverable(false);
		groupContextManager.startBluewaveScan(SCAN_PERIOD_IN_SECONDS * 1000);
		
		// Creates a Custom Context Provider for Bluewave Data
		groupContextManager.registerContextProvider(new BluewaveContextProvider(this, groupContextManager));
		
		// Connects to the Server
		connectionKey = groupContextManager.connect(COMM_MODE, IP_ADDRESS, PORT);
		
		// Creates an Array of Context Receivers
		this.contextReceivers = new ArrayList<ContextReceiver>();
		
		// Initialization
		configureContextProviders();
		configureAppProviders();
		
		// Create Intent Filter and Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED);
		this.filter.addAction(AndroidGroupContextManager.ACTION_GCF_OUTPUT);
		this.filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
		this.filter.addAction(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED);
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
	private void configureContextProviders()
	{
		// Creates the Provider
		identityProvider = new UserIdentityContextProvider(groupContextManager);
		
		// Registers the Provider
		groupContextManager.registerContextProvider(identityProvider);
	}
	
	private void configureAppProviders()
	{
		// Creates the List of Apps
		apps = new ArrayList<ApplicationProvider>();
		
		// Creates Application Providers
		//apps.add(new App_ContactCard(this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT));
		//apps.add(new App_HomeLights(this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT));
		apps.add(new App_Identity(this, identityProvider, groupContextManager, COMM_MODE, IP_ADDRESS, PORT));
		//apps.add(new App_GameConnectMeFactory(this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT));
		
		// Listens on the Channel for these Applications
		for (ApplicationProvider app : apps)
		{
			groupContextManager.registerContextProvider(app);
			groupContextManager.subscribe(connectionKey, app.getContextType());
		}
		
		// Debug Message
		Log.d(LOG_NAME, apps.size() + " apps loaded.");
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
				String    deviceID   = parser.getDeviceID();
				CommMode  commMode   = CommMode.valueOf(context.getString("COMM_MODE")); 
				String    ipAddress  = context.getString("IP_ADDRESS");
				int       port       = context.getInt("PORT");
				
				String result = "";
				
				if (ipAddress != null)
				{
					for (ContextProvider p : groupContextManager.getRegisteredProviders())
					{
						if (p instanceof AndroidApplicationProvider)
						{
							AndroidApplicationProvider appProvider = (AndroidApplicationProvider)p;
							
							// Determines whether the App Provider Wants to Share this Application
							boolean redundant = isRedundant(appProvider, context);
							boolean sendData  = appProvider.sendAppData(parser.toString());
							
							Toast.makeText(this, appProvider.getContextType() + ": [" + deviceID + "] redundant: " + redundant + "; sendData: " + sendData, Toast.LENGTH_LONG).show();
							
							if (!redundant && sendData)
							{
								result += appProvider.getContextType() + " ";
								
								// Either Connects, or 
								if (!groupContextManager.isConnected(commMode, ipAddress, port))
								{
									groupContextManager.connect(commMode, ipAddress, port);
								}
								else
								{
									String connectionKey = groupContextManager.getConnectionKey(commMode, ipAddress, port);
									
									// Creates a List of Parameters
									ArrayList<String> parameters = new ArrayList<String>();
									
									for (String s : appProvider.getInformation(parser.toString()))
									{
										parameters.add(s);
									}
									
									// Adds a Custom Parameter Needed by the DNS to Know Which Device this Advertisement is For
									parameters.add("DESTINATION=" + deviceID);
									
									groupContextManager.sendComputeInstruction(
											connectionKey, 
											ApplicationSettings.DNS_CHANNEL, 
											"LOS_DNS", 
											new String[] { "LOS_DNS" }, 
											"SEND_ADVERTISEMENT", 
											parameters.toArray(new String[0]));	
								}
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
		if (!contextReceivers.contains(newContextReceiver))
		{
			contextReceivers.add(newContextReceiver);
		}
	}
	
	// Private Methods --------------------------------------------------------------------------
	private boolean isRedundant(AndroidApplicationProvider appProvider, JSONObject context)
	{
		if (context.has("APPS"))
		{
			try
			{
				JSONArray apps = context.getJSONArray("APPS");
				
				for (int i=0; i<apps.length(); i++)
				{			
					JSONObject appObject = apps.getJSONObject(i);
					
					if (appObject.getString("name").equals(appProvider.getContextType()) &&
						appObject.getInt("expires") >= SCAN_PERIOD_IN_SECONDS)
					{
						return true;
					}
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			
			return false;
		}
		
		return true;
	}
	
	// Intent Receiver --------------------------------------------------------------------------
	private class IntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED))
			{
				onGCFData(context, intent);
			}
			else if (intent.getAction().equals(AndroidGroupContextManager.ACTION_GCF_OUTPUT))
			{
				onOutput(context, intent);
			}
			else if (intent.getAction().equals(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED))
			{
				onOtherUserContextReceived(context, intent);
			}
			else if (intent.getAction().equals(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED))
			{
				onConnected(context, intent);
			}
			else
			{
				Log.e("", "Unknown Action: " + intent.getAction());
			}
		}
		
		private void onGCFData(Context context, Intent intent)
		{
			// Extracts the values from the intent
			String   contextType = intent.getStringExtra(ContextData.CONTEXT_TYPE);
			String   deviceID    = intent.getStringExtra(ContextData.DEVICE_ID);
			String[] values      = intent.getStringArrayExtra(ContextData.PAYLOAD);
			
			// Forwards Values to the ContextReceiver for Processing
			for (ContextReceiver contextReceiver : contextReceivers)
			{
				contextReceiver.onContextData(new ContextData(contextType, deviceID, values));
			}
		}
	
		private void onOutput(Context context, Intent intent)
		{
			// Extracts the values from the intent
			String text = intent.getStringExtra(AndroidGroupContextManager.GCF_OUTPUT);
			
			// Forwards Values to the Application for Processing
			for (ContextReceiver contextReceiver : contextReceivers)
			{
				contextReceiver.onGCFOutput(text);
			}
		}
	
		private void onOtherUserContextReceived(Context context, Intent intent)
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
	
		private void onConnected(Context context, Intent intent)
		{
			Toast.makeText(getApplicationContext(), "Connected to: " + 
				intent.getStringExtra(AndroidCommManager.EXTRA_IP_ADDRESS) + ":" + 
				intent.getIntExtra(AndroidCommManager.EXTRA_PORT, -1), Toast.LENGTH_SHORT).show();
		}
	}
}
