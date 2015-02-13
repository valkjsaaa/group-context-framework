package com.adefreitas.awareproviders;

import com.adefreitas.groupcontextframework.*;
import com.adefreitas.messages.CommMessage;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Barometer;
import com.aware.Light;
import com.aware.providers.Barometer_Provider.Barometer_Data;
import com.aware.providers.Light_Provider.Light_Data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * AWARE Light Provider.
 * Data Format: { current Lumens, max Lumens (since last transmission) }
 * 
 * @author adefreit
 *
 */
public class LightContextProvider extends ContextProvider
{
	// Context Configuration
	private static final String FRIENDLY_NAME = "Light";	
	private static final String CONTEXT_TYPE  = "LGT";
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	// Aware Configuration Steps
	private static final Uri    URI 		= Light_Data.CONTENT_URI;
	private static final String ACTION_NAME = Light.ACTION_AWARE_LIGHT;
	private static final String STATUS_NAME = Aware_Preferences.STATUS_LIGHT;
	
	private Context		  context;
	private IntentFilter  intentFilter;
	private LightReceiver receiver;
	
	private double maxLumens;
	private double currentLumens;
	private double accuracy;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param sensorManager
	 */
	public LightContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		
		// AWARE
		this.context 	  = context;
		this.intentFilter = new IntentFilter();
		this.receiver     = new LightReceiver();
				
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
	
		// Light Data Variables
		this.maxLumens     = 0.0;
		this.currentLumens = 0.0;
		this.accuracy 	   = 0.0;
		
		Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Started");
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
		String isSensorOn = Aware.getSetting(context.getContentResolver(), STATUS_NAME);
		
		if (isSensorOn.equals("true"))
		{
			Aware.setSetting(context.getContentResolver(), STATUS_NAME, false);
			Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Shutting Down");
		}
		else
		{
			Log.d(LOG_NAME, FRIENDLY_NAME + "Light Sensor Already Off");
		}
		
		// Settings Go Here
		Intent applySettings = new Intent(Aware.ACTION_AWARE_REFRESH);
		context.sendBroadcast(applySettings);
				
		Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return accuracy;
	}
	
	public void sendMostRecentReading()
	{		
		maxLumens = Math.max(currentLumens, maxLumens);
		
		this.getGroupContextManager().sendContext(getContextType(), "", new String[0], new String[] { Double.toString(currentLumens), Double.toString(maxLumens) });
		
		// Resets maxLumens
		maxLumens = 0;
		
		context.getContentResolver().delete(URI, null, null);
	}
	
    class LightReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			Cursor lastCallCursor = context.getContentResolver().query(URI, null, null, null, "timestamp DESC LIMIT 1");
			
			if (lastCallCursor != null && lastCallCursor.moveToFirst())
			{
				do 
				{
					currentLumens = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Light_Data.LIGHT_LUX));
					maxLumens     = Math.max(currentLumens, maxLumens);
					accuracy 	  = (double)lastCallCursor.getInt(lastCallCursor.getColumnIndex(Light_Data.ACCURACY)) / 3.0;
					//Log.d("GCF-ContextProvider", "Received LIGHT: " + currentLumens + " (Accuracy = " + accuracy + ")");
				}
				while (lastCallCursor.moveToNext());
			}
			
			lastCallCursor.close();
			
			context.getContentResolver().delete(URI, null, null);
		}
	}
}
