package com.adefreitas.gcfimpromptu;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.androidframework.ContextReceiver;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

/**
 * This is a template for a context provider.
 * COPY AND PASTE; NEVER USE
 * @author adefreit
 */
public class ActivityContextProvider extends ContextProvider implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
	// Context Configuration	
	private static final String CONTEXT_TYPE  = "ACT";
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	// Variables
	private Context context;
	private boolean runForever;
	
	// Intents
	Intent 		  i;
	PendingIntent p;
	
	// Intent Filters
	private ContextReceiver contextReceiver;
	private IntentFilter    filter;
	private IntentReceiver  intentReceiver;
	
	// Most Recent Value
	private String activity;
	private int	   confidence;
	
	// Activity Recognition
	private GoogleApiClient googleApiClient;
	
	public ActivityContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		this.context = context;
		
		// Default Values
		runForever = false;
		activity   = "unknown";
		confidence = 0;
		
		// Sets Up the Google API Client
		if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS)
		{
			googleApiClient = new GoogleApiClient.Builder(context)
			.addApi(ActivityRecognition.API)
			.addConnectionCallbacks(this)
			.addOnConnectionFailedListener(this)
			.build();
			googleApiClient.connect();
			
			// Creates the Intent Filter
			this.intentReceiver = new IntentReceiver();
			this.filter 		= new IntentFilter();
			this.filter.addAction(ActivityRecognitionIntentService.ACTION_ACTIVITY_UPDATE);	
		}
		else
		{
			Toast.makeText(context, "Google Play Services Not Found.", Toast.LENGTH_SHORT).show();
		}
	}

	public void start(boolean runForever)
	{
		this.runForever = runForever;
		context.registerReceiver(intentReceiver, filter);
		
		if (!googleApiClient.isConnecting() && googleApiClient.isConnected())
		{
			googleApiClient.connect();	
		}
		
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started [runForever=" + runForever + "]");
	}
	
	@Override
	public void start() 
	{
		start(runForever);
	}

	@Override
	public void stop() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
		
		if (!runForever)
		{
			ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(googleApiClient, p);
			googleApiClient.disconnect();
			context.unregisterReceiver(intentReceiver);
		}
	}

	public void reboot()
	{
		runForever = false;
		Log.d(LOG_NAME, "Rebooting");
		super.reboot();
	}
	
	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendContext() 
	{
		this.getGroupContextManager().sendContext(this.getContextType(), new String[0], new String[] { "TYPE=" + activity, "CONFIDENCE=" + confidence});
	}


	// Getters
	public String getActivity()
	{
		return activity;
	}
	
	public int getConfidence()
	{
		return confidence;
	}

	// Google API Client Methods
	@Override
	public void onConnectionFailed(ConnectionResult arg0) 
	{
		Log.d(LOG_NAME, "Activity Recognition Connection Failed");
	}

	@Override
	public void onConnected(Bundle bundle) 
	{
		Log.d(LOG_NAME, "Activity Recognition Connected");
		i = new Intent(context, ActivityRecognitionIntentService.class);
		p = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(googleApiClient, 15000, p);
	}

	@Override
	public void onConnectionSuspended(int cause) 
	{
		Log.d(LOG_NAME, "Activity Recognition Connection Suspended");	
		googleApiClient.connect();
	}

	// Intent Receiver
	private class IntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(ActivityRecognitionIntentService.ACTION_ACTIVITY_UPDATE))
			{
				// DEBUG:  Produces a Toast if the Activity Changes
				if (!activity.equals(intent.getStringExtra(ActivityRecognitionIntentService.EXTRA_ACTIVITY)))
				{
					Toast.makeText(context, "Activity: " + activity + " (" + confidence + "%)", Toast.LENGTH_SHORT).show();
				}
				
				// Stores Values
				activity   = intent.getStringExtra(ActivityRecognitionIntentService.EXTRA_ACTIVITY);
				confidence = intent.getIntExtra(ActivityRecognitionIntentService.EXTRA_CONFIDENCE, 0);
				
				// Friendly Log Time!
				Log.d(LOG_NAME, "Activity: " + activity + " (" + confidence + "%)");
			}
			else
			{
				Log.e("", "Unknown Action: " + intent.getAction());
			}
		}
	}
}
