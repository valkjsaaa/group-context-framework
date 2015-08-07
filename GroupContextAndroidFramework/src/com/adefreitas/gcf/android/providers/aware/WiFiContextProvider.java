package com.adefreitas.gcf.android.providers.aware;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextReportingThread;
import com.adefreitas.gcf.GroupContextManager;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.WiFi;
import com.aware.providers.WiFi_Provider.WiFi_Data;

/**
 * AWARE WiFi Access Point Provider.
 * Data Format: { SSID_1, Strength_1, SSID_2, Strength_2, . . . }
 * 
 * @author adefreit
 *
 */
public class WiFiContextProvider extends ContextProvider
{
	// Context Configuration
	private static final String FRIENDLY_NAME = "WiFi";	
	private static final String CONTEXT_TYPE  = "WIFI";
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	private static final String DB_QUERY      = "rssi ASC LIMIT 10";
	
	// Aware Configuration Steps
	private static final Uri    URI 		  = WiFi_Data.CONTENT_URI;
	private static final String ACTION_NAME   = WiFi.ACTION_AWARE_WIFI_SCAN_ENDED;
	private static final String STATUS_NAME   = Aware_Preferences.STATUS_WIFI;
	
	// Context Variables
	private Context		      context;
	private IntentFilter      intentFilter;
	private CustomReceiver    receiver;
	private ArrayList<String> results;
	private double 			  fitness;
		
	private ContextReportingThread t;
	
	public WiFiContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		
		this.context 	  = context;
		this.intentFilter = new IntentFilter();
		this.receiver 	  = new CustomReceiver();
		this.results      = new ArrayList<String>();
		this.fitness      = 0.0;
		
		stop();
	}

	@Override
	public void start() 
	{
		// Cleans Out the Database
		int deletedRows = context.getContentResolver().delete(URI, null, null);
		Log.d(LOG_NAME, "Starting:  Removed " + deletedRows + " from " + FRIENDLY_NAME + " database.");
		
		intentFilter.addAction(ACTION_NAME);
		context.registerReceiver(receiver, intentFilter);
		
		// Turns on the Aware Sensor
		String isSensorOn = Aware.getSetting(context.getContentResolver(), STATUS_NAME);
		
		if (!isSensorOn.equals("true"))
		{
			Aware.setSetting(context.getContentResolver(), STATUS_NAME, true);
			Aware.setSetting(context.getContentResolver(), Aware_Preferences.FREQUENCY_WIFI, 60);
			Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Spinning Up");
		}
		else
		{
			Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Already On");
		}
		
		// Settings Go Here
		Intent applySettings = new Intent(Aware.ACTION_AWARE_REFRESH);
		context.sendBroadcast(applySettings);
	
		// Initializes
		results.clear();
		fitness = 0.0;
		
		Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Started");
	}

	@Override
	public void stop() 
	{
		// Removes the Aware Context Receiver
		try
		{
			context.unregisterReceiver(receiver);	
		}
		catch (Exception ex)
		{
			
		}
		
		// Turns off the Aware Sensor
		String isSensorOn = Aware.getSetting(context.getContentResolver(), STATUS_NAME);
		
		if (isSensorOn.equals("true"))
		{
			Aware.setSetting(context.getContentResolver(), STATUS_NAME, false);
			Log.d(LOG_NAME, FRIENDLY_NAME + " Shutting Down");
		}
		else
		{
			Log.d(LOG_NAME, FRIENDLY_NAME + " Already Off");
		}
		
		// Settings Go Here
		Intent applySettings = new Intent(Aware.ACTION_AWARE_REFRESH);
		context.sendBroadcast(applySettings);
		
		// Halts the Reporting Thread
		if (t != null)
		{
			t.halt();
			t = null;	
		}
		
		Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return fitness;
	}

	@Override
	public void sendContext() 
	{			
		this.getGroupContextManager().sendContext(this.getContextType(), new String[0], results.toArray(new String[0]));
		
		context.getContentResolver().delete(URI, null, null);
	}
	
	private class CustomReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			// Now that we are getting data, set fitness to 1.0
			fitness = 1.0;
			
			// Clears Existing Results
			results.clear();
			
			// Performs a Query of the Local DB to Grab the Results
			Cursor cursor = context.getContentResolver().query(URI, null, null, null, DB_QUERY);
			
			if (cursor != null && cursor.moveToFirst())
			{
				do 
				{
					String ssid 		  = cursor.getString(cursor.getColumnIndex(WiFi_Data.SSID));
					double signalStrength = (double)(Math.abs(cursor.getInt(cursor.getColumnIndex(WiFi_Data.RSSI)))) / 100.0;
					
					if (!results.contains(ssid))
					{
						results.add(ssid);
						results.add(Double.toString(signalStrength));
						//Log.d(LOG_NAME, "Found SSID: " + ssid + "(" + signalStrength + ")");
					}
				}
				while (cursor.moveToNext());
			}
			
			cursor.close();
			
			// Cleans Out the Database
			int deletedRows = context.getContentResolver().delete(URI, null, null);
			Log.d(LOG_NAME, "Retrieved:  Removed " + deletedRows + " from " + FRIENDLY_NAME + " database.");
		}
	}
	
}
