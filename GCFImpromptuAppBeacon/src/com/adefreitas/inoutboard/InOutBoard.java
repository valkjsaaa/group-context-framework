package com.adefreitas.inoutboard;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.adefreitas.androidbluewave.BluewaveManager;
import com.adefreitas.androidbluewave.JSONContextParser;
import com.adefreitas.androidframework.ContextReceiver;
import com.adefreitas.magicappserver.GCFApplication;
import com.adefreitas.magicappserver.R;
import com.adefreitas.messages.ContextData;
import com.adefreitas.messages.ContextRequest;

public class InOutBoard extends ActionBarActivity implements ContextReceiver
{
	// Link to the Application
	private GCFApplication application;
	
	// Context Provider
	UserIdentityContextProvider p;
	
	// Intent Filters
	private IntentFilter   filter;
	private IntentReceiver intentReceiver;

	// Controls
	private Toolbar  toolbar;
	private ListView lstUsers;
	
	/**
	 * Android Method:  Used when the Activity is Created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_in_out_board);
		
		// Creates a Link to the Application
		this.application = (GCFApplication)this.getApplication();
		this.p           = (UserIdentityContextProvider)application.getGroupContextManager().getContextProvider("USER_ID");
		
		// Links to Controls/Views
		toolbar  = (Toolbar)this.findViewById(R.id.toolbar);
		lstUsers = (ListView)this.findViewById(R.id.lstUsers);
		
		// Sets Up Event Handlers
		lstUsers.setOnItemClickListener(onItemClickListener);
		
		// Sets Up Toolbar
		this.setSupportActionBar(toolbar);
		
		// Create Intent Filter and Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(App_InOutBoardIdentity.ACTION_IDENTITY_UPDATE);
		this.filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
		this.filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	}

	/**
	 * Android Method:  Used when the Menu is Created
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.in_out_board, menu);
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
		
		// Asks for Context
		application.getGroupContextManager().sendRequest("USER_ID", ContextRequest.MULTIPLE_SOURCE, new String[0], 60000, new String[0]);
		
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
	    
	    application.getGroupContextManager().cancelRequest("USER_ID");
	    
	    // Disables the Intent Listener
	    this.unregisterReceiver(intentReceiver);
	    
	    application.removeContextReceiver(this);
	}
	
	/**
	 * Android method:  Used when a Menu Item is Selected
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) 
		{
			return true;
		}
		else if (id == R.id.action_clear_list)
		{
			p.clear();
			updateView();
		}
		else if (id == R.id.action_identify)
		{
			promptForLocation();
		}
		else if (id == R.id.action_insert_manually)
		{
			promptForManualInsertion();
		}
		return super.onOptionsItemSelected(item);
	}
	
	// Activity Methods --------------------------------------------
	private void updateView()
	{   
		toolbar.setTitle(this.getString(R.string.title_activity_in_out_board) + " [" + p.getLocationName() + "]");
		
        UserDataListAdapter adapter = new UserDataListAdapter(this, R.layout.user_info_single, p.getCurrentUserData());
        
        lstUsers.setAdapter(adapter);
	}
	
	private void promptForLocation()
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Specify Location");
		alert.setMessage("Enter the Location for this Sign");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int whichButton) 
			{
			  String value = input.getText().toString();
			  p.setLocationName(value);
			  updateView();
			  Toast.makeText(application, "Location Changed To: " + p.getLocationName(), Toast.LENGTH_SHORT).show();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() 
		{
		  public void onClick(DialogInterface dialog, int whichButton) 
		  {
		    // Canceled.
		  }
		});

		alert.show();
	}

	private void promptForManualInsertion()
	{
		LayoutInflater      inflater     = getLayoutInflater();
		View 		        dialogLayout = inflater.inflate(R.layout.prompt_insert_manually, null);
		AlertDialog.Builder alert 		 = new AlertDialog.Builder(this);
		alert.setView(dialogLayout);
		
		// Grabs Controls
		final TextView txtDeviceName  = (TextView)dialogLayout.findViewById(R.id.txtDeviceName);
		final TextView txtDisplayName = (TextView)dialogLayout.findViewById(R.id.txtDisplayName);
		
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int whichButton) 
			{
				if (txtDeviceName.getText().length() > 0 && txtDisplayName.getText().length() > 0)
				{
					try
					{
						String deviceID = txtDeviceName.getText().toString();
						String name     = txtDisplayName.getText().toString();
						
						JSONObject obj = new JSONObject();
						obj.put("name", name);
						obj.put("phone", "TBD");
						
					    p.addEntry(deviceID, obj);
					    Toast.makeText(application, "Added Bluetooth ID " + deviceID + " [" + name + "]", Toast.LENGTH_SHORT).show();
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
				else
				{
					Toast.makeText(application, "Device ID / Name Cannot be NULL", Toast.LENGTH_SHORT).show();
				}
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() 
		{
		  public void onClick(DialogInterface dialog, int whichButton) 
		  {
		    // Canceled.
		  }
		});
		
	    // Shows the Finished Dialog
		alert.show();	
	}

	private void promptForDetails(UserData data)
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);

		alert.setTitle("User Details");
		alert.setMessage("User: " + data.getName());

		// Set an EditText view to get user input 
		final TextView details = new TextView(this);
		details.setTextColor(Color.BLACK);
		alert.setView(details);
		
		details.setText(data.toString());

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int whichButton) 
			{
				// Do Nothing
			}
		});

		alert.show();
	}

	// EVENT HANDLERS ----------------------------------------------
	final OnItemClickListener onItemClickListener = new OnItemClickListener() 
    {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			try
			{
				UserData user = (UserData)lstUsers.getItemAtPosition(position);
				promptForDetails(user);
			}
			catch (Exception ex)
			{
				Toast.makeText(application, "Problem Occurred While Selecting User: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
				ex.printStackTrace();
			}
		}
    };
	
	// GCF METHODS -------------------------------------------------
	@Override
	public void onContextData(ContextData data) 
	{
		System.out.println("Received: " + data.toString());
		p.update(data.getPayload("VALUES"));
		updateView();
	}

	@Override
	public void onBluewaveContext(JSONContextParser parser) 
	{
		// TODO Auto-generated method stub
	}

	// Intent Receiver
	private class IntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(App_InOutBoardIdentity.ACTION_IDENTITY_UPDATE))
			{
				updateView();
			}
			else if (intent.getAction().equals(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED))
			{
				// This is the Raw JSON from the Device
				String json = intent.getStringExtra(BluewaveManager.OTHER_USER_CONTEXT);
				
				// Creates a Parser
				JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
				
				// Updates Entry
				p.updateEntry(parser.getDeviceID());
				
				// Updates the View
				updateView();
			}
			else if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
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
