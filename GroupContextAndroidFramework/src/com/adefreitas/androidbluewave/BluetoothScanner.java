package com.adefreitas.androidbluewave;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

/**
 * This class is responsible for performing continuous Bluetooth Discovery
 * @author adefreit
 *
 */
public class BluetoothScanner 
{
	// Constants
	private static final String LOG_NAME 	  	   = "Bluewave-Scan";
	private static final long   MAX_TIME_IN_MEMORY = 60000;	// Max Time for a Device to be Remembered in the Log
	private static final long   MIN_REST_TIME      = 10000;  // Minimum Delay Time Between Scans
	
	// Log of All Bluetooth Devices Found During a Single Discovery Cycle
	private List<String> btDevicesFound;
	
	// Log of All Bluetooth Devices Found Over Multiple Scans (KEY = Bluetooth Name WITHOUT Bluewave Formatting)
	private HashMap<String, BluetoothDeviceInfo> log;
	
	// Android Application Context
	private Context 		 context;
	
	// Android Bluetooth Hardware Adapter
	private BluetoothAdapter bluetooth;
	
	// Internal Tracking Variables
	private int     scanInterval;		// In Milliseconds
	private Date    scanStartDate;		// Date of the latest Bluetooth Scan
	private Boolean scanning;			// Flag
	
	// Thread Used to Restart the Bluetooth Scanner After Discover is Complete
	private RestartThread restartThread;
	
	/**
	 * Constructor
	 * @param context
	 */
	public BluetoothScanner(Context context)
	{		
		// Initializes Variables
		this.context   	   = context;
		this.bluetooth 	   = BluetoothAdapter.getDefaultAdapter();
		this.scanStartDate = new Date();
		this.scanInterval  = 60000;
				
		// Sets Filter for When Bluetooth Devices are Found
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		context.registerReceiver(mReceiver, filter);
		
		// Initializes Data Structures
		btDevicesFound  = new ArrayList<String>();
		log 		  = new HashMap<String, BluetoothDeviceInfo>();
		restartThread = new RestartThread();
	}
	
	public void start(int scanInterval)
	{				
		// Enables Bluetooth on Device if Not Already
		if (!bluetooth.isEnabled())
		{
			bluetooth.enable();
			Log.d(LOG_NAME, "Enabling Bluetooth");
		}
		
		// Cancels the Current Scan (if one is in progress)
		if(isScanning())
		{
			bluetooth.cancelDiscovery();
		}
		
		bluetooth.startDiscovery();
		scanning 	  = true;
		scanStartDate = new Date();
		
		Log.d(LOG_NAME, "Bluetooth Scan Started (" + bluetooth.getAddress() + ")");
	}
	
	public void stop()
	{
		if (isScanning())
		{
			bluetooth.cancelDiscovery();
			Log.d(LOG_NAME, "Bluetooth Scan Stopped");
		}
		
		scanning = false;
	}
		
