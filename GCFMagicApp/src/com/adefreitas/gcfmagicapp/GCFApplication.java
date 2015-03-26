package com.adefreitas.gcfmagicapp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import com.adefreitas.androidbluewave.BluewaveManager;
import com.adefreitas.androidbluewave.JSONContextParser;
import com.adefreitas.androidbluewave.PersonalContextProvider;
import com.adefreitas.androidframework.AndroidBatteryMonitor;
import com.adefreitas.androidframework.AndroidCommManager;
import com.adefreitas.androidframework.AndroidGroupContextManager;
import com.adefreitas.androidframework.ContextReceiver;
import com.adefreitas.androidframework.toolkit.CloudStorageToolkit;
import com.adefreitas.androidframework.toolkit.SftpToolkit;
import com.adefreitas.gcfmagicapp.lists.AppInfo;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.liveos.ApplicationFunction;
import com.adefreitas.liveos.ApplicationSettings;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ContextData;
import com.adefreitas.miscproviders.GoogleCalendarProvider;
import com.google.gson.Gson;

public class GCFApplication extends Application
{
	// Application Constants
	public static final String LOG_NAME          	 = "MAGIC_APP"; 
	public static final String IDENTITY_CONTEXT_TAG  = "identity";
	public static final String LOCATION_CONTEXT_TAG  = "location";
	public static final String TIME_CONTEXT_TAG	     = "time";
	public static final String ACTIVITY_CONTEXT_TAG  = "activity";
	public static final String SNAP_TO_IT_TAG    	 = "snap-to-it";
	public static final int    UPDATE_SECONDS    	 = 15;
	public static final String ACTION_APP_UPDATE 	 = "APP_UPDATE";
	public static final String ACTION_IMAGE_UPLOADED = "STI_IMAGE_UPLOADED";
	public static final String DOWNLOAD_FOLDER       = "/Download/Impromptu/";							 			     // Path on the Phone where Files are Downloaded
	public static final String UPLOAD_SFTP_PATH      = "/var/www/html/gcf/universalremote/magic/";		 			     // Folder Path on the Cloud Server
	public static final String UPLOAD_WEB_PATH   	 = "http://" + Settings.DEV_SFTP_IP + "/gcf/universalremote/magic/"; // Web Path to the Path Above
	
	// GCF Communication Settings (Connection to DNS)
	public static final CommMode COMM_MODE  = CommMode.MQTT;
	public static final String   IP_ADDRESS = Settings.DEV_MQTT_IP;
	public static final int      PORT 	    = Settings.DEV_MQTT_PORT;
	public static final String   DEV_NAME   = Settings.getDeviceName(android.os.Build.SERIAL);
		
	// GCF Variables
	public AndroidBatteryMonitor      batteryMonitor;
	public AndroidGroupContextManager groupContextManager;
	
	// App Specific Context Providers
	public GoogleCalendarProvider calendarProvider;
	
	// Object Serialization
	private Gson gson;
	
	// Bluewave
	public BluewaveManager bluewaveManager;
	
	// Cloud Storage Settings
	private CloudStorageToolkit cloudToolkit;
	
	// Intent Filters
	private ContextReceiver contextReceiver;
	private IntentFilter    filter;
	private IntentReceiver  intentReceiver;
	
	// Application Specific Tools
	private HashMap<String, String> preferences;
	private ArrayList<AppInfo> 	    appCatalog;
	private ArrayList<AppInfo>		activeApps;
	
	// Timer
	private TimerHandler timerHandler;
	
	// Snap To It
	private Date    lastSnapToItDeviceContact;
	private Date    lastPhotoTaken;
	private Date    lastAutoLaunch;
	
