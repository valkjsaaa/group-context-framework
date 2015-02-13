package com.adefreitas.awareproviders;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.ContextReportingThread;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Locations;
import com.aware.providers.Light_Provider.Light_Data;
import com.aware.providers.Locations_Provider.Locations_Data;

public class LocationContextProvider extends ContextProvider
{
	private static final Uri URI = Locations_Data.CONTENT_URI;
	
	private Context		     context;
	private IntentFilter     intentFilter;
	private LocationReceiver receiver;
	
	// Stores the last valid location
	private double latitude;
	private double longitude;
	private double altitude;
	
	// Stores the last accuracy
	//private static final int  MAX_ACCURACIES = 10;
	private ArrayList<Double> accuracies;
	private double 			  accuracy;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param request
	 */
	public LocationContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super("LOC", groupContextManager);
		
		// AWARE
		this.context 	  = context;
		this.intentFilter = new IntentFilter();
		this.receiver     = new LocationReceiver();
		
		// Location Specific Values
		latitude   = Double.NaN;
		longitude  = Double.NaN;
		altitude   = Double.NaN;
		accuracies = new ArrayList<Double>();
		
		stop();
	}
	
	@Override
	public void start() 
	{			
		intentFilter.addAction(Locations.ACTION_AWARE_LOCATIONS);
		context.registerReceiver(receiver, intentFilter);
		
		// Turns on the Aware Sensor
		String isGpsOn     = Aware.getSetting(context.getContentResolver(), Aware_Preferences.STATUS_LOCATION_GPS);
		String isNetworkOn = Aware.getSetting(context.getContentResolver(), Aware_Preferences.STATUS_LOCATION_NETWORK);
		
		if(!isGpsOn.equals("true")) 
		{
			Aware.setSetting(context.getContentResolver(), Aware_Preferences.STATUS_LOCATION_GPS, true);
			Log.d("GCM-ContextProvider", "GPS Spinning Up");
		}
		else
		{
			Log.d("GCM-ContextProvider", "GPS Already Started");
		}
		
		if(!isNetworkOn.equals("true")) 
		{
			Aware.setSetting(context.getContentResolver(), Aware_Preferences.STATUS_LOCATION_NETWORK, true);
			Log.d("GCM-ContextProvider", "Network Spinning Up");
		}
		else
		{
			Log.d("GCM-ContextProvider", "Network Already Started");
		}
		
		// AWARE Settings
		Aware.setSetting(context.getContentResolver(), Aware_Preferences.FREQUENCY_GPS, 0);
		Intent applySettings = new Intent(Aware.ACTION_AWARE_REFRESH);
		context.sendBroadcast(applySettings);
		
		latitude   = Double.NaN;
		longitude  = Double.NaN;
		altitude   = Double.NaN;
		
		Log.d("GCM-ContextProvider", "Location Sensor Started" + "::" + URI);
	}

	@Override
	public void stop() 
	{
		try
		{
			context.unregisterReceiver(receiver);	
		}
		catch (Exception ex)
		{
			
		}
		
		// Turns off the Aware Sensor
		String isGpsOn     = Aware.getSetting(context.getContentResolver(), Aware_Preferences.STATUS_LOCATION_GPS);
		String isNetworkOn = Aware.getSetting(context.getContentResolver(), Aware_Preferences.STATUS_LOCATION_NETWORK);
		
		if(isGpsOn.equals("true")) 
		{
			Aware.setSetting(context.getContentResolver(), Aware_Preferences.STATUS_LOCATION_GPS, false);
			Log.d("GCM-ContextProvider", "GPS Shutting Down");
		}
		else
		{
			Log.d("GCM-ContextProvider", "GPS Already Off");
		}
		
		if(isNetworkOn.equals("true")) 
		{
			Aware.setSetting(context.getContentResolver(), Aware_Preferences.STATUS_LOCATION_NETWORK, false);
			Log.d("GCM-ContextProvider", "Network Shutting Down");
		}
		else
		{
			Log.d("GCM-ContextProvider", "Network Already Off");
		}
		
		Intent applySettings = new Intent(Aware.ACTION_AWARE_REFRESH);
		context.sendBroadcast(applySettings);
		
		Log.d("GCM-ContextProvider", "Location Sensor Stopped");
	}
	
	public double getFitness(String[] parameters)
	{
		return 1.0;
	}
	
	@Override
	public void sendMostRecentReading() 
	{
		if (!Double.isNaN(latitude) && !Double.isNaN(longitude) && !Double.isNaN(altitude) && !Double.isNaN(accuracy))
		{
			this.getGroupContextManager().sendContext(this.getContextType(), "", new String[0], new String[] { Double.toString(latitude), Double.toString(longitude), Double.toString(altitude), Double.toString(accuracy) });
			
			@SuppressWarnings("unused")
			int deletedRows = context.getContentResolver().delete(URI, null, null);
		}
		else
		{
			Log.e("GCF-LOC", "Not sending location data because it's null");
		}
	}
	
	class LocationReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			Cursor lastCallCursor = context.getContentResolver().query(URI, null, null, null, "timestamp DESC LIMIT 1");
			
			if (lastCallCursor != null && lastCallCursor.moveToFirst())
			{
				do 
				{
					latitude  = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Locations_Data.LATITUDE));
					longitude = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Locations_Data.LONGITUDE));
					altitude  = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Locations_Data.ALTITUDE));
					accuracy  = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Light_Data.ACCURACY)) / 3.0;
					//accuracies.add(lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Locations_Data.ACCURACY)));
				}
				while (lastCallCursor.moveToNext());
			}
			
			lastCallCursor.close();
		}
	}
}