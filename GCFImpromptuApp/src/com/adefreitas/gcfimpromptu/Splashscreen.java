package com.adefreitas.gcfimpromptu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff.Mode;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.adefreitas.androidframework.GCFService;
import com.adefreitas.gcfmagicapp.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class Splashscreen extends Activity 
{
    // Splash screen timer
    private static int SPLASH_TIME_OUT = 1000;
	
	// Intent Receiver
	private IntentFilter   filter;
	private IntentReceiver intentReceiver;
	
	// Flags
	private boolean splashTimeout = false;
	
	// Link to the Application
	private GCFApplication application;
	
	// Intent to Start the Main Activity
	private Intent   startIntent;
	private Runnable splashRunnable;
	
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
		
		// Creates a Runnable that Starts the Application
		splashRunnable = new Runnable() 
		{	 
	        @Override
	        public void run() 
	        {
	            splashTimeout = true;
	            startApplication();
	        }
	    };
		
		// Verifies that Critical Services are Enabled before Starting the App
		verifyServices();
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
		if (application.getGCFService() != null && application.getGCFService().isReady() && splashTimeout)
		{
			startActivity(startIntent);
			finish();
		}
		else
		{
			ProgressBar progressBar = (ProgressBar)this.findViewById(R.id.progressBar);
			progressBar.setVisibility(View.VISIBLE);
			progressBar.getIndeterminateDrawable().setColorFilter(0xFF0186D5, android.graphics.PorterDuff.Mode.MULTIPLY);
		}
	}
	
	/**
	 * Custom Method to Make Sure that the Right Services are Enabled
	 */
	private void verifyServices()
	{
		// LOCATION CHECK
		LocationManager lm 				 = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		boolean 		locationEnabled = false;
		
		int locationMode = 0;
	    String locationProviders;

	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
	    {
		    try 
		    {
		    	locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
		    } 
		    catch (Exception e) 
		    {
		    	e.printStackTrace();
		    }
	
		    locationEnabled = locationMode != Settings.Secure.LOCATION_MODE_OFF;
	    }
	    else
	    {
	    	// Looks at Location Mode for Jelly Bean and Below Systems
	        locationProviders = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
	        locationEnabled   = !TextUtils.isEmpty(locationProviders);
	    }

		if (!locationEnabled)
		{
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		    dialog.setMessage("Location Services Not Enabled");
		    dialog.setPositiveButton("Location Settings", new DialogInterface.OnClickListener() 
		      {
                @Override
	            public void onClick(DialogInterface paramDialogInterface, int paramInt) 
                {
	              Intent myIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		          startActivity(myIntent);
		          finish();
		        }
              });
		    dialog.setNegativeButton("Ignore", new DialogInterface.OnClickListener() 
		      {
                @Override
	            public void onClick(DialogInterface paramDialogInterface, int paramInt) 
                {
                	Toast.makeText(Splashscreen.this, "Some services may not work properly without location.", Toast.LENGTH_LONG).show();
                	
                	// Lets the Main Screen Exist for a Few Seconds
        			new Handler().postDelayed(splashRunnable, SPLASH_TIME_OUT);
	            }
 	          });
		    dialog.show();
		}

		// GOOGLE PLAY SERVICES CHECK
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		
		try 
		{
		    if (status != ConnectionResult.SUCCESS) 
		    {
		        GooglePlayServicesUtil.getErrorDialog(status, this, 1).show();
		        finish();
		    }
		} 
		catch (Exception e) 
		{
		    Log.e("IMPROMPTU", "" + e);
		}
		
		// Lets the Main Screen Exist for a Few Seconds
		if (locationEnabled && status == ConnectionResult.SUCCESS)
		{
			new Handler().postDelayed(splashRunnable, SPLASH_TIME_OUT);
		}
		else
		{
	        //finish();
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
