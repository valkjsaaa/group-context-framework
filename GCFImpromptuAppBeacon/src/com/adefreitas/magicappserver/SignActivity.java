package com.adefreitas.magicappserver;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.androidbluewave.JSONContextParser;
import com.adefreitas.androidframework.ContextReceiver;
import com.adefreitas.messages.ContextData;

public class SignActivity extends ActionBarActivity implements ContextReceiver
{
	// This is a Link to a the Main Application ( contains some custom methods :)
	private GCFApplication application;
	
	// Places all of Your Android Views (contained in res/layout/<ACTIVITY NAME>.xml) here
	private TextView txtHelloWorld;
	
	/**
	 * Android Method:  Used when the Activity is Created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sign);
		
		// Gets a Reference to the Main Application
		this.application = (GCFApplication)this.getApplication();
		
		// Gets the "Views" from the Android Activity (activity_sign.xml)
		txtHelloWorld = (TextView)this.findViewById(R.id.txtHelloWorld);
	}

	/**
	 * Android Method:  Used when an Activity is Resumed
	 */
	protected void onResume()
	{
		super.onResume();
		
		// Sets Up Context Listening
		application.setContextReceiver(this);
	}
	
	/**
	 * Android Method:  Used when an Activity is Paused
	 */
	@Override
	protected void onPause() 
	{
	    super.onPause();

	    application.removeContextReceiver(this);
	}
	
	/**
	 * Android Method:  Used when the Menu is Created
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sign, menu);
		return true;
	}

	/**
	 * Android Method:  Used when a Menu Item is Selected
	 * Go to res/menu to see the XML file that generates the menu for this sign
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * GCF Method:  Used when Context is Delivered to the App
	 */
	@Override
	public void onContextData(ContextData data) 
	{
		// TODO Auto-generated method stub
	}
	
	/**
	 * Bluewave Method:  Used when Bluewave Context is Detected by the App
	 */
	@Override
	public void onBluewaveContext(JSONContextParser parser) 
	{
		try
		{
			if (parser.getJSONObject("preferences") != null && parser.getJSONObject("preferences").has("name"))
			{
				Log.d("SIGN", "Found Preferences and Name!");
				txtHelloWorld.setText("I just detected: " + parser.getJSONObject("preferences").getString("name"));
			}	
			else
			{
				Log.d("SIGN", "Did not find Preferences and Name :(");
			}
		}
		catch (Exception ex)
		{
			// Displays a Toast Notification on the Phone to Show You the Problem
			Toast.makeText(this, "Problem Analyzing Bluewave Context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
}