	public void setDiscoverable(boolean on)
	{
		if (on)
		{
			// Allows Bluetooth Adapter to be in Discoverable mode
			if (bluetooth.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
			{
				Intent discoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				discoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0); // Set to 0 for infinite, Max is 1200 (I think)
				discoverable.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				this.context.startActivity(discoverable);	
				Log.d(LOG_NAME, "Making Device Bluetooth Discoverable");
			}
			else
			{
				Log.d(LOG_NAME, "Could Not Set Discoverable to TRUE.  Bluetooth Scan Mode is " + bluetooth.getScanMode());
			}
		}
		else
		{
			Intent discoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1); // Set to 0 for infinite, Max is 1200 (I think)
			discoverable.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.context.startActivity(discoverable);	
			Log.d(LOG_NAME, "Making Device Bluetooth UN-Discoverable");
		}
	}
	
	public boolean isScanning()
	{
		return bluetooth.isDiscovering();
	}
	
	public void setBluetoothName(String newName)
	{		
		BluetoothAdapter.getDefaultAdapter().setName(newName);
		Log.d(LOG_NAME, "Bluetooth Name Changed to: " + newName);
	}
	
	public String getBluetoothName()
	{
		return BluetoothAdapter.getDefaultAdapter().getName();
	}
	
	public int getScanInterval()
	{
		return scanInterval;
	}
	
	public void setScanInterval(int newIntervalInMilliseconds)
	{
		scanInterval = newIntervalInMilliseconds;
	}
	
	public String[] getNearbyDevices(Date since)
	{
		ArrayList<String> result = new ArrayList<String>();
		
		for (String key : log.keySet())
		{
			if (key != null && log.get(key).getDate().getTime() >= since.getTime())
			{								
				if (!result.contains(key))
				{
					result.add(key);
				}
			}
		}
		
		// Converts Result to an Array of Strings
		return result.toArray(new String[0]);
	}

	public short getRSSI(String deviceName)
	{
		if (log.containsKey(deviceName))
		{
			return log.get(deviceName).getRSSI();
		}
		else
		{
			return 0;
		}
	}
	
	private void cleanLog()
	{
		int count = 0;
		
		// Clears List of Devices Encountered this Scan
		btDevicesFound.clear();
		
    	// Erases Old Entries
    	for (String deviceID : new ArrayList<String>(log.keySet()))
        {
        	Date lastContact = log.get(deviceID).getDate();
        	
        	if (new Date().getTime() - lastContact.getTime() > MAX_TIME_IN_MEMORY)
        	{
        		log.remove(deviceID);
        		count++;
        	}
        } 
    	
        Log.d(LOG_NAME, "Log Cleanup Complete (" + MAX_TIME_IN_MEMORY + "ms lifetime; " + count + " removed; " + log.size() + " remaining)");	
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
	        	onFoundDevice(context, intent);
	        }
	        else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
	        {	
	        	onDiscoveryFinished(context, intent);
	        }
	    }
	    
	    private void onFoundDevice(Context context, Intent intent)
	    {
            // Get the BluetoothDevice object from the Intent
            BluetoothDeviceInfo deviceInfo = new BluetoothDeviceInfo(intent);

            // Processes NON-NULL Bluetooth Device Names
            if (deviceInfo.getDeviceName() != null && !btDevicesFound.contains(deviceInfo.getDeviceName()))
            {
            	btDevicesFound.add(deviceInfo.getDeviceName());
            	
	            // Logs the Discovery
	            Log.i(LOG_NAME, "Found " + deviceInfo.getDeviceName() + ": (RSSI=" + deviceInfo.getRSSI() + ")");
            	
            	// Adds the Device Name to the Log
	            log.put(deviceInfo.getDeviceName(), deviceInfo);

	            // Sends an Intent Containing the Results
	    		Intent updateResults = new Intent(BluewaveManager.ACTION_BLUETOOTH_SCAN_UPDATE);
	    		updateResults.putExtra(BluewaveManager.BLUETOOTH_SCAN_RESULT, deviceInfo.getFullName());
	    		updateResults.putExtra(BluewaveManager.BLUETOOTH_RSSI_RESULT, deviceInfo.getRSSI());
	    		context.sendBroadcast(updateResults);
            }
	    }
	
	    private synchronized void onDiscoveryFinished(Context context, Intent intent)
	    {
	    	Log.i(LOG_NAME, "Discovery Finished");
	    	
	    	// Removes All Entries from the Log that are Considered "Old"
        	cleanLog();
        	
        	if (scanning)
        	{
            	final long timeElapsed = new Date().getTime() - scanStartDate.getTime();
            	final long timeToRest  = scanInterval - timeElapsed;
            	
            	// Creates a Simple Thread that Lets the Device Sleep for a Bit Before Scanning Again
            	restartThread.kill();
            	
            	restartThread = new RestartThread();
            	restartThread.setTimeToRest(timeToRest);
            	restartThread.start();	
        	}
	    }
	};

	/**
	 * This Thread Restarts Discovery after a Set Period of Time
	 * @author adefreit
	 *
	 */
	private class RestartThread extends Thread
	{
		private long    timeToRest;
		private boolean finished;
		
		public RestartThread()
		{
			this.timeToRest = Math.max(timeToRest, MIN_REST_TIME);
			this.finished   = false;
		}
		
		public void setTimeToRest(long timeToRest)
		{
			this.timeToRest = Math.max(timeToRest, MIN_REST_TIME);
		}
		
		public void run()
		{	
			try
			{
				Log.i(LOG_NAME, "Bluetooth Sleeping for " + timeToRest + "ms");
				Thread.sleep(timeToRest);
				
				// Keeps Sleeping Until Bluetooth Isn't Scanning Anymore
				while (isScanning())
				{
					Log.e(LOG_NAME, "Bluetooth still Discovering");
					Thread.sleep(5000);
				}
				
	        	// Restarts Bluetooth Discovery
	        	BluetoothScanner.this.start(scanInterval);
			}
			catch (Exception ex)
			{
				Log.e(LOG_NAME, "Restart Thread Exception: " + ex.getMessage());
			}
			
			Log.i(LOG_NAME, "Bluetooth Restart Thread Terminated");
			this.finished = true;
		}
	
		public void kill()
		{
			if (!finished)
			{
				this.interrupt();
			}
		}
	}
	
	/**
	 * This class contains information about a Bluetooth Device
	 * @author adefreit
	 *
	 */
	private class BluetoothDeviceInfo
	{
		private String fullName;
		private Short  rssi;
		private Date   date;
		
		public BluetoothDeviceInfo(Intent discoverIntent)
		{
			BluetoothDevice device = discoverIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            
			fullName = device.getName();
			rssi 	 = discoverIntent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short)0);
			date 	 = new Date();
		}
		
		public String getFullName()
		{
			return fullName;
		}
		
		public String getDeviceName()
		{
			return BluewaveManager.getDeviceName(fullName);
		}
		
		public short getRSSI()
		{
			return rssi;
		}
	
		public Date getDate()
		{
			return date;
		}
	}
}
