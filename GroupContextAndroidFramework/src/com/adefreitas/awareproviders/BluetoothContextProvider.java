package com.adefreitas.awareproviders;

import java.util.ArrayList;
import java.util.Date;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Bluetooth;
import com.aware.providers.Bluetooth_Provider.Bluetooth_Data;

/**
 * AWARE Bluetooth Provider.
 * Data Format: { Device1, Device2, etc. }
 * 
 * @author adefreit
 *
 */
public class BluetoothContextProvider extends ContextProvider
{
	// Context Configuration
	private static final String FRIENDLY_NAME = "Bluetooth";	
	private static final String CONTEXT_TYPE  = "BLU";
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	// Aware Configuration Steps
	private static final Uri    URI 		= Bluetooth_Data.CONTENT_URI;
	private static final String ACTION_NAME = Bluetooth.ACTION_AWARE_BLUETOOTH_NEW_DEVICE;
	private static final String STATUS_NAME = Aware_Preferences.STATUS_BLUETOOTH;
	
	private Context		   context;
	private IntentFilter   intentFilter;
	private CustomReceiver receiver;
	
	// Custom Values
	private static final int EXPIRATION_TIME = 90000;
	private ArrayList<BluetoothRecord> nearbyDevices;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param sensorManager
	 */
	public BluetoothContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		
		// AWARE
		this.context 	   = context;
		this.intentFilter  = new IntentFilter();
		this.receiver      = new CustomReceiver();
		this.nearbyDevices = new ArrayList<BluetoothRecord>();
		
		stop();
	}
	
	public void setBluetoothName(String newName)
	{		
		BluetoothAdapter.getDefaultAdapter().setName(newName);
		
		System.out.println("Bluetooth Name Changed to: " + newName);
	}
	
	public String getBluetoothName()
	{
		return BluetoothAdapter.getDefaultAdapter().getName();
	}
	
	@Override
	public void start() 
	{
		intentFilter.addAction(ACTION_NAME);
		context.registerReceiver(receiver, intentFilter);
		
		// Clears the Database
		context.getContentResolver().delete(URI, null, null);
		
		// Turns on the Aware Sensor
		String isSensorOn = Aware.getSetting(context.getContentResolver(), STATUS_NAME);
		
		if (!isSensorOn.equals("true"))
		{
			Aware.setSetting(context.getContentResolver(), STATUS_NAME, true);
			Aware.setSetting(context.getContentResolver(), Aware_Preferences.FREQUENCY_BLUETOOTH, 60);
			Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Spinning Up");
		}
		else
		{
			Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Already On");
		}
		
		// Initializes Array
		nearbyDevices.clear();
		
		// Settings Go Here
		Intent applySettings = new Intent(Aware.ACTION_AWARE_REFRESH);
		context.sendBroadcast(applySettings);
		
		Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Started");
	}

	@Override
	public void stop() 
	{
		try
		{
			context.unregisterReceiver(receiver);	
		}
		catch (Exception ex)
		{
			
		}
		
		// Turns off the Aware Sensor
		String isSensorOn = Aware.getSetting(context.getContentResolver(), STATUS_NAME);
		
		if (isSensorOn.equals("true"))
		{
			Aware.setSetting(context.getContentResolver(), STATUS_NAME, false);
			Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Shutting Down");
		}
		else
		{
			Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Already Off");
		}
		
		// Settings Go Here
		Intent applySettings = new Intent(Aware.ACTION_AWARE_REFRESH);
		context.sendBroadcast(applySettings);
				
		Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}
	
	public void sendContext()
	{	
		// Temp Array to Store all Bluetooth IDs
		ArrayList<String> deviceIDs = new ArrayList<String>();
		
		// Extracts all Bluetooth IDs from the Records and Adds them to the Temp Array
		for (BluetoothRecord record : new ArrayList<BluetoothRecord>(nearbyDevices))
		{
			if (record.isExpired())
			{
				nearbyDevices.remove(record);
			}
			else
			{
				deviceIDs.add(record.getId());	
			}
		}
		
		// Sends the List of IDs
		this.getGroupContextManager().sendContext(getContextType(), new String[0], deviceIDs.toArray(new String[0]));
		
		context.getContentResolver().delete(URI, null, null);
	}
	
    class CustomReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			Cursor lastCallCursor = context.getContentResolver().query(URI, null, null, null, "timestamp DESC");
						
			if (lastCallCursor != null && lastCallCursor.moveToFirst())
			{
				do 
				{
					String deviceID = lastCallCursor.getString(lastCallCursor.getColumnIndex(Bluetooth_Data.BT_NAME));
			
					if (deviceID != null)
					{
						BluetoothRecord newRecord = new BluetoothRecord(deviceID);
												
						// Removes Old Entries
						for (BluetoothRecord record : new ArrayList<BluetoothRecord>(nearbyDevices))
						{
							if (record.equals(newRecord.getId()))
							{
								Log.d(LOG_NAME, "  Removing " + record.getId() + " and replacing with " + newRecord.getId());
								nearbyDevices.remove(record);
							}
						}
						
						// Adds the New Entry
						nearbyDevices.add(newRecord);
					
						Log.d(LOG_NAME, "Discovered " + deviceID + " via Bluetooth.");
					}
				}
				while (lastCallCursor.moveToNext());
			}
			
			lastCallCursor.close();
			
			context.getContentResolver().delete(URI, null, null);
			
			// TODO:  Sends Updated Bluetooth Readings			
			sendContext();
		}
	}

    class BluetoothRecord
    {
    	private Date   timestamp;
    	private String id;
    	
    	public BluetoothRecord(String id)
    	{
    		this.id 	   = id;
    		this.timestamp = new Date();
    	}
    	
    	public boolean equals(String id)
    	{
    		Log.d(LOG_NAME, "Comparing " + this.id + " to " + id);
    		
    		// Todo:  GCF SPECIFIC CODE
    		if (id.startsWith("GCF") && this.id.startsWith("GCF"))
    		{
    			Log.d(LOG_NAME, "  GCF Case Detected");
    			
    			String[] idComponents_A = this.id.split(":");
        		String[] idComponents_B = id.split(":");
        		
        		// Looks for Match of the Form "GCF:<DEVICE ID>";
        		if (idComponents_A.length >= 2 && idComponents_B.length >= 2)
        		{
        			Log.d(LOG_NAME, "  Comparing [" + idComponents_A[1] + " to " + idComponents_B[1] + "]");
        			return (idComponents_A[1].equals(idComponents_B[1]));
        		}
        		else
        		{
        			Log.d(LOG_NAME, "  Not Enough Data in Name");
        		}
    		}

    		// Generic Comparison
    		return this.id == id;
    	}
    	
    	public boolean isExpired()
    	{
    		long timeElapsed = (new Date().getTime() - timestamp.getTime());
    		return timeElapsed > EXPIRATION_TIME;
    	}
    	
    	public Date getTimestamp()
    	{
    		return timestamp;
    	}
    	
    	public String getId()
    	{
    		return id;
    	}
    }
}
