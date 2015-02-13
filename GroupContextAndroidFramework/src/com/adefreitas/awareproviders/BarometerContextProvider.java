package com.adefreitas.awareproviders;

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
import com.aware.Accelerometer;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Barometer;
import com.aware.LinearAccelerometer;
import com.aware.providers.Accelerometer_Provider.Accelerometer_Data;
import com.aware.providers.Barometer_Provider.Barometer_Data;
import com.aware.providers.Linear_Accelerometer_Provider.Linear_Accelerometer_Data;

/**
 * AWARE Barometer Provider.
 * Data Format: { current Pressure (mbar/hPa), max Pressure since last transmission }
 * 
 * @author adefreit
 *
 */
public class BarometerContextProvider extends ContextProvider
{
	// Context Configuration
	private static final String FRIENDLY_NAME = "Barometer";	
	private static final String CONTEXT_TYPE  = "BAR";
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	// Aware Configuration Steps
	private static final Uri    URI 		  = Barometer_Data.CONTENT_URI;
	private static final String ACTION_NAME   = Barometer.ACTION_AWARE_BAROMETER;
	private static final String STATUS_NAME   = Aware_Preferences.STATUS_BAROMETER;
	
	// Context Variables
	private Context		   context;
	private IntentFilter   intentFilter;
	private CustomReceiver receiver;
	private double 		   maxPressure;
	private double 		   currentPressure;
	
	private ContextReportingThread t;
	
	public BarometerContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		
		this.context 	  = context;
		this.intentFilter = new IntentFilter();
		this.receiver 	  = new CustomReceiver();
				
		stop();
	}

	@Override
	public void start() 
	{
		intentFilter.addAction(ACTION_NAME);
		context.registerReceiver(receiver, intentFilter);
		
		// Turns on the Aware Sensor
		String isSensorOn = Aware.getSetting(context.getContentResolver(), STATUS_NAME);
		
		if (!isSensorOn.equals("true"))
		{
			Aware.setSetting(context.getContentResolver(), STATUS_NAME, true);
			Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Spinning Up");
		}
		else
		{
			Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Already On");
		}
		
		// Settings Go Here
		Intent applySettings = new Intent(Aware.ACTION_AWARE_REFRESH);
		context.sendBroadcast(applySettings);
	
		this.currentPressure = 0.0;
		this.maxPressure     = 0.0;
		
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
		
		Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendMostRecentReading() 
	{	
		maxPressure = Math.max(currentPressure, maxPressure);
		
		this.getGroupContextManager().sendContext(this.getContextType(), "", new String[0], new String[] { String.format("%f", currentPressure), String.format("%f", maxPressure) });		
		
		maxPressure = 0.0;
		
		context.getContentResolver().delete(URI, null, null);
	}
	
	private class CustomReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			Cursor lastCallCursor = context.getContentResolver().query(URI, null, null, null, "timestamp DESC LIMIT 1");
			
			if (lastCallCursor != null && lastCallCursor.moveToFirst())
			{
				do 
				{
					currentPressure = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Barometer_Data.AMBIENT_PRESSURE));
					maxPressure     = Math.max(currentPressure, maxPressure);
				}
				while (lastCallCursor.moveToNext());
			}
			
			lastCallCursor.close();
			
			context.getContentResolver().delete(URI, null, null);
		}
	}
	
}
