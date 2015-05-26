package com.adefreitas.androidbluewave;

import java.util.Date;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.adefreitas.androidframework.toolkit.HttpToolkit;

public class ContextListener
{
	// Log Constant
	private static final String LOG_NAME = "Bluewave_Listener";
	
	// Intent Filters
	private IntentFilter    	   filter;
	private ListenerIntentReceiver receiver;
	
	// Android Application Context
	private Context context;
	
	// HTTP Toolkit
	private HttpToolkit httpToolkit;
	
	// Other Device Context
	private HashMap<String, ContextInfo> archive = new HashMap<String, ContextInfo>();
		
	/**
	 * Constructor
	 * @param context
	 */
	public ContextListener(Context context, HttpToolkit httpToolkit)
	{
		this.context     = context;
		this.httpToolkit = httpToolkit;
		
		// Sets Up Intent Filtering and Listening
		this.receiver = new ListenerIntentReceiver();
		this.filter   = new IntentFilter();
		filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_DOWNLOADED);
		filter.addAction(BluewaveManager.ACTION_BLUETOOTH_SCAN_UPDATE);
		this.context.registerReceiver(receiver, filter);
	}
	
	/**
	 * Class Used to Keep Track of Bluetooth Downloads
	 * @author adefreit
	 *
	 */
	private class ContextInfo
	{
		private String  json;
		private String  deviceID;
		private Date    timestamp;
		private boolean downloaded;
		
		public ContextInfo(String deviceID, Date timestamp)
		{
			this.deviceID   = deviceID;
			this.timestamp  = timestamp;
			this.downloaded = false;
		}

		public void setJson(String json)
		{
			this.json 		= json;
			this.downloaded = true;
		}
	
		public String getJson() {
			return json;
		}

		public String getDeviceID() {
			return deviceID;
		}

		public boolean isDownloaded() {
			return downloaded;
		}
		
		public Date getDateDownloaded() {
			return timestamp;
		}
	}
	
	/** 
	 * Handles Intents
	 * @author adefreit
	 */
	private class ListenerIntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(BluewaveManager.ACTION_BLUETOOTH_SCAN_UPDATE))
			{
				onBluetoothScanUpdate(context, intent);
			}
			else if (intent.getAction().equals(BluewaveManager.ACTION_OTHER_USER_CONTEXT_DOWNLOADED))
			{
				onContextFileDownloaded(context, intent);
			}
			else
			{
				Log.e(LOG_NAME, "Unknown Intent: " + intent.getAction());
			}
		}
		
		private void onBluetoothScanUpdate(Context context, Intent intent)
		{
			// Grabs Bluetooth IDs
			String bluetoothID = intent.getStringExtra(BluewaveManager.BLUETOOTH_SCAN_RESULT);
			
			if (bluetoothID != null)
			{
				// Extracts the Components
				// Should be in the form BLU::<DEVICE_NAME>::<FILE NAME>::<TIMESTAMP>
											
				// Processes Bluetooth Devices with the Above Naming Convention
				if (BluewaveManager.isBluewaveName(bluetoothID))
				{
					String[] components = bluetoothID.split("::");
					String deviceID     = components[1];
					String downloadPath = components[2];
					long   lastUpdate   = Long.parseLong(components[3]);
				   	
					if (!archive.containsKey(deviceID) || archive.get(deviceID).getDateDownloaded().getTime() < lastUpdate)
					{
						// Downloads the New Context
						String url = downloadPath.replace(" ", "%20");
						httpToolkit.get(url, BluewaveManager.ACTION_OTHER_USER_CONTEXT_DOWNLOADED);
						
						// Creates an Entry (and replaces the previous entry)
						archive.put(deviceID, new ContextInfo(deviceID, new Date(lastUpdate)));
					}
					else if (archive.containsKey(deviceID))
					{
						// Retransmits the Old Context if No Update was Found
						if (deviceID != null && archive.get(deviceID).getJson() != null)
						{
							String json = archive.get(deviceID).getJson();
							Intent i = new Intent(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
							i.putExtra(BluewaveManager.OTHER_USER_CONTEXT, json);
							i.putExtra(BluewaveManager.NEW_CONTEXT, false);
							context.sendBroadcast(i);
							
							// TODO:  Debug Toast
							Log.d(LOG_NAME, "Bluewave Context (Old) from " + deviceID + " (" + json.length() + " bytes)");
						}	
					}
				}	
			}
		}
		
		private void onContextFileDownloaded(Context context, Intent intent)
		{
			// This is the Raw JSON from the Device
			String json = intent.getStringExtra(HttpToolkit.HTTP_RESPONSE);
			
			// Makes Sure that the Uploaded JSON Actually Has JSON in it!
			if (json != null)
			{
				JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
				
		    	// Remembers this File to Prevent Redundant Downloading
				String deviceID = parser.getDeviceID();
				
				if (deviceID != null)
				{
					archive.get(deviceID).setJson(json);
					Intent i = new Intent(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
					i.putExtra(BluewaveManager.OTHER_USER_CONTEXT, json);
					i.putExtra(BluewaveManager.NEW_CONTEXT, true);
					context.sendBroadcast(i);
					
					// TODO:  Debug Toast
					Log.d(LOG_NAME, "Bluewave Context (New) from " + deviceID + " (" + json.length() + " bytes)");
				}	
			}
		}
	
	}
}