	/**
	 * One-Time Application Initialization Method
	 */
	@Override
	public void onCreate() 
	{
		super.onCreate();
		
		// Initializes Timestamp
		this.lastSnapToItDeviceContact = new Date(0);
		this.lastAutoLaunch     = new Date(0);
		this.lastPhotoTaken     = new Date(0);
		
		// Creates the Group Context Manager, which is Responsible for Context Producing and Sharing
		batteryMonitor 		= new AndroidBatteryMonitor(this, DEV_NAME, 5);
		groupContextManager = new AndroidGroupContextManager(this, DEV_NAME, batteryMonitor, false);
			
		// Creates GSON
		this.gson = new Gson();
		
		// Connects to Default DNS Channel
		String connectionKey = groupContextManager.connect(COMM_MODE, IP_ADDRESS, PORT);
		groupContextManager.subscribe(connectionKey, ApplicationSettings.DNS_CHANNEL);
		
		// Creates Context Providers
		calendarProvider = new GoogleCalendarProvider(groupContextManager, this.getContentResolver());
		groupContextManager.registerContextProvider(calendarProvider);
		
		// EXPERIMENTAL:  Initializes Bluewave
		bluewaveManager = new BluewaveManager(this, groupContextManager, "http://gcf.cmu-tbank.com/" + groupContextManager.getDeviceID() + ".txt");
		//bluewaveManager.startScan();
		
		// Creates the Cloud Toolkit Helper
		//dropboxToolkit = new DropboxToolkit(this, APP_KEY, APP_SECRET, AUTH_TOKEN);
		cloudToolkit = new SftpToolkit(this);
		
		// Stores Preferences
		this.preferences = new HashMap<String, String>();
		this.appCatalog  = new ArrayList<AppInfo>();
		this.activeApps  = new ArrayList<AppInfo>();
		
		// Create Intent Filter and Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED);
		this.filter.addAction(AndroidGroupContextManager.ACTION_GCF_OUTPUT);
		this.filter.addAction(BluewaveManager.ACTION_USER_CONTEXT_UPDATED);
		this.filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
		this.filter.addAction(BluewaveManager.ACTION_COMPUTE_INSTRUCTION_RECEIVED);
		this.filter.addAction(AndroidCommManager.ACTION_CHANNEL_SUBSCRIBED);
		this.filter.addAction(ACTION_IMAGE_UPLOADED);
		this.registerReceiver(intentReceiver, filter);
		
		// Creates the Scheduled Event Timer	
		timerHandler = new TimerHandler(this, groupContextManager, bluewaveManager);
				
		// Performs an Initial Context Update
		setPersonalContext(null);
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
	 * Returns the Bluewave Manager
	 * @return
	 */
	public BluewaveManager getBluewaveManager()
	{
		return bluewaveManager;
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
	public void createNotification(String title, String subtitle)
	{
		Intent intent = new Intent(this.getApplicationContext(), MainActivity.class);
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) 
		{
			 Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			
			 PendingIntent 		 pendingIntent 		 = PendingIntent.getActivity(this, 0, intent, 0);
			 NotificationManager notificationManager = (NotificationManager)this.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
			 Notification 		 note 				 = new Notification.Builder(this)
			 	.setSmallIcon(R.drawable.ic_notification)
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
	
	// Context Update Methods -------------------------------------------------------------
	private void setPersonalContext(String photoFilePath)
	{		
		try
		{
			// Updates Via Bluewave
			bluewaveManager.getPersonalContextProvider().setContext(IDENTITY_CONTEXT_TAG, getIdentityContext());
			bluewaveManager.getPersonalContextProvider().setContext(LOCATION_CONTEXT_TAG, getLocationContext());
			bluewaveManager.getPersonalContextProvider().setContext(TIME_CONTEXT_TAG, getTimeContext());
			bluewaveManager.getPersonalContextProvider().setContext(ACTIVITY_CONTEXT_TAG, getActivityContext());
			bluewaveManager.getPersonalContextProvider().setContext(SNAP_TO_IT_TAG, getSnapToItContext(photoFilePath));
			
			// PUBLISHES THE CHANGES AT ONCE
			bluewaveManager.getPersonalContextProvider().publish();
		}
		catch (Exception ex)
		{
			Toast.makeText(this, "Error Creating Context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private JSONObject getLocationContext()
	{
		JSONObject location = new JSONObject();
		
		try
		{
			// TODO:  Experimental Location Code
			LocationManager lm        = (LocationManager) getSystemService(Context.LOCATION_SERVICE);  
	        List<String>    providers = lm.getProviders(true);

	        /* Loop over the array backwards, and if you get an accurate location, then break out the loop*/
	        Location l = null;
	        
	        for (int i=providers.size()-1; i>=0; i--) 
	        {
	                l = lm.getLastKnownLocation(providers.get(i));
	                if (l != null) break;
	        }

	        if (l != null) 
	        {
	        	location.put("LATITUDE", l.getLatitude());
	            location.put("LONGITUDE", l.getLongitude());
	        }
		}
		catch (Exception ex)
		{
			Toast.makeText(this, "Could not write location context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
		}
        
        return location;
	}
	
	private JSONObject getActivityContext()
	{
		JSONObject activity = new JSONObject();
		
		try
		{
			
		}
		catch (Exception ex)
		{
			Toast.makeText(this, "Could not write activity context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		return activity;
	}
	
	private JSONObject getIdentityContext()
	{
		JSONObject identity = new JSONObject();
		
		try
		{
			identity.put("COMM_MODE", COMM_MODE);
			identity.put("IP_ADDRESS", IP_ADDRESS);
			identity.put("PORT", PORT);
		}
		catch (Exception ex)
		{
			Toast.makeText(this, "Could not write identity context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		return identity;
	}
	
	private JSONObject getTimeContext()
	{
		JSONObject time = new JSONObject();
		
		try
		{
			time.put("SYSTEM_CLOCK", new Date().getTime());
			time.put("TIMEZONE", Calendar.getInstance().getTimeZone().getDisplayName(Locale.getDefault()));
		}
		catch (Exception ex)
		{
			Toast.makeText(this, "Could not write activity context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		return time;
	}
	
	private JSONObject getSnapToItContext(String photoFilePath)
	{
		JSONObject snaptoit = new JSONObject();
		
		try
		{
			// Sets Snap To It Value
	        if (photoFilePath != null)
	        {
	        	snaptoit.put("PHOTO", photoFilePath);
	        	snaptoit.put("TIMESTAMP", this.getLastPhotoTimestamp().getTime());
	        }
	        else
	        {
	        	this.bluewaveManager.getPersonalContextProvider().removeContext(SNAP_TO_IT_TAG);
	        }
		}
		catch (Exception ex)
		{
			Toast.makeText(this, "Could not write snap-to-it context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		return snaptoit;
	}
	
	// Running Application Methods --------------------------------------------------------------
	public void addActiveApplication(AppInfo activeApp)
	{
		if (!activeApps.contains(activeApp))
		{
			activeApps.add(activeApp);
		}
	}
	
	public ArrayList<AppInfo> getActiveApplications()
	{
		return activeApps;
	}
	
	public void removeActiveApplications()
	{
		activeApps.clear();
	}
	
	// Preference Methods -----------------------------------------------------------------------
	public void setPreference(String name, String value)
	{
		this.preferences.put(name, value);
	}
	
	public String getPreference(String name)
	{
		return this.preferences.get(name);
	}
		
	// Catalog Methods --------------------------------------------------------------------------
	public void addApplicationToCatalog(AppInfo newApp)
	{
		// Index of the Old App (If it Exists)
		boolean found = false;
		
		// Looks for the Old App
		for (AppInfo app : appCatalog)
		{
			if (newApp.getAppID().equals(app.getAppID()))
			{
				found = true;
				app.update(newApp);
			}
		}
		
		// Adds the New App If It Does Not Yet Exist
		if (!found)
		{
			appCatalog.add(newApp);
			
			if (contextReceiver == null)
			{
				createNotification("Impromptu Update", "Received New App: " + newApp.getAppName());
			}
		}
	}
	
	public AppInfo getApplicationFromCatalog(String appID)
	{
		for (AppInfo app : appCatalog)
		{
			if (app.getAppID().equals(appID))
			{
				return app;
			}
		}
		
		return null;
	}
	
	public ArrayList<AppInfo> getApplicationCatalog()
	{
		return appCatalog;
	}
	
	public void clearApplicationCatalog()
	{
		appCatalog.clear();
		
		// Notifies the Application that the App List has Changed (or been erased)
		Intent i = new Intent(ACTION_APP_UPDATE);
		GCFApplication.this.sendBroadcast(i);
	}
	
	private void updateCatalog()
	{
		boolean changed = false;
		
		// Updates App Catalog Contents
		// Looking for Canceled Application
		for (AppInfo app : new ArrayList<AppInfo>(appCatalog))
		{
			if (new Date().getTime() > app.getDateExpires().getTime() && !activeApps.contains(app) && !app.isFavorite())
			{
				getApplicationCatalog().remove(app);
				changed = true;
			}
		}
		
		// Notifies Activities if Something Changed
		if (changed)
		{
			// Notifies the Application that the App List has Changed
			Intent i = new Intent(ACTION_APP_UPDATE);
			sendBroadcast(i);
		}
	}
	
	// Snap-To-It Methods -----------------------------------------------------------------------
	public Date getLastSnapToItDeviceContact() 
	{
		return lastSnapToItDeviceContact;
	}
	
	public Date getLastPhotoTimestamp()
	{
		return lastPhotoTaken;
	}
	
	public void clearSnapToItHistory()
	{
		setPersonalContext(null);
	}
		
	public void setPhotoTaken()
	{
		lastPhotoTaken = new Date();
	}
	
	public void setAutoLaunch()
	{
		lastAutoLaunch = new Date();
	}
	
	public boolean shouldAutoLaunch()
	{
		return lastPhotoTaken != null && lastAutoLaunch != null && lastPhotoTaken.getTime() > lastAutoLaunch.getTime();
	}
		
	// Group Context Framework Methods ----------------------------------------------------------
	public void setContextReceiver(ContextReceiver newContextReceiver)
	{
		this.contextReceiver = newContextReceiver;
	}
		
	// Intent Receiver --------------------------------------------------------------------------
	private class IntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED))
			{
				onContextDataReceived(context, intent);
			}
			else if (intent.getAction().equals(AndroidGroupContextManager.ACTION_GCF_OUTPUT))
			{
				onOutput(context, intent);
			}
			else if (intent.getAction().equals(BluewaveManager.ACTION_USER_CONTEXT_UPDATED))
			{
				onUserContextUpdated(context, intent);
			}
			else if (intent.getAction().equals(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED))
			{
				onOtherUserContextReceived(context, intent);
			}
			else if (intent.getAction().equals(BluewaveManager.ACTION_COMPUTE_INSTRUCTION_RECEIVED))
			{
				onComputeInstructionReceived(context, intent);
			}
			else if (intent.getAction().equals(AndroidCommManager.ACTION_CHANNEL_SUBSCRIBED))
			{
				onChannelSubscribed(context, intent);
			}
			else if (intent.getAction().equals(ACTION_IMAGE_UPLOADED))
			{
				onSnapToItUploadComplete(context, intent);
			}
			else
			{
				Log.e("", "Unknown Action: " + intent.getAction());
			}
		}
	
		private void onContextDataReceived(Context context, Intent intent)
		{
			// Extracts the values from the intent
			String   contextType = intent.getStringExtra(ContextData.CONTEXT_TYPE);
			String   deviceID    = intent.getStringExtra(ContextData.DEVICE_ID);
			String   description = intent.getStringExtra(ContextData.DESCRIPTION);
			String[] values      = intent.getStringArrayExtra(ContextData.VALUES);
			
			// Forwards Values to the ContextReceiver for Processing
			if (contextReceiver != null)
			{
				contextReceiver.onContextData(new ContextData(contextType, deviceID, description, values));
			}
		}
	
		private void onOutput(Context context, Intent intent)
		{
			// Extracts the values from the intent
			String text = intent.getStringExtra(AndroidGroupContextManager.GCF_OUTPUT);
			
			// Forwards Values to the Application for Processing
			if (contextReceiver != null)
			{
				contextReceiver.onGCFOutput(text);
			}
		}
	
		private void onUserContextUpdated(Context context, Intent intent)
		{
			
		}
	
		private void onOtherUserContextReceived(Context context, Intent intent)
		{
			// This is the Raw JSON from the Device
			String json = intent.getStringExtra(BluewaveManager.OTHER_USER_CONTEXT);
			
			// Creates a Parser
			JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
			
			// Determines if an Application is Snap-To-It Accessible
			if (parser.getJSONObject(SNAP_TO_IT_TAG) != null)
			{
				lastSnapToItDeviceContact = new Date();
				
				Intent appUpdateIntent = new Intent(ACTION_APP_UPDATE);
				GCFApplication.this.sendBroadcast(appUpdateIntent);
			}
			
			// Forwards Values to the Application for Processing
			if (contextReceiver != null)
			{
				contextReceiver.onBluewaveContext(parser);
			}
		}
	
		private void onComputeInstructionReceived(Context context, Intent intent)
		{
			String   command    = intent.getExtras().getString(PersonalContextProvider.COMPUTE_COMMAND);
			String   sender     = intent.getExtras().getString(PersonalContextProvider.COMPUTE_SENDER);
			String[] parameters = intent.getExtras().getStringArray(PersonalContextProvider.COMPUTE_PARAMETERS);
			
			// Registers an Application in the Catalog
			if (command.equalsIgnoreCase("APPLICATION"))
			{
				try
				{
					String 			  appID       	 = CommMessage.getValue(parameters, "APP_ID");
					String			  appContextType = CommMessage.getValue(parameters, "APP_CONTEXT_TYPE");
					String 			  appName     	 = CommMessage.getValue(parameters, "NAME");
					String			  deviceID		 = CommMessage.getValue(parameters, "DEVICE_ID");
					String 			  description    = CommMessage.getValue(parameters, "DESCRIPTION");
					String 			  category	     = CommMessage.getValue(parameters, "CATEGORY");
					String 			  logo		  	 = CommMessage.getValue(parameters, "LOGO");
					Integer			  lifetime		 = Integer.getInteger(CommMessage.getValue(parameters, "LIFETIME"), 60);
					ArrayList<String> contexts    	 = CommMessage.getValues(parameters, "CONTEXTS");
					ArrayList<String> preferences 	 = CommMessage.getValues(parameters, "PREFERENCES");
					CommMode		  commMode		 = CommMode.valueOf(CommMessage.getValue(parameters, "COMM_MODE"));
					String 			  ipAddress   	 = CommMessage.getValue(parameters, "APP_ADDRESS");
					int				  port		  	 = Integer.valueOf(CommMessage.getValue(parameters, "APP_PORT"));
					String 			  channel    	 = CommMessage.getValue(parameters, "APP_CHANNEL");
					Double			  photoMatches   = (CommMessage.getValue(parameters, "PHOTO_MATCHES") != null) ? Double.valueOf(CommMessage.getValue(parameters, "PHOTO_MATCHES")) : 0.0;

					// Creates Individual Function Objects
					ArrayList<ApplicationFunction> functions    = new ArrayList<ApplicationFunction>();
					String 						   functionJSON = CommMessage.getValue(parameters, "FUNCTIONS");
					
					if (functionJSON != null && !functionJSON.equals("null"))
					{					
						JSONArray functionArray = new JSONArray(functionJSON);
						
						for (int i=0; i<functionArray.length(); i++)
						{
							// Gets Each Object Element and Converts It to an 
							JSONObject 			functionElement = (JSONObject)functionArray.get(i);
							ApplicationFunction function 	    = gson.fromJson(functionElement.toString(), ApplicationFunction.class);
						
							if (function != null)
							{
								functions.add(function);	
							}
						}
					}
											
					// Adds the App
					AppInfo app = new AppInfo(appID, appContextType, deviceID, appName, description, category, logo, lifetime, photoMatches, contexts, preferences, functions, commMode, ipAddress, port, channel);
										
					GCFApplication.this.addApplicationToCatalog(app);
					
					// Notifies the Application that the App List has Changed
					Intent i = new Intent(ACTION_APP_UPDATE);
					GCFApplication.this.sendBroadcast(i);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					Toast.makeText(GCFApplication.this, "Problem With App: " + ex.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
	
		private void onChannelSubscribed(Context context, Intent intent)
		{
			String channel = intent.getStringExtra("CHANNEL");
			
			if (channel.equals(ApplicationSettings.DNS_CHANNEL))
			{
				if (!timerHandler.isRunning())
				{
					timerHandler.start(UPDATE_SECONDS * 1000);
				}
			}
		}
	
		private void onSnapToItUploadComplete(Context context, Intent intent)
		{
			String uploadPath = intent.getStringExtra(CloudStorageToolkit.CLOUD_UPLOAD_PATH);
			Toast.makeText(GCFApplication.this, "Uploaded Complete: " + uploadPath, Toast.LENGTH_SHORT).show();
			
		    // Makes the File Visible!
		    MediaScannerConnection.scanFile(GCFApplication.this, new String[] { uploadPath }, null, null);
			
		    // Erases All Applications
			clearApplicationCatalog();
			
			// Updates Context with the New Upload Path
			setPersonalContext(uploadPath);		
			
			// Sends a New Query!
			sendQuery(groupContextManager, bluewaveManager);
		}
	}
	
	private static void sendQuery(GroupContextManager gcm, BluewaveManager bluewaveManager)
	{
		JSONContextParser context = bluewaveManager.getPersonalContextProvider().getContext(); 
		
		if (context != null)
		{
			gcm.sendComputeInstruction("LOS_DNS", new String[] { "LOS_DNS" }, "QUERY", new String[] { "CONTEXT=" + context.toString() });	
		}
	}
	
	// Timed Event ------------------------------------------------------------------------------
	/**
	 * This Class Allows the App To Update Its Context Once per Interval
	 * @author adefreit
	 */
	static class TimerHandler extends Handler
	{
		private long 	 delayTime;
		private Runnable scheduledTask;
		private boolean  running;
		
		public TimerHandler(GCFApplication app, AndroidGroupContextManager gcm, BluewaveManager bw)
		{
			final GCFApplication			 application		 = app;
			final AndroidGroupContextManager groupContextManager = gcm;
			final BluewaveManager			 bluewaveManager	 = bw;
			
			running       = false;
			scheduledTask = new Runnable() 
			{
				public void run() 
				{ 									
					// Updates the Application Catalog to Remove Expired Entries
					application.updateCatalog();
					
					// Updates the Device's Personal Context
					application.setPersonalContext(null);
					
					//Toast.makeText(application, "Running Scheduled Task", Toast.LENGTH_SHORT).show();
					sendQuery(groupContextManager, bluewaveManager);
					
					bluewaveManager.updateBluetoothName();
					//bluewaveManager.getPersonalContextProvider().publish();
					
					// This Runs Once Every X Seconds
					postDelayed(this, delayTime);
				}
			};
		}
		
		public void start(long delayTime)
		{
			// Stops Any Existing Delays
			stop();
			
			this.delayTime = delayTime;
			
			// Creates the Next Task Instance
			postDelayed(scheduledTask, 100);
			
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
