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
import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.providers.Applications_Provider.Applications_Foreground;

public class ApplicationContextProvider extends ContextProvider
{
	//  Keys
	public  static final String CONTEXT_TYPE = "APP";
	private static final Uri    URI 	     = Applications_Foreground.CONTENT_URI; //Uri.parse("content://com.aware.provider.applications/applications_foreground");
	
	private Context		        context;
	private IntentFilter        intentFilter;
	private ApplicationReceiver receiver;
	
	// Application Variables
	private String currentApplication;
		
	private ContextReportingThread t;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param sensorManager
	 */
	public ApplicationContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		
		// AWARE
		this.context 	  = context;
		this.intentFilter = new IntentFilter();
		this.receiver     = new ApplicationReceiver();
		
		// Sets Initial Values
		currentApplication = "";
		
		stop();
	}
	
	@Override
	public void start() 
	{
		intentFilter.addAction(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND);
		context.registerReceiver(receiver, intentFilter);
		
		// Turns on the Aware Sensor
		String isApplicationSensorOn = Aware.getSetting(context.getContentResolver(), Aware_Preferences.STATUS_APPLICATIONS);
		
		if (!isApplicationSensorOn.equals("true"))
		{
			Aware.setSetting(context.getContentResolver(), Aware_Preferences.STATUS_APPLICATIONS, true);
			Log.d("GCM-ContextProvider", "Foreground Application Spinning Up");
		}
		else
		{
			Log.d("GCM-ContextProvider", "Foreground Application Sensor Already On");
		}
		
		// Settings Go Here
		Intent applySettings = new Intent(Aware.ACTION_AWARE_REFRESH);
		context.sendBroadcast(applySettings);
	
		// Turns on the Reporting Thread
//		t = new ContextReportingThread(this);
//		t.start();
		
		Log.d("GCM-ContextProvider", "Application Sensor Started");
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
		String isApplicationSensorOn = Aware.getSetting(context.getContentResolver(), Aware_Preferences.STATUS_APPLICATIONS);
		
		if (isApplicationSensorOn.equals("true"))
		{
			Aware.setSetting(context.getContentResolver(), Aware_Preferences.STATUS_APPLICATIONS, false);
			Log.d("GCM-ContextProvider", "Foreground Application Sensor Shutting Down");
		}
		else
		{
			Log.d("GCM-ContextProvider", "Foreground Application Sensor Already Off");
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
		
		Log.d("GCM-ContextProvider", "Foreground Application Sensor Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return (currentApplication.length() > 0) ? 1.0 : 0.0;
	}
	
	public void sendContext()
	{			
		this.getGroupContextManager().sendContext(CONTEXT_TYPE, new String[0], new String[] { currentApplication });
			
		context.getContentResolver().delete(URI, null, null);
	}
	
    class ApplicationReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			Cursor lastCallCursor = context.getContentResolver().query(URI, null, null, null, "timestamp DESC LIMIT 1");
			
			if (lastCallCursor != null && lastCallCursor.moveToFirst())
			{
				do 
				{
					currentApplication = lastCallCursor.getString(lastCallCursor.getColumnIndex(Applications_Foreground.APPLICATION_NAME));
					Log.d("GCF-ContextProvider", "Received APPLICATION: " + currentApplication + " (Accuracy = " + getFitness(null) + ")");
				}
				while (lastCallCursor.moveToNext());
			}
			
			lastCallCursor.close();
		}
	}
}
