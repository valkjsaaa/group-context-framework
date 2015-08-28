package com.adefreitas.gcftestsuite;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.gcf.android.AndroidBatteryMonitor;
import com.adefreitas.gcf.android.AndroidGroupContextManager;
import com.adefreitas.gcf.android.toolkit.CloudStorageToolkit;
import com.adefreitas.gcf.android.toolkit.SftpToolkit;
import com.adefreitas.awareproviders.AccelerometerContextProvider;
import com.adefreitas.awareproviders.BluetoothContextProvider;
import com.adefreitas.awareproviders.LocationContextProvider;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.Settings;

public class GCFApplication extends Application
{
	// Application Constants
	public static final String  LOG_NAME    = "GCF_TESTSUITE"; 
	public static final boolean REMOTE_MODE = true;
	
	// GCF Communication Settings (BROADCAST_MODE Assumes a Functional TCP Relay Running)
	public static final CommMode COMM_MODE  = CommMode.MQTT;
	public static final String   IP_ADDRESS = Settings.DEV_MQTT_IP;
	public static final int      PORT 	    = Settings.DEV_MQTT_PORT;
	public static final String   CHANNEL    = "cmu/gcf_framework";
	public static final String   DEV_NAME   = Settings.getDeviceName(android.os.Build.SERIAL);
	
	// GCF Variables
	public AndroidBatteryMonitor      batteryMonitor;
	public AndroidGroupContextManager groupContextManager;
	
	// Dropbox Settings
	public  final static String DROPBOX_FOLDER = "/var/www/html/gcf/universalremote/";
	private final static String APP_KEY        = "e0c5st27ef37smy";
	private final static String APP_SECRET     = "ib32717oahl9esn";
	private final static String AUTH_TOKEN 	   = "sPolNy0CQ5IAAAAAAAAAAf2NWw6W4vuT5EXBmzPpi9KZISd9j_a_wKOTu6ZQqzUF";
	private CloudStorageToolkit cloudToolkit;
	
	/**
	 * One-Time Application Initialization Method
	 */
	@Override
	public void onCreate() 
	{
		super.onCreate();
		
		// Creates the Dropbox Helper
		cloudToolkit = new SftpToolkit(this);
		
		// Creates the Group Context Manager, which is Responsible for Context Producing and Sharing
		batteryMonitor 		= new AndroidBatteryMonitor(this, getDeviceName(), 5);
		groupContextManager = new AndroidGroupContextManager(this, getDeviceName(), batteryMonitor, false);
		
		// Handles Connections
		String connectionKey = groupContextManager.connect(COMM_MODE, IP_ADDRESS, PORT);
		groupContextManager.subscribe(connectionKey, CHANNEL);
			
		// Registers the Providers
		groupContextManager.registerContextProvider(new BluetoothContextProvider(this, groupContextManager));
		//groupContextManager.registerContextProvider(new MP3Provider(this, cloudToolkit, groupContextManager));
		groupContextManager.registerContextProvider(new AccelerometerContextProvider(this, groupContextManager));
		//groupContextManager.registerContextProvider(new AudioContextProvider(groupContextManager));
		groupContextManager.registerContextProvider(new LocationContextProvider(this, groupContextManager));
	}

	public String getDeviceName()
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String 			  deviceName  = sharedPrefs.getString("device_name", null);
		
		if (deviceName == null || deviceName.trim().length() == 0)
		{
			return Settings.getDeviceName(android.os.Build.SERIAL);
		}
		else
		{
			return deviceName;
		}
	}
	
	public void setDeviceName(String newName)
	{	
		if (groupContextManager != null)
		{
			if (!this.groupContextManager.getDeviceID().equals(newName))
			{
				Toast.makeText(this, "Changing Device ID to " + newName, Toast.LENGTH_SHORT).show();
				groupContextManager.setDeviceID(newName);
			}
		}
	}
	
	public AndroidGroupContextManager getGroupContextManager()
	{
		return groupContextManager;
	}
	
	public CloudStorageToolkit getCloudToolkit()
	{
		if (cloudToolkit == null)
		{
			Log.e(LOG_NAME, "Cloud Storage code not instantiated.  Check GCFApplication.java");
		}
		
		return cloudToolkit;
	}
	
}
