package com.adefreitas.awareproviders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.aware.Accelerometer;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.providers.Accelerometer_Provider.Accelerometer_Data;

/**
 * AWARE Accelerometer Provider.  Includes Gravity.
 * Data Format: { current X, current Y, current Z, max X, max Y, max Z }
 * 
 * @author adefreit
 *
 */
public class AccelerometerContextProvider extends ContextProvider
{
	// Context Configuration
	private static final String FRIENDLY_NAME = "Accelerometer";	
	private static final String CONTEXT_TYPE  = "ACC";
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	// Aware Configuration Steps
	private static final Uri    URI 		  = Accelerometer_Data.CONTENT_URI;
	private static final String ACTION_NAME   = Accelerometer.ACTION_AWARE_ACCELEROMETER;
	private static final String STATUS_NAME   = Aware_Preferences.STATUS_ACCELEROMETER;
	
	// GCF Variables
	private Context		   context;
	private IntentFilter   intentFilter;
	private CustomReceiver receiver;
	
	// Provider Variables
	private double x, y, z, maxX, maxY, maxZ;
	
	public AccelerometerContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		
		this.context 	  = context;
		this.intentFilter = new IntentFilter();
		this.receiver 	  = new CustomReceiver();
				
		intentFilter.addAction(ACTION_NAME);
		
		this.x    = 0.0;
		this.y    = 0.0;
		this.z    = 0.0;
		this.maxX = 0.0;
		this.maxY = 0.0;
		this.maxZ = 0.0;
		
		stop();
	}

	@Override
	public void start() 
	{
		context.registerReceiver(receiver, intentFilter);
		
		// Turns on the Aware Sensor
		String isAccelerometerOn = Aware.getSetting(context.getContentResolver(), STATUS_NAME);
		
		if (!isAccelerometerOn.equals("true"))
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
		String isAccelerometerOn = Aware.getSetting(context.getContentResolver(), STATUS_NAME);
		
		if (isAccelerometerOn.equals("true"))
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
		String[] data = new String[] { Double.toString(x), Double.toString(y), Double.toString(z), Double.toString(maxX), Double.toString(maxY), Double.toString(maxZ)};
		this.getGroupContextManager().sendContext(this.getContextType(), new String[0], data);		
		
		maxX = 0.0;
		maxY = 0.0;
		maxZ = 0.0;
		
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
					x 				    = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Accelerometer_Data.VALUES_0));
					y 				    = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Accelerometer_Data.VALUES_1));
					z 			        = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Accelerometer_Data.VALUES_2));
					double amplitude    = Math.sqrt(x*x + y*y + z*z);
					double maxAmplitude = Math.sqrt(maxX*maxX + maxY*maxY + maxZ*maxZ);
					
					if (amplitude > maxAmplitude)
					{
						maxX = x;
						maxY = y;
						maxZ = z;
					}
				}
				while (lastCallCursor.moveToNext());
			}
			
			lastCallCursor.close();
			
			context.getContentResolver().delete(URI, null, null);
		}
	}
	
}
