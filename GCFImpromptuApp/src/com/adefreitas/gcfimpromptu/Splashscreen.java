package com.adefreitas.gcfimpromptu;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;

import com.adefreitas.androidframework.GCFService;
import com.adefreitas.gcfmagicapp.R;

public class Splashscreen extends Activity 
{
    // Splash screen timer
    private static int SPLASH_TIME_OUT = 2000;
	
	// Intent Receiver
	private IntentFilter    filter;
	private IntentReceiver  intentReceiver;
	
	// Flags
	private boolean splashTimeout = false;
	
	// Link to the Application
	private GCFApplication application;
	
	// Intent to Start the Main Activity
	Intent startIntent;
	
	/**
	 * Android Method:  Runs when the Activity is Created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splashscreen);
		
		// Creates a Link to the Application
		this.application = (GCFApplication)this.getApplication();
		
		// Generates the Intent Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter 		= new IntentFilter();
		this.filter.addAction(GCFService.ACTION_GCF_STARTED);
		
		// Creates an Intent to Start the Main Activity
		startIntent = new Intent(this, MainActivity.class);
		
		// Lets the Main Screen Exist for a Few Seconds
		new Handler().postDelayed(new Runnable() 
		{	 
	        /*
	         * Showing splash screen with a timer. This will be useful when you
	         * want to show case your app logo / company
	         */
	        @Override
	        public void run() 
	        {
	            splashTimeout = true;
	            startApplication();
	        }
	    }, SPLASH_TIME_OUT);
	}
	
	/**
	 * Android Method:  Used when an Activity is Resumed
	 */
	protected void onResume()
	{
		super.onResume();
		
		// Sets Up the Intent Listener
		this.registerReceiver(intentReceiver, filter);
	}
	
	/**
	 * Android Method:  Used when an Activity is Paused
	 */
	@Override
	protected void onPause() 
	{
	    super.onPause();
	    this.unregisterReceiver(intentReceiver);
	}
	
	/**
	 * Custom Method to Start the Main Activity
	 * NOTE:  GCF Service Must be Instatiated First!
	 */
	protected void startApplication()
	{
		if (application.getGCFService().isReady() && splashTimeout)
		{
			startActivity(startIntent);
			finish();
		}
	}
	
	/**
	 * Intent Receiver
	 * @author adefreit
	 *
	 */
	private class IntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equalsIgnoreCase(GCFService.ACTION_GCF_STARTED))
			{
				startApplication();
			}
		}
	}
}
