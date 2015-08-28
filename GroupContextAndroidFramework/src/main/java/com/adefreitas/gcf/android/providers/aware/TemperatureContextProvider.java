package com.adefreitas.gcf.android.providers.aware;

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
import com.aware.Temperature;
import com.aware.providers.Temperature_Provider.Temperature_Data;

/**
 * AWARE Temperature Provider.
 * Data Format: { temperature (Celsius), max temperature (since last transmission) }
 * 
 * @author adefreit
 *
 */
public class TemperatureContextProvider extends ContextProvider
{
	// Context Configuration
	private static final String FRIENDLY_NAME = "Temperature";	
	private static final String CONTEXT_TYPE  = "TEMP";
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	// Aware Configuration Steps
	private static final Uri    URI 		  = Temperature_Data.CONTENT_URI;
	private static final String ACTION_NAME   = Temperature.ACTION_AWARE_TEMPERATURE;
	private static final String STATUS_NAME   = Aware_Preferences.STATUS_TEMPERATURE;
	
	// Context Variables
	private Context		   context;
	private IntentFilter   intentFilter;
	private CustomReceiver receiver;
	private double 		   maxTemperature;
	private double 		   currentTemperature;
	
	private ContextReportingThread t;
	
	public TemperatureContextProvider(Context context, GroupContextManager groupContextManager) 
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
	
		this.currentTemperature = 0.0;
		this.maxTemperature     = 0.0;
		
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
	public void sendContext() 
	{	
		maxTemperature = Math.max(maxTemperature, currentTemperature);
		
		this.getGroupContextManager().sendContext(this.getContextType(), new String[0], new String[] { Double.toString(currentTemperature), Double.toString(maxTemperature) });		
		
		maxTemperature = 0.0;
		
		int deletedRows = context.getContentResolver().delete(URI, null, null);
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
					currentTemperature = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Temperature_Data.TEMPERATURE_CELSIUS));
					maxTemperature     = Math.max(maxTemperature,  currentTemperature);
				}
				while (lastCallCursor.moveToNext());
			}
			
			lastCallCursor.close();
		}
	}
	
}
