package com.adefreitas.androidbluewave;

import java.util.Date;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.androidframework.toolkit.HttpToolkit;
import com.adefreitas.groupcontextframework.GroupContextManager;

public class BluewaveManager
{
	// Log Constant
	private static final String LOG_NAME = "BLUEWAVE";
	
	// JSON Constants
	public static final String DEVICE_TAG = "device";
	public static final String DEVICE_ID  = "deviceID";
	public static final String TIMESTAMP  = "timestamp";
	public static final String CONTEXTS   = "contextproviders";
	public static final String DEVICES	  = "devices";
	
	// Intents (and Extras)
	protected static final String ACTION_USER_CONTEXT_DOWNLOADED 	   = "USER_CONTEXT_DOWNLOADED";
	protected static final String ACTION_OTHER_USER_CONTEXT_DOWNLOADED = "OTHER_USER_CONTEXT_DOWNLOADED";
	public    static final String ACTION_USER_CONTEXT_UPDATED	 	   = "USER_CONTEXT_UPDATED";
	public    static final String ACTION_OTHER_USER_CONTEXT_RECEIVED   = "OTHER_USER_CONTEXT_RECEIVED";
	public    static final String OTHER_USER_CONTEXT		    	   = "OTHER_USER_CONTEXT";
	public 	  static final String ACTION_COMPUTE_INSTRUCTION_RECEIVED  = "PCP_COMPUTE_INSTRUCTION";
	protected static final String OTHER_USER_ID		    			   = "OTHER_USER_ID";
	protected static final String ACTION_BLUETOOTH_SCAN_UPDATE 		   = "BT_UPDATE";
	protected static final String BLUETOOTH_SCAN_RESULTS       		   = "BT_RESULTS";
	protected static final String BLUETOOTH_RSSI_RESULTS       		   = "RSSI_RESULTS";
	
	// Group Context Framework
	private GroupContextManager gcm;
	
	// Application Context
	private Context context;
	
	// HTTP Toolkit
	private HttpToolkit httpToolkit;
	
	// Bluewave Components
	private String					urlToContextFile;
	private BluetoothScanner		bluetoothScanner;
	private PersonalContextProvider pcp;
	private ContextListener			contextListener;			
	
	/**
	 * Constructor
	 * @param context
	 * @param gcm
	 * @param urlToContextFile
	 */
	public BluewaveManager(Context context, GroupContextManager gcm, String urlToContextFile)
	{
		this.context 		  = context;
		this.gcm 	 		  = gcm;
		this.urlToContextFile = urlToContextFile;
		
		// Creates the HTTP Toolkit
		this.httpToolkit = new HttpToolkit((Application)context.getApplicationContext());
		
		// Creates the Bluetooth Scanner
		this.bluetoothScanner = new BluetoothScanner(context);
		
		// Creates the Personal Context Provider
		this.pcp = new PersonalContextProvider(context, this, gcm, httpToolkit, urlToContextFile);
		this.gcm.registerContextProvider(this.pcp);
		
		// Creates the Context Listener
		this.contextListener = new ContextListener(context, httpToolkit);
		
		updateBluetoothName();
	}
	
	/**
	 * Gets the Personal Context Provider Used by Bluewave
	 * @return
	 */
	public PersonalContextProvider getPersonalContextProvider()
	{
		return pcp;
	}
	
	/**
	 * Begins Bluewave Scanning and Analyzing
	 */
	public void startScan()
	{
		this.bluetoothScanner.start();
		
		this.pcp.setContext("Debug", "Test");
	}
	
	/**
	 * Updates the Device's Bluetooth Name
	 */
	public void updateBluetoothName()
	{
		// Creates the Main Bluetooth Name
		String bluetoothName = "BLU::" + gcm.getDeviceID() + "::" + urlToContextFile + "::" + new Date().getTime();
		
		// Updates the Bluetooth Name
		bluetoothScanner.setBluetoothName(bluetoothName);
		Log.d("BLUETOOTH", "Set Name to: " + bluetoothName);
	}
	
	/**
	 * Returns All Nearby Devices Since the Specified Number of Seconds
	 * @param seconds
	 * @return
	 */
	public String[] getNearbyDevices(int seconds)
	{
		Date since = new Date(new Date().getTime() - (seconds * 1000));
		return bluetoothScanner.getNearbyDevices(since);
	}

	/**
	 * Returns the RSSI Value (bigger = closer) of a given Bluetooth ID
	 * Returns 0 if no value is found
	 * @param deviceName
	 * @return
	 */
	public short getRSSI(String deviceName)
	{
		return bluetoothScanner.getRSSI(deviceName);
	}
}
