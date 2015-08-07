package com.adefreitas.gcfimpromptu;

import java.io.File;
import java.util.ArrayList;
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
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.ValueCallback;
import android.widget.Toast;

import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.Settings;
import com.adefreitas.gcf.android.AndroidCommManager;
import com.adefreitas.gcf.android.AndroidGroupContextManager;
import com.adefreitas.gcf.android.ContextReceiver;
import com.adefreitas.gcf.android.GCFService;
import com.adefreitas.gcf.android.bluewave.BluewaveManager;
import com.adefreitas.gcf.android.bluewave.JSONContextParser;
import com.adefreitas.gcf.android.providers.ActivityContextProvider;
import com.adefreitas.gcf.android.providers.AudioContextProvider;
import com.adefreitas.gcf.android.providers.BluetoothContextProvider;
import com.adefreitas.gcf.android.providers.BluewaveContextProvider;
import com.adefreitas.gcf.android.providers.CompassContextProvider;
import com.adefreitas.gcf.android.providers.GPSContextProvider;
import com.adefreitas.gcf.android.providers.GoogleCalendarProvider;
import com.adefreitas.gcf.android.providers.LocationContextProvider;
import com.adefreitas.gcf.android.providers.PostureContextProvider;
import com.adefreitas.gcf.android.providers.TemperatureContextProvider;
import com.adefreitas.gcf.android.toolkit.CloudStorageToolkit;
import com.adefreitas.gcf.android.toolkit.HttpToolkit;
import com.adefreitas.gcf.android.toolkit.SftpToolkit;
import com.adefreitas.gcf.impromptu.ApplicationFunction;
import com.adefreitas.gcf.impromptu.ApplicationSettings;
import com.adefreitas.gcf.messages.CommMessage;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.adefreitas.gcf.messages.ContextData;
import com.adefreitas.gcf.messages.ContextRequest;
import com.adefreitas.gcf.toolkit.SHA1;
import com.adefreitas.gcfimpromptu.lists.AppCategoryInfo;
import com.adefreitas.gcfimpromptu.lists.AppInfo;
import com.adefreitas.gcfmagicapp.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class GCFApplication extends Application
{	
	// Debug Flag (Set to False Before Publishing!)
	public static final boolean DEBUG		   = true;
	public static final String  APP_PROCESS_ID = "" + new Date().getTime();	
	public static final String  LOG_NAME       = "IMPROMPTU";

	// Application Constants 	 
	public static final int    UPDATE_SECONDS    	  = 60;
	public static final int    APP_EXPIRATION_TIME    = 60 & 5;
	public static final String PREFERENCES_NAME       = "com.adefreit.impromptu.preferences";
	public static final String PREFERENCE_APP_CATALOG = "appCatalog";
	public static final String PREFERENCE_APP_PREF    = "appPreferences";
	public static final String ACTION_APP_UPDATE 	  = "APP_UPDATE";
	public static final String ACTION_IMAGE_UPLOADED  = "STI_IMAGE_UPLOADED";
	public static final String ACTION_APP_SELECTED    = "APP_SELECTED";
	public static final String ACTION_QUICKBOOT		  = "android.intent.action.QUICKBOOT_POWERON";
	public static final String ACTION_LOGO_DOWNLOADED = "android.intent.action.LOGO_DOWNLOADED";
	public static final String EXTRA_APP_ID           = "APP_ID";
	public static final String LOGO_DOWNLOAD_FOLDER   = "/Download/Impromptu/Logos/";	// Path on the Phone where Logos are Downloaded
	public static final String DOWNLOAD_FOLDER        = "/Download/Impromptu/";			// Path on the Phone where Files are Downloaded
	
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
	private Gson gson;
	
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
	private ContextDeliveryHandler timerHandler;
	public static long lastQuery = 0;
	
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
		setPersonalContext();
		
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
			Log.d(LOG_NAME, "Impromptu Starting GCF Service");
			
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
				title       = app.getName();
				subtitle    = app.getDescription();
				
				intent = new Intent(this.getApplicationContext(), MainActivity.class);
				intent.putExtra("APP_ID", app.getID());
			}
			else
			{
				title    = "New Apps are Available";
				subtitle = "Tap to View";
				
				// Removes the Last Comma
				//subtitle = subtitle.substring(0, subtitle.length()-2);
				
				intent = new Intent(this.getApplicationContext(), MainActivity.class);
			}
			
			// Generates the Notification
			createNotification(0, title, subtitle, intent);
		}
	}
	
	public void createNotification(int id, String title, String subtitle, Intent intent)
	{
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
			 
			 notificationManager.notify(id, note);
		}
		else
		{
			Toast.makeText(this, title + ": " + subtitle, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void cancelNotification(int id)
	{
		NotificationManager notificationManager = (NotificationManager)this.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(id);
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
	
	// Service Verification ---------------------------------------------------------------
	public boolean verifyServices()
	{
		// Checking Services
		boolean locationWorking = verifyLocationServices();
		boolean googleWorking   = verifyGoogleServices();
		boolean bluewaveWorking = verifyBluewaveServices();
		
		if (!locationWorking || !googleWorking || !bluewaveWorking)
		{
			Intent newIntent = new Intent(getApplicationContext(), Splashscreen.class);
			createNotification(0, "Impromptu Needs More Context", "Tap to Troubleshoot", newIntent);			
			return false;
		}
		else
		{
			cancelNotification(0);
		}
		
		return true;
	}

	public boolean verifyLocationServices()
	{
		boolean locationEnabled = false;
		
		// LOCATION SERVICES CHECK
		LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		int locationMode = 0;
	    String locationProviders;

	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
	    {
		    try 
		    {
		    	locationMode = android.provider.Settings.Secure.getInt(getContentResolver(), android.provider.Settings.Secure.LOCATION_MODE);
		    } 
		    catch (Exception e) 
		    {
		    	e.printStackTrace();
		    }
	
		    locationEnabled = (locationMode != android.provider.Settings.Secure.LOCATION_MODE_OFF);
	    }
	    else
	    {
	    	// Looks at Location Mode for Jelly Bean and Below Systems
	        locationProviders = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
	        locationEnabled   = !TextUtils.isEmpty(locationProviders);
	    }
	    
	    return locationEnabled;
	}
	
	public boolean verifyGoogleServices()
	{
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		
		try 
		{
		    return (status == ConnectionResult.SUCCESS);
		} 
		catch (Exception e) 
		{
		    Log.e("IMPROMPTU", "" + e);
		    return false;
		}
	}
	
	public boolean verifyBluewaveServices()
	{
		if (getGCFService() == null)
		{
			return false;
		}
		else
		{
			return getGCFService().getBluewaveManager().isDiscoverable();	
		}
	}
	
	// Context Update Methods -------------------------------------------------------------
	private void setPersonalContext()
	{
		setPersonalContext(null, null, 0.0, 0.0, 0.0);
	}
	
	private void setPersonalContext(String snapToItCode)
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
				getBluewaveManager().getPersonalContextProvider().setContext(SNAP_TO_IT_TAG, getSnapToItContext(snapToItCode));
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
	
	private void setPersonalContext(String photoFilePath, String applianceName, double azimuth, double pitch, double roll)
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
				getBluewaveManager().getPersonalContextProvider().setContext(SNAP_TO_IT_TAG, getSnapToItContext(photoFilePath, applianceName, azimuth, pitch, roll));
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
							appObject.put("expiring", timeToExpire < (UPDATE_SECONDS * 1.5 * 1000));
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
	
	private JSONObject getSnapToItContext(String photoFilePath, String applianceName, double azimuth, double pitch, double roll)
	{
		JSONObject snaptoit = new JSONObject();
		
		try
		{
			// Sets Snap To It Value
	        if (photoFilePath != null)
	        {
	        	snaptoit.put("PHOTO", photoFilePath);
	        	snaptoit.put("TIMESTAMP", System.currentTimeMillis());
	        	snaptoit.put("AZIMUTH", azimuth);
	        	snaptoit.put("PITCH", pitch);
	        	snaptoit.put("ROLL", roll);
	        	snaptoit.put("APPLIANCE_NAME", applianceName);
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
		
		return snaptoit;
	}
	
	private JSONObject getSnapToItContext(String code)
	{
		JSONObject snaptoit = new JSONObject();
		
		try
		{
			// Sets Snap To It Value
	        if (code != null)
	        {
	        	Log.d(LOG_NAME, "Snap To It Code: " + code);
	        	snaptoit.put("CODE", code);
	        }
	        else
	        {
	        	Log.d(LOG_NAME, "No Snap To It Code");
	        	getBluewaveManager().getPersonalContextProvider().removeContext(SNAP_TO_IT_TAG);
	        }
		}
		catch (Exception ex)
		{
			Toast.makeText(this, "Could not write snap-to-it context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
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
		setPersonalContext();
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
			
	public void removeSnapToItApps()
	{
		// Removes the App
		for (AppCategoryInfo category : appCatalog)
		{
			if (category.getName().equalsIgnoreCase("Snap-To-It"))
			{
				appCatalog.remove(category);
				Intent i = new Intent(ACTION_APP_UPDATE);
				sendBroadcast(i);
				break;
			}
		}
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
				
				if (timeElapsedInMS > 60000 * APP_EXPIRATION_TIME)
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
		setPersonalContext();
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
				// Sets a Debug Flag
				gcfService.getGroupContextManager().setDebug(DEBUG);
				
				// Connects to Default DNS Channel and Channels
				connectionKey = gcfService.getGroupContextManager().connect(COMM_MODE, IP_ADDRESS, PORT);
				
				// Creates Context Providers
				ContextProvider 		   calendarProvider  = new GoogleCalendarProvider(gcfService.getGroupContextManager(), GCFApplication.this.getContentResolver());
				//ContextProvider 		   lightProvider     = new LightContextProvider(GCFApplication.this, gcfService.getGroupContextManager());
				ActivityContextProvider    acp 			     = new ActivityContextProvider(GCFApplication.this, gcfService.getGroupContextManager());
				AudioContextProvider       audioProvider     = new AudioContextProvider(gcfService.getGroupContextManager());
				BluewaveContextProvider    bluewaveProvider  = new BluewaveContextProvider(GCFApplication.this, gcfService.getGroupContextManager(), 60000);
				BluetoothContextProvider   bluetoothProvider = new BluetoothContextProvider(GCFApplication.this, gcfService.getGroupContextManager(), 60000);
				LocationContextProvider    locationProvider  = new LocationContextProvider(GCFApplication.this, gcfService.getGroupContextManager());
				GPSContextProvider		   gpsProvider	     = new GPSContextProvider(GCFApplication.this, gcfService.getGroupContextManager());
				CompassContextProvider     compassProvider   = new CompassContextProvider(GCFApplication.this, gcfService.getGroupContextManager());
				TemperatureContextProvider tempProvider      = new TemperatureContextProvider(gcfService.getGroupContextManager(), GCFApplication.this);
				PostureContextProvider     postureProvider   = new PostureContextProvider(GCFApplication.this, gcfService.getGroupContextManager());
								
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
				//gcfService.getGroupContextManager().registerContextProvider(tempProvider);
				//gcfService.getGroupContextManager().registerContextProvider(postureProvider);
								
				if (!isInForeground() && DEBUG)
				{
					Toast.makeText(GCFApplication.this, "Impromptu Running", Toast.LENGTH_SHORT).show();	
				}
				
				// Checks to Make Sure all Services are Working
				verifyServices();
			}
		}
		
		private void onContextDataReceived(Context context, Intent intent)
		{
			// Extracts the values from the intent
			String   contextType = intent.getStringExtra(ContextData.CONTEXT_TYPE);
			String   deviceID    = intent.getStringExtra(ContextData.DEVICE_ID);
			String[] values      = intent.getStringArrayExtra(ContextData.PAYLOAD);
						
			//Log.d(LOG_NAME, "Received [" + contextType + "]: " + Arrays.toString(values));
			
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
			String json = intent.getStringExtra(BluewaveManager.EXTRA_OTHER_USER_CONTEXT);
			
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
							
				boolean isSystemAlert = false;
				
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
						AppInfo app   = new AppInfo(appID, appContextType, deviceID, appName, description, category, logo, lifetime, photoMatches, contexts, preferences, functions, commMode, ipAddress, port, channel);					
						boolean isNew = GCFApplication.this.addApplicationToCatalog(app);
						
						// Keeps Track of New Apps
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
				
				// Creates a Notification IFF a New App was Found
				if (newApps.size() >= 1)
				{
					createNotification(newApps);
					Log.d(LOG_NAME, "Received " + appsJson.length + " apps (" + newApps.size() + " new)");
				}
				
				// Stores the Catalog in Long Term Memory
				saveCatalog();
			}
		}
	
		private void onChannelSubscribed(Context context, Intent intent)
		{
			// Creates the Scheduled Event Timer
			if (timerHandler == null)
			{
				timerHandler = new ContextDeliveryHandler(GCFApplication.this);				
				timerHandler.start();	
			}
		}
	
		private void onSnapToItUploadComplete(Context context, Intent intent)
		{
			String uploadPath = intent.getStringExtra("UPLOAD_PATH");
			String applianceName = intent.getStringExtra("APPLIANCE_NAME");
			double azimuth    = intent.getDoubleExtra("AZIMUTH", 0.0);
			double pitch      = intent.getDoubleExtra("PITCH", 0.0);
			double roll       = intent.getDoubleExtra("ROLL", 0.0);
			
			//Toast.makeText(GCFApplication.this, "Uploaded Complete: " + uploadPath, Toast.LENGTH_SHORT).show();
						
			// Sends a New Query!
			sendQuery(GCFApplication.this, uploadPath, applianceName, azimuth, pitch, roll, true);	
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
		
	public static void sendQuery(GCFApplication application, String snapToItPhoto, String applianceName, double azimuth, double pitch, double roll, boolean force)
	{
		// This flag is used to determine if the system should send a message.  It is true when:
		//   1.  The user has elected to send context from the app's settings screen, OR the system is charging
		//   2.  A set amount of time has elapsed
		boolean sendUpdate = application.appSharedPreferences.getBoolean("impromptu_send", true) || 
				             application.getGroupContextManager().getBatteryMonitor().isCharging();
		sendUpdate = sendUpdate && (System.currentTimeMillis() - lastQuery) > (UPDATE_SECONDS * 1000  - 5000);
		
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
					application.setPersonalContext(snapToItPhoto, applianceName, azimuth, pitch, roll);
					
					// Sends the Context to the Application Directory
					application.getGCFService().getGroupContextManager().sendComputeInstruction(connectionKey, 
							ApplicationSettings.DNS_CHANNEL, 
							"LOS_DNS", 
							new String[] { "LOS_DNS" }, 
							"QUERY", 
							new String[] { "CONTEXT=" + context.toString(), "TIMESTAMP=" + new Date().toString(), "PID=" + APP_PROCESS_ID });
				
					// Remembers when the last time a query was sent
					lastQuery = System.currentTimeMillis();
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
	
	public static void sendQuery(GCFApplication application, String snapToItCode)
	{
		boolean sendUpdate = application.appSharedPreferences.getBoolean("impromptu_send", true) || application.getGroupContextManager().getBatteryMonitor().isCharging();
		
		if (application.getGCFService() != null)
		{
			JSONContextParser context = application.getGCFService().getGroupContextManager().getBluewaveManager().getPersonalContextProvider().getContext(); 
			
			if (context != null)
			{
				Log.d(LOG_NAME, "Sending Context to DNS: " + context.toString().length() + " bytes");
				
				// Updates the Application Catalog to Remove Expired Entries
				application.updateCatalog();
				
				// Sends a Context Update to the System
				if (sendUpdate)
				{
					// Updates the Device's Personal Context
					application.setPersonalContext(snapToItCode);
					
					// Sends the Context to the Application Directory
					application.getGCFService().getGroupContextManager().sendComputeInstruction(connectionKey, 
							ApplicationSettings.DNS_CHANNEL, 
							"LOS_DNS", 
							new String[] { "LOS_DNS" }, 
							"QUERY", 
							new String[] { "CONTEXT=" + context.toString(), "TIMESTAMP=" + new Date().toString(), "PID=" + APP_PROCESS_ID });
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
	private static class ContextDeliveryHandler extends Handler
	{
		private boolean 		     running;
		private final GCFApplication app;
		
		public ContextDeliveryHandler(final GCFApplication app)
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
					Date nextExecute = new Date(System.currentTimeMillis() + UPDATE_SECONDS * 1000);
					
					// Looks for Special Cases for Updates
//					if (app.getGroupContextManager().getBatteryMonitor().isCharging())
//					{
//						if (app.getGroupContextManager().isConnectedWiFi())
//						{
//							// If Charging, go to 30 seconds
//							nextExecute = new Date(System.currentTimeMillis() + 30000);	
//						}
//					}
					
					// Sends the Context to the Application Directory
					sendQuery(app, null, null, 0.0, 0.0, 0.0, false);
					
					// Requests Context Information from Self
					if (app.getGroupContextManager().getRequest("LOC") == null || app.getGroupContextManager().getRequest("ACT") == null)
					{
						app.getGroupContextManager().sendRequest("LOC", ContextRequest.LOCAL_ONLY, GCFApplication.UPDATE_SECONDS * 1000, new String[0]);
						app.getGroupContextManager().sendRequest("ACT", ContextRequest.LOCAL_ONLY, GCFApplication.UPDATE_SECONDS * 1000, new String[0]);	
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
