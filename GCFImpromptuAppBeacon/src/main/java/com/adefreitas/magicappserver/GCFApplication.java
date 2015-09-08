package com.adefreitas.magicappserver;

import java.util.ArrayList;
import java.util.Date;

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
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.beacon.apps.App_AndroidPrintProxy;
import com.adefreitas.beacon.apps.App_AndroidProjectorProxy;
import com.adefreitas.beacon.apps.App_NSHMap;
import com.adefreitas.beacon.inoutboard.UserIdentityContextProvider;
import com.adefreitas.beacon.iot.*;
import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.Settings;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.android.toolkit.CloudStorageToolkit;
import com.adefreitas.gcf.android.AndroidBatteryMonitor;
import com.adefreitas.gcf.android.AndroidCommManager;
import com.adefreitas.gcf.android.AndroidGroupContextManager;
import com.adefreitas.gcf.android.ContextReceiver;
import com.adefreitas.gcf.android.bluewave.BluewaveManager;
import com.adefreitas.gcf.android.bluewave.JSONContextParser;
import com.adefreitas.gcf.android.impromptu.AndroidApplicationProvider;
import com.adefreitas.gcf.android.providers.BluewaveContextProvider;
import com.adefreitas.gcf.android.providers.TemperatureContextProvider;
import com.adefreitas.gcf.impromptu.ApplicationProvider;
import com.adefreitas.gcf.impromptu.ApplicationSettings;
import com.adefreitas.gcf.messages.ContextData;
import com.adefreitas.gcf.messages.ContextRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GCFApplication extends Application
{
	// Application Constants
	public static final String  LOG_NAME 			    = "APP_BEACON"; 
	public static final String  PREFERENCES_NAME        = "com.adefreit.impromptu.appbeaconpreferences";
	public static final int     BLUEWAVE_UPDATE_SECONDS = 60;
	public static final int     SCAN_PERIOD_IN_SECONDS  = 30;
	public static final boolean BLUETOOTH_DISCOVERABLE  = true;
	
	// GCF Communication Settings (BROADCAST_MODE Assumes a Functional TCP Relay Running)
	public static final CommMode COMM_MODE  	 = CommMode.MQTT;
	public static final String   IP_ADDRESS 	 = Settings.DEV_MQTT_IP;
	public static final int      PORT 	    	 = Settings.DEV_MQTT_PORT;
	public static final String 	 DEV_NAME   	 = Settings.getDeviceName(android.os.Build.SERIAL);
	public static final String   APP_DIR_CONTEXT = "LOS_DNS";
		
	// GCF Bluewave Variables
	public static final String   BLUEWAVE_APP_ID   = "PRINT_SERVICES";
	public static final String[] BLUEWAVE_CONTEXTS = new String[] { "device", "identity", "face" };
	
	// GCF Variables
	public AndroidBatteryMonitor      batteryMonitor;
	public AndroidGroupContextManager groupContextManager;
	public String 					  connectionKey;
	
	// GCF Context Providers
	public UserIdentityContextProvider    identityProvider;
	public IOT_TabletContextProvider iotProvider;
	
	// GSON Serializer (Converts Java Objects to JSON, and vice versa)
	private Gson gson;
	
	// Cloud Storage Settings
	private CloudStorageToolkit cloudToolkit;
	
	// Loaded Applications
	private ArrayList<ApplicationProvider> apps;
	
	// Intent Filters
	private ArrayList<ContextReceiver> contextReceivers;
	private IntentFilter    		   filter;
	private IntentReceiver  		   intentReceiver;
	
	// Timer
	private AutoUpdateHandler timerHandler;
	
	/**
	 * One-Time Application Initialization Method
	 */
	@Override
	public void onCreate() 
	{
		super.onCreate();
		
		// Creates the Group Context Manager, which is Responsible for Context Producing and Sharing
		batteryMonitor 		= new AndroidBatteryMonitor(this, DEV_NAME, 5);
		groupContextManager = new AndroidGroupContextManager(this, DEV_NAME, batteryMonitor, false);
			
		// Initializes Bluewave
		groupContextManager.getBluewaveManager().setDiscoverable(BLUETOOTH_DISCOVERABLE);
		groupContextManager.startBluewaveScan(SCAN_PERIOD_IN_SECONDS * 1000);
		groupContextManager.getBluewaveManager().setCredentials(BLUEWAVE_APP_ID, BLUEWAVE_CONTEXTS);
		
		// Creates a Custom Context Provider for Bluewave Data
		groupContextManager.registerContextProvider(new BluewaveContextProvider(this, groupContextManager, 60000));
		
		// Connects to the Server
		connectionKey = groupContextManager.connect(COMM_MODE, IP_ADDRESS, PORT);
		groupContextManager.subscribe(connectionKey, "IN_OUT_SIGN");
		
		// Creates an Array of Context Receivers
		this.contextReceivers = new ArrayList<ContextReceiver>();
		
		// Initializing Sensors and Services
		configureContextProviders();
		configureAppProviders();
				
		// Initializing Serialization Tools
		gson = new Gson();
		
		// Create Intent Filter and Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED);
		this.filter.addAction(AndroidGroupContextManager.ACTION_GCF_OUTPUT);
		this.filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
		this.filter.addAction(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED);
		this.filter.addAction(BluewaveManager.ACTION_USER_CONTEXT_UPDATED);
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
	
	/**
	 * Returns the Current Connection Key
	 * @return
	 */
	public String getConnectionKey()
	{
		return connectionKey;
	}
	
	// ------------------------------------------------------------------------------------------------
	// Application Initialization Methods
	// ------------------------------------------------------------------------------------------------
	/**
	 * Adds Context Providers (i.e., "sensors") to the Beacon
	 */
	private void configureContextProviders()
	{
		// Application Preferences
		SharedPreferences appSharedPreferences = this.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
		
		// Creates the Provider(s)
		identityProvider = new UserIdentityContextProvider(groupContextManager, appSharedPreferences);
		iotProvider      = new IOT_TabletContextProvider(this, groupContextManager, "phone");
		
		// Registers the Provider(s)
		//groupContextManager.registerContextProvider(identityProvider);
		//groupContextManager.registerContextProvider(new TemperatureContextProvider(groupContextManager, this));
		groupContextManager.registerContextProvider(iotProvider);
	}
	
	/**
	 * Adds Applications (i.e., "services") to the Beacon
	 */
	private void configureAppProviders()
	{
		// Creates a List of Apps
		apps = new ArrayList<ApplicationProvider>();
		
		// Creates Application Providers
		//apps.add(new App_AndroidPrintProxy("PEWTER", this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT, "STI_PRINTER_PEWTER"));
		//apps.add(new App_AndroidPrintProxy("ZIRCON", this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT, "STI_PRINTER_ZIRCON"));
		//apps.add(new App_AndroidPrintProxy("NSH Color 3508", this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT, "STI_PRINTER_NSH Color 3508"));
		//apps.add(new App_AndroidPrintProxy("DEVLAB", this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT, "STI_PRINTER_DEVLAB"));
		//apps.add(new App_AndroidProjectorProxy(this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT, "STI_DIGITAL_PROJECTOR"));
		
		//apps.add(new App_NSHMap(this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT));
		//apps.add(new App_RoomSensors(this, groupContextManager, "HCI Commons", COMM_MODE, IP_ADDRESS, PORT));
		//apps.add(new App_InOutBoardIdentity(this, identityProvider, groupContextManager, COMM_MODE, IP_ADDRESS, PORT));
		//apps.add(new App_ContactCard(this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT));
		//apps.add(new App_HomeLights(this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT));
		//apps.add(new App_GameConnectMeFactory(this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT));
		//apps.add(new App_TestApp(this, groupContextManager, COMM_MODE, IP_ADDRESS, PORT));
		
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
	/**
	 * This Method is Called Each Time the Device Receives Bluewave Information
	 * @param parser
	 */
	public void onBluewaveContext(JSONContextParser parser)
	{
		// Gives Context to Beacon Apps
		// This allows them to see if they should be "installed" on the user's device
		analyzeBluewaveContext(parser);
				
		// Creates a Toast
		Toast.makeText(this, "Discovered: " + parser.getDeviceID(), Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * This method is used by an activity to allow it to receive context without having to create an intent filter
	 * @param newContextReceiver
	 */
	public void setContextReceiver(ContextReceiver newContextReceiver)
	{		
		if (!contextReceivers.contains(newContextReceiver))
		{
			Log.d(LOG_NAME, "Adding new Context Receiver: " + newContextReceiver.toString());
			contextReceivers.add(newContextReceiver);
		}
		else
		{
			Log.d(LOG_NAME, "Ignoring Context Receiver: " + newContextReceiver.toString());
		}
	}
	
	/**
	 * This method is used by an activity to allow it to stop receiving context
	 * @param contextReceiver
	 */
	public void removeContextReceiver(ContextReceiver contextReceiver)
	{
		if (contextReceivers.contains(contextReceiver))
		{
			Log.d(LOG_NAME, "Removing Context Receiver: " + contextReceiver.toString());
			contextReceivers.remove(contextReceiver);
		}
	}
	
	// Private Methods --------------------------------------------------------------------------
	/**
	 * This method allows all installed beacon apps to see the Bluewave context
	 * @param parser
	 */
	private void analyzeBluewaveContext(JSONContextParser parser)
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
					ArrayList<String> appPayload = new ArrayList<String>();
					
					for (ContextProvider p : groupContextManager.getRegisteredProviders())
					{	
						if (p instanceof AndroidApplicationProvider)
						{
							AndroidApplicationProvider appProvider = (AndroidApplicationProvider)p;
							
							// Determines whether the App Provider Wants to Share this Application
							boolean redundant = isRedundant(appProvider, context);
							boolean sendData  = appProvider.sendAppData(parser.toString());
							
							//Toast.makeText(this, appProvider.getContextType() + ": [" + deviceID + "] redundant: " + redundant + "; sendData: " + sendData, Toast.LENGTH_LONG).show();
							
							if (!redundant && sendData)
							{
								result += appProvider.getContextType() + " ";
								
								appPayload.add(gson.toJson(appProvider.getInformation(parser.toString())));
							}
						}
					}
					
					// Transmits All of the Payloads at Once
					if (appPayload.size() > 0)
					{
						String connectionKey = "";
						
						// Either Connects, or Uses the Existing Connection
						if (!groupContextManager.isConnected(commMode, ipAddress, port))
						{
							connectionKey = groupContextManager.connect(commMode, ipAddress, port);
						}
						else
						{
							connectionKey = groupContextManager.getConnectionKey(commMode, ipAddress, port);
						}
						
						// Sends ONE Message with Everything :)
						groupContextManager.sendComputeInstruction(
								connectionKey, 
								ApplicationSettings.DNS_CHANNEL, 
								APP_DIR_CONTEXT, 
								new String[] { "LOS_DNS" }, 
								"SEND_ADVERTISEMENT", 
								new String[] { "DESTINATION=" + deviceID, "APPS=" + gson.toJson(appPayload.toArray(new String[0])) });	
					}
					
					if (result.length() > 0)
					{
						Toast.makeText(this, "Sending app advertisements [" + result.trim() + "] to: " + deviceID, Toast.LENGTH_LONG).show();
					}
				}
			}
			catch (Exception ex)
			{
				Toast.makeText(this, "Problem Analyzing Context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
			}	
		}
	}
	
	/**
	 * This method is used to determine if an app has already been sent to a device
	 * @param appProvider
	 * @param context
	 * @return
	 */
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
					
					if (appObject.getString("name").equals(appProvider.getContextType()))
					{
						if (appObject.has("expiring"))
						{
							return !appObject.getBoolean("expiring");
						}
						else
						{
							return false;
						}
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
			else if (intent.getAction().equals(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED))
			{
				onOtherUserContextReceived(context, intent);
			}
			else if (intent.getAction().equals(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED))
			{
				onConnected(context, intent);
			}
			else if (intent.getAction().equals(BluewaveManager.ACTION_USER_CONTEXT_UPDATED))
			{
				
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
			
			ContextData data = new ContextData(contextType, deviceID, values);
			
			if (data.getContextType().equals("POSTURE"))
			{
				iotProvider.speak(data.getPayload("VALUE"));
			}
			
			// Forwards Values to the ContextReceiver for Processing
			for (ContextReceiver contextReceiver : contextReceivers)
			{
				contextReceiver.onContextData(data);
			}
		}

		private void onOtherUserContextReceived(Context context, Intent intent)
		{
			// This is the Raw JSON from the Device
			String json = intent.getStringExtra(BluewaveManager.EXTRA_OTHER_USER_CONTEXT);
			
			// Creates a Parser
			JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
			
			// Handles Context Internally
			onBluewaveContext(parser);
			
			// Forwards Values to the Application for Processing
			for (ContextReceiver contextReceiver : contextReceivers)
			{
				Log.d(LOG_NAME, "Sending Bluewave Context to: " + contextReceiver.toString());
				contextReceiver.onBluewaveContext(parser);
			}
		}
	
		private void onConnected(Context context, Intent intent)
		{
			Toast.makeText(getApplicationContext(), "Connected to: " + 
				intent.getStringExtra(AndroidCommManager.EXTRA_IP_ADDRESS) + ":" + 
				intent.getIntExtra(AndroidCommManager.EXTRA_PORT, -1), Toast.LENGTH_SHORT).show();
			
			// Creates the Scheduled Event Timer
			if (timerHandler == null)
			{
				timerHandler = new AutoUpdateHandler(GCFApplication.this);				
				timerHandler.start();	
			}
			
			//groupContextManager.sendRequest("POSTURE", ContextRequest.SINGLE_SOURCE, new String[] { }, 2000, new String[] { "CHANNEL=dev/" + groupContextManager.getDeviceID() });
		}
	}

	// Timed Event ------------------------------------------------------------------------------
	/**
	 * This Class Allows the App To Update Its Context Once per Interval
	 * @author adefreit
	 */
	private static class AutoUpdateHandler extends Handler
	{
		private boolean 		     running;
		private final GCFApplication app;
		
		public AutoUpdateHandler(final GCFApplication app)
		{			
			running  = false;
			this.app = app;
		}
		
		private Runnable scheduledTask = new Runnable() 
		{	
			public void run() 
			{ 	
				if (running)
				{
					// By Default:  Next Update Occurs According to the Value Specified in GCFApplication.java
					Date nextExecute = new Date(System.currentTimeMillis() + BLUEWAVE_UPDATE_SECONDS * 1000);
					// Sets the Context
					AndroidGroupContextManager gcm = (AndroidGroupContextManager)app.getGroupContextManager();
					try
					{
						JSONArray array = new JSONArray();
						
						for (ContextProvider contextProvider : gcm.getRegisteredProviders())
						{
							if (contextProvider instanceof IOTContextProvider)
							{
								IOTContextProvider iot = (IOTContextProvider)contextProvider;
								array.put(iot.getJSON());	
							}
						}

                        JSONObject faceContent = new JSONObject()
                            .put("name", "Jackie Yang")
                            .put("pictures", new JSONArray()
                                    .put("http://i1233.photobucket.com/albums/ff382/valkjsaaa/face989669909_1.png")
                                    .put("http://i1233.photobucket.com/albums/ff382/valkjsaaa/face2006286377_1.png"));

//						JSONObject faceContent = new JSONObject()
//								.put("name", "Adrian de Freitas")
//								.put("pictures", new JSONArray()
//										.put("https://dl.pushbulletusercontent.com/JEtPXbn3VzPQMHXnZIvRLtQb9My8cpyP/face1739439998.png")
//										.put("https://dl.pushbulletusercontent.com/Vo7ZaJSqhhJjVylU8cL2T7xa3PZO3IQ2/face1448919907.png")
//                                        .put("https://dl.pushbulletusercontent.com/aIBnlCB2OWNJPqBkHn6CEtziUp70bVP6/face665690445.png"));

						// Adds All of the Array Contents
						gcm.getBluewaveManager().getPersonalContextProvider().setContext("face", faceContent);
						gcm.getBluewaveManager().getPersonalContextProvider().publish();
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
					
					// Removes Any Existing Callbacks
					removeCallbacks(this);
					
					// Sleep Time Depends on Whether or Not the Application is in the Foreground
					postDelayed(this, nextExecute.getTime() - System.currentTimeMillis());
				}
			}
		};
		
		public void start()
		{
			// Stops Any Existing Delays
			stop();
			
			// Creates the Next Task Instance
			postDelayed(scheduledTask, 5000);
			
			running = true;
		}
		
		public void stop()
		{
			removeCallbacks(scheduledTask);	
			
			running = false;
		}
		
		public boolean isRunning()
		{
			return running;
		}
	}
}
