package com.adefreitas.gcfimpromptu;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.ValueCallback;
import android.widget.Toast;

import com.adefreitas.androidbluewave.BluewaveManager;
import com.adefreitas.androidbluewave.JSONContextParser;
import com.adefreitas.androidframework.AndroidCommManager;
import com.adefreitas.androidframework.AndroidGroupContextManager;
import com.adefreitas.androidframework.ContextReceiver;
import com.adefreitas.androidframework.GCFService;
import com.adefreitas.androidframework.toolkit.CloudStorageToolkit;
import com.adefreitas.androidframework.toolkit.HttpToolkit;
import com.adefreitas.androidframework.toolkit.SftpToolkit;
import com.adefreitas.androidproviders.ActivityContextProvider;
import com.adefreitas.androidproviders.AudioContextProvider;
import com.adefreitas.androidproviders.BluetoothContextProvider;
import com.adefreitas.androidproviders.BluewaveContextProvider;
import com.adefreitas.androidproviders.CompassContextProvider;
import com.adefreitas.androidproviders.GPSContextProvider;
import com.adefreitas.androidproviders.GoogleCalendarProvider;
import com.adefreitas.androidproviders.LocationContextProvider;
import com.adefreitas.gcfimpromptu.lists.AppCategoryInfo;
import com.adefreitas.gcfimpromptu.lists.AppInfo;
import com.adefreitas.gcfmagicapp.R;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.hashlibrary.SHA1;
import com.adefreitas.liveos.ApplicationFunction;
import com.adefreitas.liveos.ApplicationSettings;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;
import com.adefreitas.messages.ContextData;
import com.adefreitas.messages.ContextRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class GCFApplication extends Application
{	
	// Application Constants
	public static final String LOG_NAME          	  = "IMPROMPTU"; 
	public static final String APP_PROCESS_ID		  = "" + new Date().getTime();										// DEBUG		 
	public static final String PREFERENCES_NAME       = "com.adefreit.impromptu.preferences";
	public static final String PREFERENCE_APP_CATALOG = "appCatalog";
	public static final String PREFERENCE_APP_PREF    = "appPreferences";
	public static final int    UPDATE_SECONDS    	  = 60;
	public static final String ACTION_APP_UPDATE 	  = "APP_UPDATE";
	public static final String ACTION_IMAGE_UPLOADED  = "STI_IMAGE_UPLOADED";
	public static final String ACTION_APP_SELECTED    = "APP_SELECTED";
	public static final String ACTION_QUICKBOOT		  = "android.intent.action.QUICKBOOT_POWERON";
	public static final String ACTION_LOGO_DOWNLOADED = "android.intent.action.LOGO_DOWNLOADED";
	public static final String EXTRA_APP_ID           = "APP_ID";
	public static final String LOGO_DOWNLOAD_FOLDER   = "/Download/Impromptu/Logos/";	
	public static final String DOWNLOAD_FOLDER        = "/Download/Impromptu/";							 			      // Path on the Phone where Files are Downloaded
	public static final String UPLOAD_SFTP_PATH       = "/var/www/html/gcf/universalremote/magic/";		 			      // Folder Path on the Cloud Server
	public static final String UPLOAD_WEB_PATH   	  = "http://" + Settings.DEV_SFTP_IP + "/gcf/universalremote/magic/"; // Web Path to the Path Above
	
	// Context Constants
	public static final String IDENTITY_CONTEXT_TAG  = "identity";
	public static final String LOCATION_CONTEXT_TAG  = "location";
	public static final String TIME_CONTEXT_TAG	     = "time";
	public static final String ACTIVITY_CONTEXT_TAG  = "activity";
	public static final String SNAP_TO_IT_TAG    	 = "snap-to-it";
	public static final String PREFERENCES_TAG		 = "preferences";
	
	// GCF Constants (Connection to App Server)
	public static final CommMode COMM_MODE  = CommMode.MQTT;
	public static final String   IP_ADDRESS = Settings.DEV_MQTT_IP;
	public static final int      PORT 	    = Settings.DEV_MQTT_PORT;
	
	// GCF Variables
	public  static String 	  connectionKey;
	private GCFService 		  gcfService;
	private ServiceConnection gcfServiceConnection = new ServiceConnection() 
	{
	    public void onServiceConnected(ComponentName name, IBinder service) 
	    {
	        GCFService.GCFServiceBinder mLocalBinder = (GCFService.GCFServiceBinder)service;
	        gcfService 								 = mLocalBinder.getService();
	    }
		
		public void onServiceDisconnected(ComponentName name) 
		{
	        gcfService = null;
	    }
	};
	
	// Object Serialization
	private Gson 			  gson;
	
	// App Storage
	private SharedPreferences appSharedPreferences;
	
	// Cloud Storage Settings
	private CloudStorageToolkit cloudToolkit;
	private HttpToolkit         httpToolkit = new HttpToolkit(this);
	
	// Intent Filters
	private ContextReceiver contextReceiver;
	private IntentFilter    filter;
	private IntentReceiver  intentReceiver;
	
	// Application Specific Tools
	private boolean					   inForeground;
	private HashMap<String, String>    appPreferences;
	private ArrayList<AppCategoryInfo> appCatalog;
	private ArrayList<AppInfo>		   activeApps;
		
	// Timer
	private ContextAutoDeliveryHandler timerHandler;
	
	// Snap To It Variables
	private Date lastSnapToItDeviceContact;
	private Date lastPhotoTaken;
	private Date lastAutoLaunch;
	
	/**
	 * One-Time Application Initialization Method
	 */
	@Override
	public void onCreate() 
	{
		super.onCreate();
		
		// Initializes Timestamp
		this.lastSnapToItDeviceContact = new Date(0);
		this.lastAutoLaunch    		   = new Date(0);
		this.lastPhotoTaken     	   = new Date(0);
		this.inForeground 			   = false;
		
		// Creates GSON
		this.gson = new Gson();
				
		// Creates Service (if Not Already Created)
		startGCFService();

		// Application Preferences
		appSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		// Creates the Cloud Toolkit Helper
		cloudToolkit = new SftpToolkit(this);
		
		// Initializes App Data Structures
		this.appPreferences  = new HashMap<String, String>();
		this.appCatalog 	 = new ArrayList<AppCategoryInfo>();
		this.activeApps      = new ArrayList<AppInfo>();
		
		// Create Intent Filter and Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(GCFService.ACTION_GCF_STARTED);
		this.filter.addAction(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED);
		this.filter.addAction(AndroidGroupContextManager.ACTION_GCF_OUTPUT);
		this.filter.addAction(BluewaveManager.ACTION_USER_CONTEXT_UPDATED);
		this.filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
		this.filter.addAction(BluewaveManager.ACTION_COMPUTE_INSTRUCTION_RECEIVED);
		this.filter.addAction(AndroidCommManager.ACTION_CHANNEL_SUBSCRIBED);
		this.filter.addAction(ACTION_IMAGE_UPLOADED);
		this.filter.addAction(ACTION_LOGO_DOWNLOADED);
		this.filter.addAction(Intent.ACTION_BOOT_COMPLETED);
		this.filter.addAction(ACTION_QUICKBOOT);
		this.registerReceiver(intentReceiver, filter);
		
		// Performs an Initial Context Update
		setPersonalContext(null);
		
		// Loads App Directory from Memory
		if (appSharedPreferences.contains(PREFERENCE_APP_CATALOG))
		{
			String json = appSharedPreferences.getString(PREFERENCE_APP_CATALOG, "");
			
			if (json.length() > 0)
			{
				// Attempts to Extract the App Catalog from JSON
				try
				{
					appCatalog = (ArrayList<AppCategoryInfo>) gson.fromJson(json, new TypeToken<ArrayList<AppCategoryInfo>>(){}.getType());
				}
				catch (Exception ex)
				{
					Toast.makeText(this, "App Catalog Corruped.  Using Blank.", Toast.LENGTH_SHORT).show();
				}
				
				this.updateCatalog();
			}
		}
		else
		{
			Log.d(LOG_NAME, "Creating New App Catalog");
		}
		
		// Loads App Preferences from Memory
		if (appSharedPreferences.contains(PREFERENCE_APP_PREF))
		{
			String json = appSharedPreferences.getString(PREFERENCE_APP_PREF, "");
			
			if (json.length() > 0)
			{
				// Attempts to Extract the App Preferences from JSON
				try
				{
					appPreferences = (HashMap<String, String>) gson.fromJson(json, new TypeToken<HashMap<String, String>>(){}.getType());
				}
				catch (Exception ex)
				{
					Toast.makeText(this, "App Preferences Corruped.  Using Blank.", Toast.LENGTH_SHORT).show();
				}
				
				this.updateCatalog();
			}
		}
		else
		{
			Log.d(LOG_NAME, "Creating New App Preferences");
		}
	}
	
	/**
	 * Creates the GCF Service if it does not already exist
	 */
	private void startGCFService()
	{
		if (gcfService == null)
		{
			Log.d(LOG_NAME, "Starting GCF Service");
			
			// Creates Service for GCF
			Intent i = new Intent(this, GCFService.class);
			i.putExtra("name", "Impromptu");
			this.bindService(i, gcfServiceConnection, BIND_AUTO_CREATE);
			this.startService(i);
		}
	}
	
	/**
	 * Returns the GCF Service
	 * @return
	 */
	public GCFService getGCFService()
	{
		return gcfService;
	}
	
	/**
	 * Returns the Group Contest Manager
	 * @return
	 */
	public AndroidGroupContextManager getGroupContextManager()
	{
		return gcfService.getGroupContextManager();
	}
	
	/**
	 * Returns the Bluewave Manager
	 * @return
	 */
	public BluewaveManager getBluewaveManager()
	{
		return gcfService.getGroupContextManager().getBluewaveManager();
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
	 * Retrieves the Current HTTP Toolkit
	 * @return
	 */
	public HttpToolkit getHttpToolkit()
	{
		return httpToolkit;
	}
	
	/**
	 * Creates a Toast Notification
	 * @param title
	 * @param subtitle
	 */
	public void createNotification(ArrayList<AppInfo> apps)
	{
		if (!isInForeground())
		{
			String title    = "";
			String subtitle = "";
			Intent intent;
			
			// Creates the Intent
			if (apps.size() == 1)
			{
				AppInfo app = apps.get(0);
				title       = "New App: " + app.getName();
				subtitle    = app.getDescription();
				
				intent = new Intent(this.getApplicationContext(), MainActivity.class);
				intent.putExtra("APP_ID", app.getID());
			}
			else
			{
				title = apps.size() + " New Apps Available";
				
				for (AppInfo app : apps)
				{
					subtitle += app.getName() + ", ";
				}
				
				// Removes the Last Comma
				subtitle = subtitle.substring(0, subtitle.length()-2);
				
				intent = new Intent(this.getApplicationContext(), MainActivity.class);
			}
			
			// Generates the Notification
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) 
			{
				 Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				
				 Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
				 
				 PendingIntent 		 pendingIntent 		 = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
				 NotificationManager notificationManager = (NotificationManager)this.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
				 notificationManager.cancelAll();
				 
				 Notification note = new Notification.Builder(this)
				 	.setLargeIcon(bm)
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
				Toast.makeText(this, title + ": " + subtitle, Toast.LENGTH_SHORT).show();
			}	
		}
	}
	
	/**
	 * Sets a Flag to Let the Application Know if it is in the Foreground
	 * @param value
	 */
	public void setInForeground(boolean value)
	{
		inForeground = value;
		//Toast.makeText(this, "inForeground = " + inForeground, Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Returns TRUE if the application is in the foreground, and false otherwise
	 * @return
	 */
	public boolean isInForeground()
	{
		return inForeground;
	}
		
	/**
	 * Removes All Extraneous Information
	 */
	public void clearCache()
	{
		appCatalog.clear();
		this.saveCatalog();
		
		appPreferences.clear();
		this.savePreferences();
		
		File logoFolder = new File(Environment.getExternalStorageDirectory() + LOGO_DOWNLOAD_FOLDER);
		for (File file : logoFolder.listFiles())
		{
			file.delete();
		}
		
		Toast.makeText(this, "Cache Cleared", Toast.LENGTH_SHORT).show();
		
		// Notifies the Application that the App List has Changed (or been erased)
		Intent i = new Intent(ACTION_APP_UPDATE);
		sendBroadcast(i);
	}
	
	/**
	 * Performs Any Tasks Needed to Halt the Application
	 */
	public void halt()
	{
		if (timerHandler != null)
		{
			timerHandler.stop();
		}
	}
	
	// Context Update Methods -------------------------------------------------------------
	private void setPersonalContext(String photoFilePath)
	{		
		try
		{			
			if (gcfService != null)
			{
				// Updates Via Bluewave
				getBluewaveManager().getPersonalContextProvider().setContext(IDENTITY_CONTEXT_TAG, getIdentityContext());
				getBluewaveManager().getPersonalContextProvider().setContext(LOCATION_CONTEXT_TAG, getLocationContext());
				getBluewaveManager().getPersonalContextProvider().setContext(TIME_CONTEXT_TAG, getTimeContext());
				getBluewaveManager().getPersonalContextProvider().setContext(ACTIVITY_CONTEXT_TAG, getActivityContext());
				getBluewaveManager().getPersonalContextProvider().setContext(SNAP_TO_IT_TAG, getSnapToItContext(photoFilePath));
				getBluewaveManager().getPersonalContextProvider().setContext(PREFERENCES_TAG, getPreferences());
				
				// PUBLISHES THE CHANGES AT ONCE
				getBluewaveManager().getPersonalContextProvider().publish();	
			}
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
			LocationContextProvider locationProvider = (LocationContextProvider)gcfService.getGroupContextManager().getContextProvider("LOC");

	        if (locationProvider != null && locationProvider.hasLocation()) 
	        {
	        	location.put("LATITUDE", locationProvider.getLatitude());
	            location.put("LONGITUDE", locationProvider.getLongitude());
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
		
		if (appSharedPreferences.getBoolean("BLUEWAVE_activity", true))
		{
			try
			{
				ActivityContextProvider acp = (ActivityContextProvider)gcfService.getGroupContextManager().getContextProvider("ACT");
				
				if (acp != null)
				{
					activity.put("type", acp.getActivity());
					activity.put("confidence", acp.getConfidence());
				}
			}
			catch (Exception ex)
			{
				Toast.makeText(this, "Could not write activity context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
			}	
		}
		
		return activity;
	}
	
	private JSONObject getIdentityContext()
	{
		JSONObject identity = new JSONObject();
		
		if (appSharedPreferences.getBoolean("BLUEWAVE_identity", true))
		{
			try
			{
				// Communications Settings
				identity.put("COMM_MODE", COMM_MODE);
				identity.put("IP_ADDRESS", IP_ADDRESS);
				identity.put("PORT", PORT);
				
				// Already Accessible Applications
				JSONArray availableApps = new JSONArray();
				
				if (appCatalog != null)
				{
					for (AppInfo app : getAvailableApps())
					{
						if (app.getTimeToExpire() > 0)
						{
							JSONObject appObject = new JSONObject();
							appObject.put("name", app.getAppContextType());
							//appObject.put("expires", app.getDateExpires().getTime());
							
							long timeToExpire = app.getDateExpires().getTime() - System.currentTimeMillis();
							appObject.put("expiring", timeToExpire < (UPDATE_SECONDS * 1000));
							availableApps.put(appObject);
						}
					}
					
					identity.put("APPS", availableApps);
				}
				
				// Accounts!
				JSONArray emailHashes  = new JSONArray();
				JSONArray emailDomains = new JSONArray();
				
				for (String email : getUniqueEmail())
				{
					emailHashes.put(SHA1.getHash(email.toLowerCase()));
					emailDomains.put(email.split("@")[1]);
				}
				
				// Adds Domains and Hashes
				identity.put("emailDomains", emailDomains);
				identity.put("email", emailHashes);
				
			}
			catch (Exception ex)
			{
				Toast.makeText(this, "Could not write identity context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
				ex.printStackTrace();
			}	
		}
		
		return identity;
	}
	
	private JSONObject getTimeContext()
	{
		JSONObject time = new JSONObject();
		
		if (appSharedPreferences.getBoolean("BLUEWAVE_time", true))
		{
			try
			{
				//time.put("SYSTEM_CLOCK", new Date().getTime());
				time.put("TIMEZONE", Calendar.getInstance().getTimeZone().getDisplayName(Locale.getDefault()));
			}
			catch (Exception ex)
			{
				Toast.makeText(this, "Could not write activity context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
			}	
		}
		
		return time;
	}
	
	private JSONObject getSnapToItContext(String photoFilePath)
	{
		JSONObject snaptoit = new JSONObject();
		
		if (appSharedPreferences.getBoolean("BLUEWAVE_snap-to-it", true))
		{
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
		        	getBluewaveManager().getPersonalContextProvider().removeContext(SNAP_TO_IT_TAG);
		        }
			}
			catch (Exception ex)
			{
				Toast.makeText(this, "Could not write snap-to-it context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
			}	
		}
		
		return snaptoit;
	}
	
	private JSONObject getPreferences()
	{
		JSONObject preferences = new JSONObject();
		
		if (appSharedPreferences.getBoolean("BLUEWAVE_preferences", true))
		{
			try
			{
				for (String key : appPreferences.keySet())
				{
					preferences.put(key, appPreferences.get(key));
				}
			}
			catch (Exception ex)
			{
				Toast.makeText(this, "Could not write preferences context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
		
		return preferences;
	}
	
	private ArrayList<String> getUniqueEmail()
	{
		ArrayList<String> result = new ArrayList<String>();
		
		// Gets All Accounts on this Device
		Account[] accounts = AccountManager.get(this).getAccounts();
        for (Account account : accounts) 
        {
        	if (!result.contains(account.name) && account.name.contains("@"))
        	{
        		result.add(account.name);
        	}
        }
        
        return result;
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
		
	// Preference Methods (Application and app) -------------------------------------------------
	public void setPreference(String name, String value)
	{
		// Stores the Context
		this.appPreferences.put(name, value);
		
		savePreferences();
		
		// Allows the Device's Personal Context
		setPersonalContext(null);
	}
	
	public String getPreference(String name)
	{
		return this.appPreferences.get(name);
	}
		
	public SharedPreferences getAppSharedPreferences()
	{
		return appSharedPreferences;
	}
	
	private void savePreferences()
	{
		// Saves this in Memory
		SharedPreferences.Editor editor = appSharedPreferences.edit();
		editor.putString(PREFERENCE_APP_PREF, gson.toJson(appPreferences));
		editor.commit();
	}
	
	// TODO:  TEMPORARY VALUES NEEDED TO TEST screen rotation issues with app engine
    public ValueCallback<Uri[]> mFilePathCallback;
    public ValueCallback<Uri>   mUploadMessage;
    public Uri      			imageUri;
    public String 				mCameraPhotoPath;
	
	// Catalog Methods --------------------------------------------------------------------------
	public boolean addApplicationToCatalog(AppInfo newApp)
	{	
		boolean foundCategory = false;
		boolean isNew		  = false;
		
		// Tries to Add an App to an Existing Category
		for (AppCategoryInfo category : appCatalog)
		{
			// Looks for the Category
			if (category.getName().equalsIgnoreCase(newApp.getCategory()))
			{
				// Sets a Flag to Let us Know that the Category Already Exists
				foundCategory = true;
				
				if (category.hasApp(newApp.getID()))
				{
					// Updates the Existing App Entry
					category.updateApp(newApp);
				}
				else
				{
					// Creates a New App Entry from Scratch!
					category.addApp(newApp);
					isNew = true;
				}

				// Notifies the Application that the App List has Changed (or been erased)
				Intent i = new Intent(ACTION_APP_UPDATE);
				sendBroadcast(i);
				
				break;
			}
		}
		
		// Creates the Category if it does not yet exist
		if (!foundCategory)
		{
			AppCategoryInfo newCategory = new AppCategoryInfo(newApp.getCategory());
			newCategory.addApp(newApp);
			appCatalog.add(newCategory);
			isNew = true;
			
			// Notifies the Application that the App List has Changed (or been erased)
			Intent i = new Intent(ACTION_APP_UPDATE);
			sendBroadcast(i);
		}
		
		return isNew;
	}
	
	public void removeApplicationFromCatalog(AppInfo appToRemove)
	{
		for (AppCategoryInfo category : appCatalog)
		{
			if (category.getName().equalsIgnoreCase(appToRemove.getCategory()))
			{
				if (category.hasApp(appToRemove.getID()))
				{
					category.removeApp(appToRemove);
					
					// Notifies the Application that the App List has Changed (or been erased)
					Intent i = new Intent(ACTION_APP_UPDATE);
					sendBroadcast(i);
				}
				
				break;
			}
		}
		
		saveCatalog();
	}
	
	public AppInfo getApplicationFromCatalog(String appID)
	{
		for (AppCategoryInfo category : appCatalog)
		{
			for (AppInfo app : category.getApps())
			{
				if (app.getID().equalsIgnoreCase(appID))
				{
					return app;
				}
			}
		}
		
		return null;
	}
	
	public ArrayList<AppInfo> getAvailableApps()
	{
		ArrayList<AppInfo> catalog = new ArrayList<AppInfo>();
		
		for (AppCategoryInfo category : appCatalog)
		{
			for (AppInfo app : category.getApps())
			{
				if (!app.isExpired())
				{
					catalog.add(app);
				}
			}
		}
		
		return catalog;
	}
	
	public ArrayList<AppCategoryInfo> getCatalog()
	{
		return appCatalog;
	}
					
	private void updateCatalog()
	{
		boolean changed = false;
		
		// Updates App Catalog Contents
		// Looking for Canceled Application
		for (AppCategoryInfo category : new ArrayList<AppCategoryInfo>(appCatalog))
		{
			for (AppInfo app : new ArrayList<AppInfo>(category.getApps()))
			{
				long timeElapsedInMS = new Date().getTime() - app.getDateExpires().getTime();
				
				if (timeElapsedInMS > 60000 * 60)
				{
					category.removeApp(app);
					changed = true;
				}
			}
			
			if (!category.containsAvailableApps())
			{
				//appCatalog.remove(category);
				//appCatalog.add(category);
				changed = true;
			}
		}
				
		// Notifies Activities if Something Changed
		if (changed)
		{
			// Notifies the Application that the App List has Changed
			Intent i = new Intent(ACTION_APP_UPDATE);
			sendBroadcast(i);
			
			saveCatalog();
		}
	}
	
	private void saveCatalog()
	{
		Log.d(LOG_NAME, "Saving App Catalog (" + appCatalog.size() + " categories)");
		SharedPreferences.Editor editor = appSharedPreferences.edit();
		editor.putString(PREFERENCE_APP_CATALOG, gson.toJson(appCatalog));
		editor.commit();
	}
	
	private void getLogo(String logoURL)
	{
		String filename = logoURL.substring(logoURL.lastIndexOf("/")+1);
		File   logo     = new File(Environment.getExternalStorageDirectory() + LOGO_DOWNLOAD_FOLDER + filename);
		
		if (!logo.exists())
		{
			Log.d(LOG_NAME, "Downloading logo " + filename);
			String downloadFolder = Environment.getExternalStorageDirectory() + LOGO_DOWNLOAD_FOLDER;
			
			// Starts an Async Process to Download the Logos
			httpToolkit.download(logoURL, downloadFolder, ACTION_LOGO_DOWNLOADED);
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
	public class IntentReceiver extends BroadcastReceiver
	{		
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(GCFService.ACTION_GCF_STARTED))
			{
				onGCFServiceStarted(context, intent);
			}
			else if (intent.getAction().equals(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED))
			{
				onContextDataReceived(context, intent);
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
			else if (intent.getAction().equals(ACTION_LOGO_DOWNLOADED))
			{
				onLogoDownloaded(context, intent);
			}
			else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
			{
				onBootup(context, intent);
			}
			else if (intent.getAction().equals(ACTION_QUICKBOOT))
			{
				onBootup(context, intent);
			}
			else
			{
				Log.e("", "Unknown Action: " + intent.getAction());
			}
		}
	
		private void onGCFServiceStarted(Context context, Intent intent)
		{
			if (gcfService != null && gcfService.isReady())
			{	
				// Connects to Default DNS Channel and Channels
				connectionKey = gcfService.getGroupContextManager().connect(COMM_MODE, IP_ADDRESS, PORT);
				
				// Creates Context Providers
				ContextProvider 		 calendarProvider  = new GoogleCalendarProvider(gcfService.getGroupContextManager(), GCFApplication.this.getContentResolver());
				//ContextProvider 		 lightProvider     = new LightContextProvider(GCFApplication.this, gcfService.getGroupContextManager());
				ActivityContextProvider  acp 			   = new ActivityContextProvider(GCFApplication.this, gcfService.getGroupContextManager());
				AudioContextProvider     audioProvider     = new AudioContextProvider(gcfService.getGroupContextManager());
				BluewaveContextProvider  bluewaveProvider  = new BluewaveContextProvider(GCFApplication.this, gcfService.getGroupContextManager(), 60000);
				BluetoothContextProvider bluetoothProvider = new BluetoothContextProvider(GCFApplication.this, gcfService.getGroupContextManager(), 60000);
				LocationContextProvider  locationProvider  = new LocationContextProvider(GCFApplication.this, gcfService.getGroupContextManager());
				GPSContextProvider		 gpsProvider	   = new GPSContextProvider(GCFApplication.this, gcfService.getGroupContextManager());
				CompassContextProvider   compassProvider   = new CompassContextProvider(GCFApplication.this, gcfService.getGroupContextManager());
				
				// Registers Context Providers
				gcfService.getGroupContextManager().registerContextProvider(calendarProvider);
				//gcfService.getGroupContextManager().registerContextProvider(lightProvider);	
				gcfService.getGroupContextManager().registerContextProvider(acp);
				gcfService.getGroupContextManager().registerContextProvider(audioProvider);
				//gcfService.getGroupContextManager().registerContextProvider(bluewaveProvider);
				gcfService.getGroupContextManager().registerContextProvider(bluetoothProvider);
				gcfService.getGroupContextManager().registerContextProvider(locationProvider);
				gcfService.getGroupContextManager().registerContextProvider(gpsProvider);
				gcfService.getGroupContextManager().registerContextProvider(compassProvider);
				
				// Requests Context Information from Self
				gcfService.getGroupContextManager().sendRequest("LOC", ContextRequest.LOCAL_ONLY, UPDATE_SECONDS * 1000, new String[0]);
				gcfService.getGroupContextManager().sendRequest("ACT", ContextRequest.LOCAL_ONLY, UPDATE_SECONDS * 1000, new String[0]);
				//gcfService.getGroupContextManager().sendRequest("COMPASS", ContextRequest.LOCAL_ONLY, 250, new String[0]);
				
				if (!isInForeground())
				{
					Toast.makeText(GCFApplication.this, "GCF Ready [" + gcfService.getGroupContextManager().getRegisteredProviders().length + " context providers]", Toast.LENGTH_SHORT).show();	
				}
			}
		}
		
		private void onContextDataReceived(Context context, Intent intent)
		{
			// Extracts the values from the intent
			String   contextType = intent.getStringExtra(ContextData.CONTEXT_TYPE);
			String   deviceID    = intent.getStringExtra(ContextData.DEVICE_ID);
			String[] values      = intent.getStringArrayExtra(ContextData.PAYLOAD);
						
			Log.d(LOG_NAME, "Received [" + contextType + "]: " + Arrays.toString(values));
			
			// Forwards Values to the ContextReceiver for Processing
			if (contextReceiver != null)
			{
				contextReceiver.onContextData(new ContextData(contextType, deviceID, values));
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
			String   contextType = intent.getExtras().getString(ComputeInstruction.COMPUTE_CONTEXT_TYPE);
			String   command     = intent.getExtras().getString(ComputeInstruction.COMPUTE_COMMAND);
			String   sender      = intent.getExtras().getString(ComputeInstruction.COMPUTE_SENDER);
			String[] payload     = intent.getExtras().getStringArray(ComputeInstruction.COMPUTE_PARAMETERS);
			
			ComputeInstruction instruction = new ComputeInstruction(contextType, sender, new String[0], command, payload);
			
			// Registers an Application in the Catalog
			if (command.equalsIgnoreCase("APPLICATION"))
			{
				String[] appsJson          = gson.fromJson(instruction.getPayload("APPS"), new TypeToken<String[]>() {}.getType());
				ArrayList<AppInfo> newApps = new ArrayList<AppInfo>();
									
				for (String s : appsJson)
				{
					String[] appPayload = gson.fromJson(s, new TypeToken<String[]>() {}.getType());
					
					// Generates a Fake Instruction
					ComputeInstruction appInstruction = new ComputeInstruction(contextType, sender, new String[0], command, appPayload);
				
					try
					{
						String 			  appID       	 = appInstruction.getPayload("APP_ID");
						String			  appContextType = appInstruction.getPayload("APP_CONTEXT_TYPE");
						String 			  appName     	 = appInstruction.getPayload("NAME");
						String			  deviceID		 = appInstruction.getPayload("DEVICE_ID");
						String 			  description    = appInstruction.getPayload("DESCRIPTION");
						String 			  category	     = appInstruction.getPayload("CATEGORY");
						String 			  logo		  	 = appInstruction.getPayload("LOGO");
						int			      lifetime		 = Integer.parseInt(appInstruction.getPayload("LIFETIME"));
						ArrayList<String> contexts    	 = CommMessage.getValues(appInstruction.getPayload(), "CONTEXTS");
						ArrayList<String> preferences 	 = CommMessage.getValues(appInstruction.getPayload(), "PREFERENCES");
						CommMode		  commMode		 = CommMode.valueOf(appInstruction.getPayload("COMM_MODE"));
						String 			  ipAddress   	 = appInstruction.getPayload("APP_ADDRESS");
						int				  port		  	 = Integer.valueOf(appInstruction.getPayload("APP_PORT"));
						String 			  channel    	 = appInstruction.getPayload("APP_CHANNEL");
						Double			  photoMatches   = (appInstruction.getPayload("PHOTO_MATCHES") != null) ? Double.valueOf(appInstruction.getPayload("PHOTO_MATCHES")) : 0.0;
						
						// Starts Downloading Logo
						if (logo != null && logo.length() > 0)
						{
							getLogo(logo);
						}
						
						// Creates Individual Function Objects
						ArrayList<ApplicationFunction> functions    = new ArrayList<ApplicationFunction>();
						String 						   functionJSON = appInstruction.getPayload("FUNCTIONS");
						
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
						boolean isNew = GCFApplication.this.addApplicationToCatalog(app);
						
						if (isNew)
						{
							newApps.add(app);
						}
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
						Toast.makeText(GCFApplication.this, "Problem With App: " + ex.getMessage(), Toast.LENGTH_LONG).show();
					}
				}
				
				// Stores the Catalog in Long Term Memory
				saveCatalog();
				
				if (newApps.size() >= 1)
				{
					createNotification(newApps);
					Log.d(LOG_NAME, "Received " + appsJson.length + " apps (" + newApps.size() + " new)");
				}
			}
		}
	
		private void onChannelSubscribed(Context context, Intent intent)
		{
			//String channel = intent.getStringExtra("CHANNEL");
			
			// Creates the Scheduled Event Timer
			if (timerHandler == null)
			{
				timerHandler = new ContextAutoDeliveryHandler(GCFApplication.this);				
				timerHandler.start();	
			}
		}
	
		private void onSnapToItUploadComplete(Context context, Intent intent)
		{
			String response = intent.getStringExtra(HttpToolkit.HTTP_RESPONSE);
			Toast.makeText(GCFApplication.this, "Uploaded Complete: " + response, Toast.LENGTH_SHORT).show();
			
//		    // Makes the File Visible!
//		    MediaScannerConnection.scanFile(GCFApplication.this, new String[] { uploadPath }, null, null);
//			
//			// Updates Context with the New Upload Path
//			setPersonalContext(uploadPath);		
//			
//			// Sends a New Query!
//			sendQuery(GCFApplication.this, true);
		}
	
		private void onLogoDownloaded(Context context, Intent intent)
		{
			String url      = intent.getStringExtra(HttpToolkit.HTTP_URL);
			String filename = url.substring(url.lastIndexOf("/")+1);
			
			File logo = new File(Environment.getExternalStorageDirectory() + LOGO_DOWNLOAD_FOLDER + filename);
			
			if (logo.exists())
			{
				//Toast.makeText(GCFApplication.this, "Downloaded: " + filename, Toast.LENGTH_SHORT).show();
				
				// Notifies the Application that the App List has Changed (or been erased)
				Intent i = new Intent(ACTION_APP_UPDATE);
				GCFApplication.this.sendBroadcast(i);
			}
			else
			{
				Toast.makeText(GCFApplication.this, "ERROR Downloading: " + filename, Toast.LENGTH_SHORT).show();
			}
		}
		
		private void onBootup(Context context, Intent intent)
		{
			//startGCFService();
		}
	}
	
	public static class BootupReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			
		}
	}
	
	public static void sendQuery(GCFApplication application, boolean force)
	{
		boolean sendUpdate = application.appSharedPreferences.getBoolean("impromptu_send", true) || application.getGroupContextManager().getBatteryMonitor().isCharging();
		//Toast.makeText(application, "Send Update = " + sendUpdate, Toast.LENGTH_SHORT).show();
		
		if (application.getGCFService() != null)
		{
			JSONContextParser context = application.getGCFService().getGroupContextManager().getBluewaveManager().getPersonalContextProvider().getContext(); 
			
			if (context != null)
			{
				Log.d(LOG_NAME, "Sending Context to DNS: " + context.toString().length() + " bytes");
				
				// Updates the Application Catalog to Remove Expired Entries
				application.updateCatalog();
				
				// Sends a Context Update to the System
				if (sendUpdate || force)
				{
					// Updates the Device's Personal Context
					application.setPersonalContext(null);
					
					// Sends the Context to the Application Directory
					application.getGCFService().getGroupContextManager().sendComputeInstruction(connectionKey, 
							ApplicationSettings.DNS_CHANNEL, 
							"LOS_DNS", 
							new String[] { "LOS_DNS" }, 
							"QUERY", 
							new String[] { "CONTEXT=" + context.toString(), "TIMESTAMP=" + new Date().toString(), "PID=" + APP_PROCESS_ID });
					
					if (force)
					{
						Toast.makeText(application, "Sending Context", Toast.LENGTH_SHORT).show();
					}
				}
			}
			else
			{
				Toast.makeText(application, "No Context Generated.  Stand By.", Toast.LENGTH_SHORT).show();
			}
		}
		else
		{
			Log.d(LOG_NAME, "Cannot Send Context to DNS: GCF Service is NULL");
		}
	}
	
	// Timed Event ------------------------------------------------------------------------------
	/**
	 * This Class Allows the App To Update Its Context Once per Interval
	 * @author adefreit
	 */
	private static class ContextAutoDeliveryHandler extends Handler
	{
		private boolean 		     running;
		private final GCFApplication app;
		
		public ContextAutoDeliveryHandler(final GCFApplication app)
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
					Date nextExecute = (app.isInForeground()) ? 
							new Date(System.currentTimeMillis() + UPDATE_SECONDS * 1000) :
							new Date(System.currentTimeMillis() + 2 * UPDATE_SECONDS * 1000);
					
					// Sends the Context to the Application Directory
					sendQuery(app, false);
					
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
