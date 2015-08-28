package com.adefreitas.gcfimpromptu;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.gcf.android.GCFService;
import com.adefreitas.gcfmagicapp.R;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class Splashscreen extends Activity 
{	
	// Intent Receiver
	private IntentFilter   filter;
	private IntentReceiver intentReceiver;
		
	// Link to the Application
	private GCFApplication application;
	
	// Controls
	private TextView txtStatus;
	private Button   btnBluewave;
	private Button   btnLocation;
	private Button   btnGoogle;
	private Button   btnOverride;
	
	// Intent to Start the Main Activity
	private Intent startIntent;
		
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
				
		// Saves Controls
		txtStatus   = (TextView)this.findViewById(R.id.txtStatus);
		btnBluewave = (Button)this.findViewById(R.id.btnBluewave);
		btnLocation = (Button)this.findViewById(R.id.btnLocation);
		btnGoogle   = (Button)this.findViewById(R.id.btnGoogle);
		btnOverride = (Button)this.findViewById(R.id.btnOverride);
		
		// Creates Invent Handlers
		btnBluewave.setOnClickListener(onBluewaveClickListener);
		btnLocation.setOnClickListener(onLocationClickListener);
		btnGoogle.setOnClickListener(onGoogleClickListener);
		btnOverride.setOnClickListener(onOverrideClickListener);
		
		// Generates the Intent Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter 		= new IntentFilter();
		this.filter.addAction(GCFService.ACTION_GCF_STARTED);
		
		// Creates an Intent to Start the Main Activity
		startIntent = new Intent(this, MainActivity.class);
	}
	
	/**
	 * Android Method:  Used when an Activity is Resumed
	 */
	protected void onResume()
	{
		super.onResume();
		
		application.setInForeground(true);
		
		// Sets Up the Intent Listener
		this.registerReceiver(intentReceiver, filter);
		
		if (application.getGCFService() != null)
		{
			startApplication();	
		}
	}
	
	/**
	 * Android Method:  Used when an Activity is Paused
	 */
	@Override
	protected void onPause() 
	{
	    super.onPause();
	    
	    // Removes the Intent Listener
	    this.unregisterReceiver(intentReceiver);
	    
	    // Removes the 
	    application.setInForeground(false);
	}
	
	/**
	 * Custom Method to Start the Main Activity
	 * NOTE:  GCF Service Must be Instatiated First!
	 */
	protected void startApplication()
	{
		boolean locationWorking = application.verifyLocationServices();
		boolean googleWorking   = application.verifyGoogleServices();
		boolean bluewaveWorking = application.verifyBluewaveServices();
		
		if (locationWorking && googleWorking && bluewaveWorking)
		{
			startActivity(startIntent);
			finish();
		}
		else
		{
			txtStatus.setVisibility(View.VISIBLE);
			
			if (!locationWorking)
			{
				btnLocation.setVisibility(View.VISIBLE);
			}
			
			if (!googleWorking)
			{
				btnGoogle.setVisibility(View.VISIBLE);
			}
			
			if (!bluewaveWorking)
			{
				btnBluewave.setVisibility(View.VISIBLE);
			}
		}
	}
	
	/**
	 * Enables Bluetooth Discovery
	 */
	private OnClickListener onBluewaveClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) 
		{
			if (application.getGCFService() != null)
			{
				application.getGCFService().getBluewaveManager().setDiscoverable(true);
				finish();
			}
			else
			{
				Toast.makeText(application, "Service Unavailable", Toast.LENGTH_SHORT).show();
			}
		}
	};
	
	/**
	 * Enables Location Tracking
	 */
	private OnClickListener onLocationClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) 
		{
			Intent myIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
	        startActivity(myIntent);
	        finish();
		}
	};
	
	/**
	 * Enables Google Play Services
	 */
	private OnClickListener onGoogleClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) 
		{
			int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(Splashscreen.this);
			
			// Will Only Display a Dialog if an ERROR is received!
			Dialog googleDialog = GooglePlayServicesUtil.getErrorDialog(status, Splashscreen.this, 1);
			
			// Displays the Dialog IFF it exists.
			if (googleDialog != null)
			{
				googleDialog.show();
				finish();
			}
		}
	};
	
	/**
	 * Bypasses Warnings and Runs the App
	 */
	private OnClickListener onOverrideClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) 
		{
			startActivity(startIntent);
			finish();
		}
	};
	
	
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
				Thread startThread = new Thread()
				{
					public void run()
					{
						try
						{
							sleep(500);
							startApplication();
						}
						catch (Exception ex)
						{
							ex.printStackTrace();
						}
					}
				};
				
				startThread.run();
			}
		}
	}
}
