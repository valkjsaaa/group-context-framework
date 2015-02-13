package com.adefreitas.magicappserver;

import java.util.Date;

import org.json.JSONObject;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.androidbluewave.JSONContextParser;
import com.adefreitas.androidframework.ContextReceiver;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ContextData;

/**
 * This is an Example Android Project with GCF Set Up and Ready to Go.  You're Welcome.
 * IMPORTANT:  Before you can use this project, you must do the following:
 * 		1. Change the package name from "com.example.groupexampleproject" to anything else
 * 		2. Update the manifest so that the package it points to your newly named package
 * 		3. Make sure that the manifest is pointing to your GCFApplication class
 * 		4. Go to res/values/strings.xml and change the app_name variable.
 * 		5. Verify the Communication Settings (IP Address, Port, and Communications Mode) in GCFApplication
 * @author adefreit
 */
public class MainActivity extends Activity implements ContextReceiver
{	
	// Link to the Application
	private GCFApplication application;
	
	// Intent Filters
	private IntentFilter    filter;
	private IntentReceiver  intentReceiver;
	
	// Android Controls
	private RelativeLayout layoutRoot;
	private TextView	   txtLabel;
	private ImageView      imgSnapToIt;
	
	// Timestamp
	private Date 	lastSnapToItContact;
	private Date	lastBluetoothScan;
	private boolean showDebugInfo = true;
	
	/**
	 * Android Method:  Used when the Activity is Created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Initializes Timestamp
		this.lastSnapToItContact = new Date(0);
		this.lastBluetoothScan   = new Date(0);
		
		// Creates a Link to the Application
		this.application = (GCFApplication)this.getApplication();
				
		// Grabs Controls
		layoutRoot  = (RelativeLayout)this.findViewById(R.id.layoutRoot);
		txtLabel    = (TextView)this.findViewById(R.id.txtLabel);
		imgSnapToIt = (ImageView)this.findViewById(R.id.imgSnapToIt);
				
		// Sets Event Handler
		layoutRoot.setOnClickListener(onClickListener);
		
		// Create Intent Filter and Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.filter.addAction(GCFApplication.APP_TICK);
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
	 * Android Method:  Used when a Menu Item is Selected
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		if (item.toString().equalsIgnoreCase(this.getString(R.string.title_activity_settings)))
	    {
	    	Intent intent = new Intent(this, SettingsActivity.class);
	    	this.startActivity(intent);
	    }
		else if (item.toString().equalsIgnoreCase(this.getString(R.string.title_activity_quit)))
		{
			android.os.Process.killProcess(android.os.Process.myPid());
		}
		
		return true;
	}
	
	/**
	 * Android Method:  Used when an Activity is Resumed
	 */
	protected void onResume()
	{
		super.onResume();

		// Sets Up the Intent Listener
		this.registerReceiver(intentReceiver, filter);
		
		// Sets Up Context Listening
		application.setContextReceiver(this);
		
		// Sets Up Initial View
		this.updateView();
	}
	
	/**
	 * Android Method:  Used when an Activity is Paused
	 */
	@Override
	protected void onPause() 
	{
	    super.onPause();
	    
	    // Disables the Intent Listener
	    this.unregisterReceiver(intentReceiver);
	}

	// GCF Logic Goes Here ----------------------------------------------------------------------
	@Override
	public void onContextData(ContextData data)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void onGCFOutput(String output)
	{
		// TODO Auto-generated method stub	
	}

	@Override
	public void onBluewaveContext(JSONContextParser parser)
	{
		// Attempts to Extract Connection Information
		JSONObject context   = parser.getJSONObject("magic");
		
		try
		{
			boolean snapToItEnabled = context.has("SNAP_TO_IT") ? context.getBoolean("SNAP_TO_IT") : false;
			
			if (snapToItEnabled)
			{
				lastSnapToItContact = new Date();
				updateView();
				//Toast.makeText(this, "Detected Snap-To-It Compatible Device", Toast.LENGTH_SHORT).show();
			}
		}
		catch (Exception ex)
		{
			Toast.makeText(this, "Error Parsing Context in MainActivity: " + ex.getMessage(), Toast.LENGTH_SHORT).show();	
		}
	}

	// Custom Application Logic Goes Here -------------------------------------------------------
	private void updateView()
	{
		String result = "GCF " + GroupContextManager.FRAMEWORK_VERSION 
						+ " [" + GCFApplication.COMM_MODE + "::" + GCFApplication.IP_ADDRESS + "::" + GCFApplication.PORT + "]\n\n";
		
		// Grabs IDs of All Registered Context Providers
		String providers = "Context Providers:\n";
		for (ContextProvider p : application.getGroupContextManager().getRegisteredProviders())
		{
			providers += "[" + p.getContextType() + "] ";
		}
		
		// Shows Active Context Providers
		result += providers + "\n\n";
		
		// Shows Important Dates
		result += "Snap-To-It: " + lastSnapToItContact + "\n";
		result += "Bluetooth:  " + lastBluetoothScan + "\n\n";
		
		String[] deviceIDs = application.bluewaveManager.getNearbyDevices(30);
		result += "Bluetooth Devices (" + deviceIDs.length + " found)\n";
		
		for (String deviceID : deviceIDs)
		{
			result += deviceID + " [rssi=" + application.bluewaveManager.getRSSI(deviceID) + "]\n";
		}
		
		// Shows/Hides the Snap-To-It Symbol
		if (new Date().getTime() - lastSnapToItContact.getTime() < 60000 && !showDebugInfo)
		{
			imgSnapToIt.setVisibility(View.VISIBLE);
		}
		else
		{
			imgSnapToIt.setVisibility(View.GONE);
		}
		
		// Grabs the Text View Control and Sets the Text
		txtLabel.setText(result.trim());
	}
	
	// Event Handlers Go Here -------------------------------------------------------------------
	public final OnClickListener onClickListener = new OnClickListener()
	{	
		@Override
		public void onClick(View v) 
		{
			showDebugInfo = !showDebugInfo;
			
			if (showDebugInfo)
			{
				txtLabel.setTextColor(Color.WHITE);
			}
			else
			{
				txtLabel.setTextColor(Color.BLACK);
			}
		}
	};
	
	// Intent Receiver
	private class IntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
			{
				lastBluetoothScan = new Date();
				updateView();
				//Toast.makeText(application, "Bluetooth Scan Completed", Toast.LENGTH_SHORT).show();
			}
			else if (intent.getAction().equals(GCFApplication.APP_TICK))
			{
				updateView();
			}
			else
			{
				Log.e("", "Unknown Action: " + intent.getAction());
			}
		}
	}
}
