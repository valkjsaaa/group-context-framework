package com.adefreitas.androidbluewave;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class BluetoothScanner 
{
	private static final String LOG_NAME 	  	   = "Bluewave-Scan";
	private static final long   MAX_TIME_IN_MEMORY = 60000;	// Max Time for a Device to be Remembered in the Log
	private static final long	SCAN_DURATION 	   = 20000;	// Max Time for a Scan (Will Cut off Automatically if it takes longer than this)
		
	private Context 		 context;
	private BluetoothAdapter bluetooth;
	
	private HashMap<String, Date>  deviceLog;
	private HashMap<String, Short> rssiLog;
	
	// Wake Lock
	PowerManager powerManager;
	WakeLock	 wakeLock;
	
	// Scanning Thread
	private ScanThread scanningThread;
	private boolean    foundDevices = false;
	
	/**
	 * Constructor
	 * @param context
	 */
	public BluetoothScanner(Context context)
	{
		powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
		wakeLock     = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BluewaveWakelockTag");
		
		// Initializes Variables
		this.context   = context;
		this.bluetooth = BluetoothAdapter.getDefaultAdapter();
				
		// Allows Bluetooth Adapter to be in Discoverable mode
		if (bluetooth.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
		{
			Intent discoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0); // Set to 0 for infinite, Max is 1200 (I think)
			discoverable.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.context.startActivity(discoverable);	
		}
		
		// Sets Filter for When Bluetooth Devices are Found
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		context.registerReceiver(mReceiver, filter);
		
		// Initializes Data Structures
		deviceLog = new HashMap<String, Date>();
		rssiLog   = new HashMap<String, Short>();
		
		scanningThread = new ScanThread();
		scanningThread.start();
	}
	
	public void start()
	{		
		startScan();
//		
//		// Terminates the Current Scan Thread
//		if (scanningThread != null)
//		{
//			stop();
//		}
//		
//		// Creates a New Scan Thread
//		scanningThread = new ScanThread();
//		scanningThread.start();
	}
	
	public void stop()
	{
		stopScan();
//		
//		if (scanningThread != null)
//		{
//			scanningThread.halt();
//		}
	}
	
	private void startScan() 
	{
		if (!bluetooth.isEnabled())
		{
			bluetooth.enable();
			Log.d(LOG_NAME, "Enabling Bluetooth");
		}
		
		if(!isScanning())
		{	
			bluetooth.startDiscovery();
			Log.d(LOG_NAME, "Bluetooth Scan Started (" + bluetooth.getAddress());
		}
	}

	private void stopScan() 
	{
		if (isScanning())
		{
			bluetooth.cancelDiscovery();
			Log.d(LOG_NAME, "Bluetooth Scan Stopped");
		}
	}
	
	public boolean isScanning()
	{
		return bluetooth.isDiscovering();
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
	
	public String[] getNearbyDevices(Date since)
	{
		ArrayList<String> result = new ArrayList<String>();
		
		for (String key : deviceLog.keySet())
		{
			if (key != null && deviceLog.get(key).getTime() >= since.getTime())
			{				
				String deviceName = getDeviceName(key);
				
				if (!result.contains(deviceName))
				{
					result.add(deviceName);
				}
			}
		}
		
		// Converts Result to an Array of Strings
		return result.toArray(new String[0]);
	}

	public short getRSSI(String deviceName)
	{
		if (rssiLog.containsKey(deviceName))
		{
			return rssiLog.get(deviceName);
		}
		else
		{
			return 0;
		}
	}
	
	private void cleanLog()
	{
    	// Erases Old Entries
    	for (String deviceID : new ArrayList<String>(deviceLog.keySet()))
        {
        	Date lastContact = deviceLog.get(deviceID);
        	
        	if (new Date().getTime() - lastContact.getTime() > MAX_TIME_IN_MEMORY)
        	{
        		deviceLog.remove(deviceID);
        	}
        } 
    	
        Log.d(LOG_NAME, "Log Cleanup Complete (" + deviceLog.size() + " entries remaining)");	
	}
	
	/**
	 * Determines if a Bluetooth Name is Bluewave Compatible
	 * @param bluetoothName
	 * @return
	 */
	public static boolean isBluewaveName(String bluetoothName)
	{
		if (bluetoothName != null)
		{
			String[] nameComponents = bluetoothName.split("::");
			
			return nameComponents.length >= 2 && nameComponents[0].equals("BLU");	
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Returns a Device's Name (compensates for Bluewave Formatting)
	 * @param bluetoothName
	 * @return
	 */
	public static String getDeviceName(String bluetoothName)
	{
		if (isBluewaveName(bluetoothName))
		{
			String[] nameComponents = bluetoothName.split("::");
			return nameComponents[1];
		}
		else
		{
			return bluetoothName;
		}
	}
	
	/**
	 * Performs a Periodic Scan Using Bluetooth
	 * @author adefreit
	 */
	private class ScanThread extends Thread
	{
		@Override
		public void run()
		{
			while (true)
			{
				try
				{
					Log.i(LOG_NAME, "Found Devices? " + foundDevices);
					
					if (!foundDevices)
					{
						bluetooth.startDiscovery();
						bluetooth.cancelDiscovery();
					}
					
					// Resets the Flag
					foundDevices = false;
					
					sleep(SCAN_DURATION);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		}
	}
	
	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() 
	{
	    public void onReceive(Context context, Intent intent) 
	    {
	        String action = intent.getAction();
	        
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) 
	        {
	        	foundDevices = true;
	        	
	            // Get the BluetoothDevice object from the Intent
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            short 			rssi   = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short)0);
	            
	            // Processes NON-NULL Bluetooth Device Names
	            if (device.getName() != null)
	            {
		            // Logs the Discovery
		            Log.i(LOG_NAME, "Found " + device.getName() + ": (RSSI=" + rssi + ")");
	            	
	            	// Adds the Device Name to the Log
		            deviceLog.put(device.getName(), new Date());
		            
		            // Adds the RSSI Value to the Log
		            // NOTE:  Removes Bluewave Modifiers
		            rssiLog.put(getDeviceName(device.getName()), rssi);

		            // Sends an Intent Containing the Results
		    		Intent updateResults = new Intent(BluewaveManager.ACTION_BLUETOOTH_SCAN_UPDATE);
		    		updateResults.putExtra(BluewaveManager.BLUETOOTH_SCAN_RESULTS, deviceLog.keySet().toArray(new String[0]));
		    		context.sendBroadcast(updateResults);
	            }
	        }
	        else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
	        {	
	        	// Removes All Entries from the Log that are Considered "Old"
	        	cleanLog();
	        	
	        	// Restarts Bluetooth Discovery
	        	start();
	        }
	    }
	};
}
