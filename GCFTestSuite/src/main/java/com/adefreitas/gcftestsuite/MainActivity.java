package com.adefreitas.gcftestsuite;

import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.androidframework.AndroidGroupContextManager;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.gcf.messages.ContextData;

/**
 * This is an Example Android Project with GCF Set Up and Ready to Go.  You're Welcome.
 * IMPORTANT:  Before you can use this project, you must do the following:
 * 		1. Change the package name from "com.example.groupexampleproject" to anything else
 * 		2. Update the manifest so that the package it points to your newly named package
 * 		3. Make sure that the manifest is pointing to your GCFApplication class
 * 		4. Go to res/values/strings.xml and change the app_name variable.
 * 		5. Verify the Communication Settings (IP Address, Port, and Communications Mode)
 * @author adefreit
 *
 */
public class MainActivity extends Activity 
{	
	// Constants
	public static final  int    LOG_SIZE     = 1000;			// Length in Characters
	private static final String LOG_CONTENTS = "LOG_CONTENTS";	// 
	
	// Link to the Application
	private GCFApplication application;
	
	// Intent Filters
	private IntentFilter  	  filter;
	private GCFIntentReceiver receiver;
	
	// Controls
	private TextView txtDevice;
	private TextView txtConsole;
	private TextView txtProviders;
	
	/**
	 * Android Method:  Used when the Activity is Created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Creates a Link to the Application
		this.application = (GCFApplication)this.getApplication();
		
		// Create Intent Filter and Receiver
		// Receivers are Set in onResume() and Removed on onPause()
		this.receiver = new GCFIntentReceiver();
		this.filter   = new IntentFilter();
		filter.addAction(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED);
		filter.addAction(AndroidGroupContextManager.ACTION_GCF_OUTPUT);	
		
		// Saves Control
		this.txtDevice    = (TextView)this.findViewById(R.id.txtDevice);
		this.txtConsole   = (TextView)this.findViewById(R.id.txtConsole);
		this.txtProviders = (TextView)this.findViewById(R.id.txtProviders);
		
		// Writes Configuration (One Time)
		showConfiguration();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) 
	{
	  super.onSaveInstanceState(savedInstanceState);
	  savedInstanceState.putString(LOG_CONTENTS, txtConsole.getText().toString());
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) 
	{
	  super.onRestoreInstanceState(savedInstanceState);

	  String logContents = savedInstanceState.getString(LOG_CONTENTS);
	  
	  txtConsole.setText(logContents);
	}
	
	/**
	 * Android Method:  Used when the Menu is Created
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Android Method:  Used when an Item in the Menu is Selected
	 */
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		Toast.makeText(this, "Selected Menu Item: " + item.toString() + " [id=" + item.getItemId() + "]", Toast.LENGTH_SHORT).show();
		
	    if (item.toString().equalsIgnoreCase("settings"))
	    {
	    	Intent intent = new Intent(this, SettingsActivity.class);
	    	this.startActivity(intent);
	    }
	    
	    return false;
	}
	
	/**
	 * Android Method:  Used when an Activity is Resumed
	 */
	protected void onResume()
	{
		super.onResume();
		this.registerReceiver(receiver, filter);
		
		showConfiguration();
	}
	
	/**
	 * Android Method:  Used when an Activity is Paused
	 */
	@Override
	protected void onPause() 
	{
	    super.onPause();
	    this.unregisterReceiver(receiver);
	}

	// Custom Application Logic Goes Here -------------------------------------------------------
	private void updateLog(String text)
	{
		String currentContents = txtConsole.getText().toString();
		
		currentContents = text + "\n" + currentContents;
		
		if (currentContents.length() > LOG_SIZE)
		{
			currentContents = currentContents.substring(0, LOG_SIZE - 1);
		}
		
		txtConsole.setText(currentContents);
		
		showConfiguration();
	}
	
	private void showConfiguration()
	{	
		String configuration  = "GCF Version:  " + GroupContextManager.FRAMEWORK_VERSION + "\n";
		configuration        += "Comm Channel: " + GCFApplication.IP_ADDRESS + "::" + GCFApplication.PORT + "\n";
		configuration        += "Providers: ";
		
		for (ContextProvider provider : this.application.getGroupContextManager().getRegisteredProviders())
		{
			if (provider.getSubscriptions().length == 0)
			{
				configuration += "[" + provider.getContextType() + "] ";
			}
			else
			{
				configuration += "[" + provider.getContextType() + "-" + provider.getSubscriptions().length + "] ";
			}
		}
		
		// Determines if Application is in Providing Context
		txtProviders.setText(configuration);
	
		// This object contains all setting values!
		txtDevice.setText(this.application.getDeviceName());
		this.application.setDeviceName(this.application.getDeviceName());
	}
	
	// Event Handlers Go Here -------------------------------------------------------------------
		
	
	// Group Context Framework Methods ----------------------------------------------------------
	public void onContextData(ContextData data)
	{		
		
	}
	
	public void onGCFOutput(String output)
	{
		updateLog(new Date() + ":\n" + output + "\n");
	}
	
	/**
	 * Handles Intents Passed by the OS
	 * @author adefreit
	 */
	// Intent Receiver --------------------------------------------------------------------------
	class GCFIntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED))
			{
				// Extracts the values from the intent
				String   contextType = intent.getStringExtra(ContextData.CONTEXT_TYPE);
				String   deviceID    = intent.getStringExtra(ContextData.DEVICE_ID);
				String[] values      = intent.getStringArrayExtra(ContextData.PAYLOAD);
				
				// Forwards Values to the Application for Processing
				onContextData(new ContextData(contextType, deviceID, values));
			}
			else if (intent.getAction().equals(AndroidGroupContextManager.ACTION_GCF_OUTPUT))
			{
				// Extracts the values from the intent
				String text = intent.getStringExtra(AndroidGroupContextManager.GCF_OUTPUT);
				
				// Forwards Values to the Application for Processing
				onGCFOutput(text);
			}
			else
			{
				Log.e("", "Unknown Action: " + intent.getAction());
			}
		}
	}
}
