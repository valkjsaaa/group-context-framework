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
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.LinearAccelerometer;
import com.aware.providers.Accelerometer_Provider.Accelerometer_Data;
import com.aware.providers.Linear_Accelerometer_Provider.Linear_Accelerometer_Data;

/**
 * AWARE Linear Accelerometer Provider.  Excludes Gravity.
 * Data Format: { X, Y, Z, maxX, maxY, maxZ }
 * 
 * @author adefreit
 *
 */
public class LinearAccelerometerContextProvider extends ContextProvider
{
	// Context Configuration
	private static final String FRIENDLY_NAME = "Linear Accelerometer";	
	private static final String CONTEXT_TYPE  = "LACC";
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	// Aware Configuration Steps
	private static final Uri    URI 		= Linear_Accelerometer_Data.CONTENT_URI;
	private static final String ACTION_NAME = LinearAccelerometer.ACTION_AWARE_LINEAR_ACCELEROMETER;
	private static final String STATUS_NAME = Aware_Preferences.STATUS_LINEAR_ACCELEROMETER;
	
	// 
	private Context		   context;
	private IntentFilter   intentFilter;
	private CustomReceiver receiver;
	private boolean 	   offsetCalculated;
	private int			   numOffsets;
	private double		   offsetX, offsetY, offsetZ;	// used to eliminate noise
	private double 		   x, y, z, maxX, maxY, maxZ;

	/**
	 * Constructor
	 * @param context
	 * @param groupContextManager
	 */
	public LinearAccelerometerContextProvider(Context context, GroupContextManager groupContextManager) 
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
		
		// Initializes Variables
		this.offsetCalculated = false;
		this.numOffsets 	  = 0;
		this.offsetX 		  = 0.0;
		this.offsetY 		  = 0.0;
		this.offsetZ 		  = 0.0;
		this.x    			  = 0.0;
		this.y    			  = 0.0;
		this.z    			  = 0.0;
		this.maxX 			  = 0.0;
		this.maxY 			  = 0.0;
		this.maxZ 			  = 0.0;
		
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
	public void sendMostRecentReading() 
	{	
		// One Last Chance to Find the Highest Magnitude
		double amplitude    = Math.sqrt(x*x + y*y + z*z);
		double maxAmplitude = Math.sqrt(maxX*maxX + maxY*maxY + maxZ*maxZ);
		
		String[] data = new String[] { Double.toString(x), Double.toString(y), Double.toString(z), Double.toString(maxX), Double.toString(maxY), Double.toString(maxZ)};
		
		if (amplitude > maxAmplitude)
		{
			maxX = x;
			maxY = y;
			maxZ = z;
		}
		
		// Reports the Current Vector, as Well as the Maximum
		this.getGroupContextManager().sendContext(this.getContextType(), "", new String[0], data);		
		
		// Resets Max Vector Since we Already Reported It
		maxX = 0.0;
		maxY = 0.0;
		maxZ = 0.0;
		
		// Deletes all Values in the Database
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
					if (!offsetCalculated && numOffsets < 5)
					{
						offsetX = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Accelerometer_Data.VALUES_0));
						offsetY = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Accelerometer_Data.VALUES_1));
						offsetZ = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Accelerometer_Data.VALUES_2));
						numOffsets++;
						
						//Log.d(LOG_NAME, String.format("Offset Calibration [%1.3f %1.3f %1.3f]", offsetX, offsetY, offsetZ));
						
						offsetCalculated = (numOffsets >= 5);
					}
					else
					{
						x 				    = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Accelerometer_Data.VALUES_0)) - offsetX;
						y 				    = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Accelerometer_Data.VALUES_1)) - offsetY;
						z 				    = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Accelerometer_Data.VALUES_2)) - offsetZ;
						double amplitude    = Math.sqrt(x*x + y*y + z*z);
						double maxAmplitude = Math.sqrt(maxX*maxX + maxY*maxY + maxZ*maxZ);
						
						if (amplitude > maxAmplitude)
						{
							maxX = x;
							maxY = y;
							maxZ = z;
						}
						//Log.d(LOG_NAME, String.format("Adjusted LACC: [%1.3f %1.3f %1.3f]; Offset [%1.3f %1.3f %1.3f]", x, y, z, offsetX, offsetY, offsetZ));
					}

				}
				while (lastCallCursor.moveToNext());
			}
			
			lastCallCursor.close();
			
			context.getContentResolver().delete(URI, null, null);
		}
	}
	
}
