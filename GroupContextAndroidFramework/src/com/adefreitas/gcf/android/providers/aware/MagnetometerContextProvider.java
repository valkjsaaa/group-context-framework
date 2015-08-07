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
import com.aware.Magnetometer;
import com.aware.providers.Magnetometer_Provider.Magnetometer_Data;

/**
 * AWARE Magnetometer Provider.
 * Data Format: { current magnetometer vector, max magnetometer vector (since last transmission) }
 * 
 * @author adefreit
 *
 */
public class MagnetometerContextProvider extends ContextProvider
{
	private static final Uri URI = Magnetometer_Data.CONTENT_URI;
	
	private Context		  		 context;
	private IntentFilter  		 intentFilter;
	private MagnetometerReceiver receiver;
	
	private double accuracy;
	private double x, y, z, maxX, maxY, maxZ;
	
	private ContextReportingThread t;
	
	public MagnetometerContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super("MAG", groupContextManager);

		// AWARE
		this.context 	  = context;
		this.intentFilter = new IntentFilter();
		this.receiver 	  = new MagnetometerReceiver();
		
		// Magnetometer Values
		this.accuracy = 0.0;
		this.x 		  = Double.NaN;
		this.y 		  = Double.NaN;
		this.z 		  = Double.NaN;
		this.maxX     = 0.0;
		this.maxY     = 0.0;
		this.maxZ     = 0.0;
		
		stop();
	}
	
	@Override
	public void start() 
	{
		intentFilter.addAction(Magnetometer.ACTION_AWARE_MAGNETOMETER);
		context.registerReceiver(receiver, intentFilter);
		
		// Turns on the Aware Sensor
		String isMagnetometerOn = Aware.getSetting(context.getContentResolver(), Aware_Preferences.STATUS_MAGNETOMETER);
		
		if (!isMagnetometerOn.equals("true"))
		{
			Aware.setSetting(context.getContentResolver(), Aware_Preferences.STATUS_MAGNETOMETER, true);
			Log.d("GCM-ContextProvider", "Magnetometer Spinning Up");
		}
		else
		{
			Log.d("GCM-ContextProvider", "Magnetometer Already On");
		}
		
		// Settings Go Here
		Intent applySettings = new Intent(Aware.ACTION_AWARE_REFRESH);
		context.sendBroadcast(applySettings);
		
		// Turns on the Reporting Thread
		t = new ContextReportingThread(this);
		t.start();
		
		Log.d("GCM-ContextProvider", "Magnetometer Started");
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
		String isMagnetometerOn = Aware.getSetting(context.getContentResolver(), Aware_Preferences.STATUS_MAGNETOMETER);
		
		if (isMagnetometerOn.equals("true"))
		{
			Aware.setSetting(context.getContentResolver(), Aware_Preferences.STATUS_MAGNETOMETER, false);
			Log.d("GCM-ContextProvider", "Magnetometer Shutting Down");
		}
		else
		{
			Log.d("GCM-ContextProvider", "Magnetometer Sensor Already Off");
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
		
		Log.d("GCM-ContextProvider", "Magnetometer Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return accuracy;
	}

	@Override
	public void sendContext() 
	{
		if (!Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(z))
		{
			String[] data = new String[] { Double.toString(x), Double.toString(y), Double.toString(z), Double.toString(maxX), Double.toString(maxY), Double.toString(maxZ)};
			this.getGroupContextManager().sendContext(getContextType(), new String[0], data);
			
			maxX = 0.0;
			maxY = 0.0;
			maxZ = 0.0;
			
			context.getContentResolver().delete(URI, null, null);	
		}
	}

	class MagnetometerReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			Cursor lastCallCursor = context.getContentResolver().query(URI, null, null, null, "timestamp DESC LIMIT 1");
			
			if (lastCallCursor != null && lastCallCursor.moveToFirst())
			{
				do 
				{
					x 		 = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Magnetometer_Data.VALUES_0));
					y 		 = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Magnetometer_Data.VALUES_1));
					z 		 = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Magnetometer_Data.VALUES_2));
					accuracy = lastCallCursor.getDouble(lastCallCursor.getColumnIndex(Magnetometer_Data.ACCURACY)) / 3.0;

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
