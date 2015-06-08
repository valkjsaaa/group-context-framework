package com.adefreitas.magicappserver;

import java.util.Date;

import org.json.JSONObject;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.androidbluewave.BluewaveManager;
import com.adefreitas.androidbluewave.JSONContextParser;
import com.adefreitas.androidframework.ContextReceiver;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.inoutboard.InOutBoard;
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
public class MainActivity extends ActionBarActivity implements ContextReceiver
{	
	// Constants
	private static final String BLUEWAVE_SCAN   = "BLUEWAVE_SCAN";
	private static final String SNAP_TO_IT_SCAN = "STI_SCAN";
	
	// Link to the Application
	private GCFApplication application;
	
	// Intent Filters
	private IntentFilter    filter;
	private IntentReceiver  intentReceiver;
	
	// Android Controls
	private Toolbar	 toolbar;
	private TextView txtContext;
	private TextView txtBluewave;
	private TextView txtDevices;
	
	// Timestamp
	private Date 	lastSnapToItContact;
	private Date	lastBluetoothUpdate;
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
		this.lastBluetoothUpdate   = new Date(0);
		
		// Creates a Link to the Application
		this.application = (GCFApplication)this.getApplication();
				
		// Grabs Controls
		toolbar	    = (Toolbar)this.findViewById(R.id.toolbar);
		txtContext  = (TextView)this.findViewById(R.id.txtContext);
		txtBluewave = (TextView)this.findViewById(R.id.txtBluewave);
		txtDevices  = (TextView)this.findViewById(R.id.txtDevices);
				
		// Sets Up Toolbar
		this.setSupportActionBar(toolbar);
		
		// Create Intent Filter and Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(BluetoothDevice.ACTION_FOUND);
		this.filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);	    
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
		else if (item.toString().equalsIgnoreCase(this.getString(R.string.title_activity_in_out_board)))
	    {
	    	Intent intent = new Intent(this, InOutBoard.class);
	    	this.startActivity(intent);
	    }
		else if (item.toString().equalsIgnoreCase(this.getString(R.string.title_activity_sign)))
	    {
	    	Intent intent = new Intent(this, SignActivity.class);
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
	    
	    application.removeContextReceiver(this);
	}

	/**
	 * Android Method:  Called to Save the State
	 */
	@Override
	protected void onSaveInstanceState(Bundle state) 
	{
	    super.onSaveInstanceState(state);
	    state.putLong(BLUEWAVE_SCAN, lastBluetoothUpdate.getTime());
	    state.putLong(SNAP_TO_IT_SCAN, lastSnapToItContact.getTime());
	}
	
	/**
	 * Android Method:  Called to Restore the State
	 */
	@Override
	protected void onRestoreInstanceState(Bundle state)
	{
		lastBluetoothUpdate   = new Date(state.getLong(BLUEWAVE_SCAN));
		lastSnapToItContact = new Date(state.getLong(SNAP_TO_IT_SCAN));
	}
	
	// GCF Logic Goes Here ----------------------------------------------------------------------
	@Override
	public void onContextData(ContextData data)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void onBluewaveContext(JSONContextParser parser)
	{		
		try
		{
			boolean snapToItEnabled = parser.getJSONRoot().has("snap-to-it");
			
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
		// Grabs IDs of All Registered Context Providers
		String providers = "";
		for (ContextProvider p : application.getGroupContextManager().getRegisteredProviders())
		{
			if (p.isSharable())
			{
				providers += "[" + p.getContextType() + "]\n";
			}
		}
		
		// Shows Active Context Providers
		txtContext.setText(providers.trim());
		
		// Shows Important Dates
		txtBluewave.setText("Completed " + lastBluetoothUpdate.toString());
		
		String devices = "";
		for (String deviceID : application.getGroupContextManager().getBluewaveManager().getNearbyDevices(GCFApplication.SCAN_PERIOD_IN_SECONDS))
		{
			devices += deviceID + " [rssi=" + application.getGroupContextManager().getBluewaveManager().getRSSI(deviceID) + "]\n";
		}
		
		// Grabs the Text View Control and Sets the Text
		txtDevices.setText(devices.trim());
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
				//layoutRoot.setVisibility(View.VISIBLE);
			}
			else
			{
				//layoutRoot.setVisibility(View.GONE);
			}
		}
	};
	
	// Intent Receiver
	private class IntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND))
			{
				lastBluetoothUpdate = new Date();
				updateView();
			}
			else if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
			{
				lastBluetoothUpdate = new Date();
				updateView();
			}
			else
			{
				Log.e("", "Unknown Action: " + intent.getAction());
			}
		}
	}
}
