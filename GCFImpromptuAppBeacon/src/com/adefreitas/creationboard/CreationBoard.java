package com.adefreitas.creationboard;

import java.util.ArrayList;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.androidbluewave.BluewaveManager;
import com.adefreitas.androidbluewave.JSONContextParser;
import com.adefreitas.androidframework.ContextReceiver;
import com.adefreitas.androidframework.toolkit.HttpToolkit;
import com.adefreitas.magicappserver.GCFApplication;
import com.adefreitas.magicappserver.R;
import com.adefreitas.messages.ContextData;

public class CreationBoard extends ActionBarActivity implements ContextReceiver
{
	// Link to the Application
	private GCFApplication application;
	
	// Intent Filters
	private IntentFilter   filter;
	private IntentReceiver intentReceiver;

	// Controls
	private Toolbar  toolbar;
	private ListView lstUsers;
	
	private HttpToolkit  toolkit;
	private final String ACTION_FOUND_DETAILS = "FOUND_DETAILS";
	
	// String Array
	ArrayList<UserData> detected = new ArrayList<UserData>();
	
	/**
	 * Android Method:  Used when the Activity is Created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_creation_board);
		
		// Creates a Link to the Application
		this.application = (GCFApplication)this.getApplication();
		
		// Links to Controls/Views
		toolbar  = (Toolbar)this.findViewById(R.id.toolbar);
		lstUsers = (ListView)this.findViewById(R.id.lstUsers);
		
		// Sets Up Toolbar
		this.setSupportActionBar(toolbar);
		
		toolkit = new HttpToolkit(application);
		
		// Create Intent Filter and Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
		this.filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.filter.addAction(ACTION_FOUND_DETAILS);
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
		//application.getGroupContextManager().sendRequest("USER_ID", ContextRequest.MULTIPLE_SOURCE, new String[0], 60000, new String[] { "CHANNEL=IN_OUT_SIGN" });
		
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
			updateView();
		}
		else if (id == R.id.action_identify)
		{
			//promptForLocation();
		}
		else if (id == R.id.action_insert_manually)
		{
			//promptForManualInsertion();
		}
		return super.onOptionsItemSelected(item);
	}
	
	// Activity Methods --------------------------------------------
	private void updateView()
	{   
		//toolbar.setTitle(this.getString(R.string.title_activity_in_out_board) + " [" + p.getLocationName() + "]");
		
        UserDataListAdapter adapter = new UserDataListAdapter(this, R.layout.user_info_single, detected);
        
        lstUsers.setAdapter(adapter);
	}
	
	private void updateUsers(String userData)
	{
		boolean updated = false;
		
		try
		{
			if (userData.length() > 0)
			{
				JSONObject obj = new JSONObject(userData);
				String deviceID = obj.getString("deviceID");
				
				for (UserData data : new ArrayList<UserData>(detected))
				{
					if (data.getDeviceID().equals(deviceID))
					{
						detected.remove(data);
					}
				}
				
				if (!updated)
				{
					UserData data = new UserData(deviceID, 
							obj.getString("userName"), 
							obj.getString("telephone"), 
							application.getGroupContextManager().getDeviceID());
					
					detected.add(data);
				}
			}
		}
		catch (Exception ex)
		{
			Toast.makeText(CreationBoard.this, "Error Parsing Context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	
	// GCF METHODS -------------------------------------------------
	@Override
	public void onContextData(ContextData data) 
	{
		System.out.println("Received: " + data.toString());
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
			if (intent.getAction().equals(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED))
			{
				// This is the Raw JSON from the Device
				String json = intent.getStringExtra(BluewaveManager.OTHER_USER_CONTEXT);
				
				// Creates a Parser
				JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
			
				// Attempts to Get Details about this user
				//String url = Uri.encode("http://gcf.cmu-tbank.com/apps/creationfest/getDeviceDetails.php?deviceID=" + parser.getDeviceID());
				toolkit.get("http://gcf.cmu-tbank.com/apps/creationfest/getDeviceDetails.php?deviceID=" + parser.getDeviceID().replace(" ",  "%20"), ACTION_FOUND_DETAILS);
			}
			else if (intent.getAction().equals(ACTION_FOUND_DETAILS))
			{
				String response = intent.getStringExtra(HttpToolkit.HTTP_RESPONSE);
				
				try
				{
					if (response != null && response.length() > 0)
					{
						updateUsers(response);
					}
				}
				catch (Exception ex)
				{
					Toast.makeText(CreationBoard.this, "Error Retrieving Device Details: " + ex.getMessage(), Toast.LENGTH_LONG).show();
				}
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
