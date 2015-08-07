package com.adefreit.gcfbluetoothrssi;

import java.util.Date;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.gcf.android.*;
import com.adefreitas.gcf.android.bluewave.*;
import com.adefreitas.gcf.android.toolkit.*;
import com.adefreitas.gcf.messages.ContextData;

public class MainActivity extends ActionBarActivity implements ContextReceiver
{
	// Link to the Application
	private GCFApplication application;
	
	// Intent Filters
	private IntentFilter    filter;
	private IntentReceiver  intentReceiver;
	
	// Views (Stored in Activity_Main.xml)
	private EditText txtRSSI;
	private TextView txtResultTitle;
	private TextView txtResult;
	
	// Timestamp
	private Date lastBluetoothUpdate = new Date(0);
	
	// This threshold is what the application will look for
	int minRSSI = -100;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Creates a Link to the Application
		this.application = (GCFApplication)this.getApplication();
		
		// Saves the Views
		txtRSSI 	   = (EditText)this.findViewById(R.id.txtRSSI);
		txtResultTitle = (TextView)this.findViewById(R.id.txtResultTitle);
		txtResult      = (TextView)this.findViewById(R.id.txtResult);
		
		// Creates an Event Handler
		txtRSSI.setOnEditorActionListener(onEditorActionListener);
		
		// Create Intent Filter and Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(BluetoothDevice.ACTION_FOUND);
		this.filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
		this.filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);	
	}

	/**
	 * Android Method:  Used when an Activity is Resumed
	 */
	protected void onResume()
	{
		super.onResume();

		// Sets Up the Intent Listener
		this.registerReceiver(intentReceiver, filter);
		
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		
		if (id == R.id.action_settings) 
		{
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onContextData(ContextData data) 
	{
		// TODO Auto-generated method stub	
	}
	
	@Override
	public void onBluewaveContext(JSONContextParser parser) 
	{
		// TODO Auto-generated method stub	
	}

	// Custom Application Logic Goes Here -------------------------------------------------------
	private void updateView()
	{	
		// Shows Important Dates
		txtResultTitle.setText("Scan Results (Updated " + lastBluetoothUpdate.toString() + ")\nMin RSSI = " + minRSSI);
		
		// Only Updates the UI if the GCF Exists
		if (application != null && application.getGroupContextManager() != null)
		{
			String devices = "";
			for (String deviceID : application.getGroupContextManager().getBluewaveManager().getNearbyDevices(60))
			{
				if (application.getGroupContextManager().getBluewaveManager().getRSSI(deviceID) > minRSSI)
				{
					devices += deviceID + " [rssi=" + application.getGroupContextManager().getBluewaveManager().getRSSI(deviceID) + "]\n";	
				}
			}
			
			// Grabs the Text View Control and Sets the Text
			txtResult.setText(devices.trim());
		}
		else
		{
			txtResult.setText("Waiting . . .");
		}
	}

	// Event Handlers ---------------------------------------------------------------------------
	final EditText.OnEditorActionListener onEditorActionListener = new EditText.OnEditorActionListener()
	{
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) 
		{
			if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) 
			{
				// RSSI Threshhold
	        	try
	        	{
	        		minRSSI = Integer.parseInt(txtRSSI.getText().toString());
	        		updateView();
	        		Toast.makeText(application, "Min RSSI = " + minRSSI, Toast.LENGTH_SHORT).show();
	        	}
	    		catch (Exception ex)
	    		{
	    			minRSSI = -100;
	    			txtRSSI.setText(-100);
	    		}
		    } 
			return true;
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
			else if (intent.getAction().equals(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED))
			{
				String json = intent.getStringExtra(BluewaveManager.EXTRA_OTHER_USER_CONTEXT);
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
