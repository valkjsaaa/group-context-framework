package template;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

import com.adefreitas.desktopframework.DesktopBatteryMonitor;
import com.adefreitas.desktopframework.DesktopGroupContextManager;
import com.adefreitas.desktopframework.EventReceiver;
import com.adefreitas.groupcontextframework.CommManager;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.messages.ContextData;
import com.adefreitas.messages.ContextRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * This is a simple example of how to use GCF in a desktop application
 * @author adefreit
 *
 */
public class GCFDesktopApplication implements EventReceiver
{
	// Creates a Unique Device ID (Needed for GCM to Operate, But You Can Change it to Anything Unique)
	public String deviceID;
	
	// GCF Communication Settings
	public static final CommManager.CommMode COMM_MODE  = CommManager.CommMode.MQTT;
	public static final String 				 IP_ADDRESS = Settings.DEV_MQTT_IP;
	public static final int    				 PORT 	    = Settings.DEV_MQTT_PORT;
	
	// This Stores all of the Unique Devices Seen Thus Far
	public ArrayList<String> uniqueDevices = new ArrayList<String>();
	
	// This Stores all of the Print Writers (One Per Context Encountered!)
	public HashMap<String, PrintWriter> writers = new HashMap<String, PrintWriter>();
	
	// GCF Variables
	public DesktopGroupContextManager gcm;
	
	// Gson
	public Gson gson = new Gson();
	
	/**
	 * Constructor:  Initializes the GCM
	 */
	public GCFDesktopApplication(String[] args)
	{
		// Assigns the Desktop Application's Name
		deviceID = (args.length >= 1) ? args[0] : "DESKTOP_APP_" + (System.currentTimeMillis() % 1000);
		
		// Creates the Group Context Manager
		gcm = new DesktopGroupContextManager(deviceID, true);
		String connectionKey = gcm.connect(COMM_MODE, IP_ADDRESS, PORT);
		gcm.subscribe(connectionKey, "TEST_CHANNEL");

		// GCM Settings
		gcm.registerEventReceiver(this);
		gcm.setDebugMode(true);
			
		// Requests Context
		//gcm.sendRequest("PCP", ContextRequest.SINGLE_SOURCE, new String[0], 60000, new String[] { "CHANNEL=TEST_CHANNEL" });
		//gcm.sendRequest("ACT", ContextRequest.MULTIPLE_SOURCE, new String[] { }, 120000, new String[] { "CHANNEL=dev/" + deviceID});
		//gcm.sendRequest("LOC_GPS", ContextRequest.MULTIPLE_SOURCE, new String[] { }, 30000, new String[] { "CHANNEL=TEST_CHANNEL"});
		//gcm.sendRequest("BLU", ContextRequest.MULTIPLE_SOURCE, new String[] { }, 60000, new String[] { "CHANNEL=TEST_CHANNEL", "TARGET=ZTE-3"});
		//gcm.sendRequest("LOC", ContextRequest.MULTIPLE_SOURCE, new String[] { }, 60000, new String[] { "CHANNEL=dev/" + deviceID, "TARGET=ZTE-3"});
		//gcm.sendRequest("AUD", ContextRequest.MULTIPLE_SOURCE, new String[] { }, 60000000, new String[] { "CHANNEL=TEST_CHANNEL" });
		//gcm.sendRequest("COMPASS", ContextRequest.SINGLE_SOURCE, new String[] { "Nexus 5-A", "Device 1" }, 1000, new String[] { "CHANNEL=TEST_CHANNEL" });
		
		// Sets Up Keyboard Press Monitoring
		setupKeypress();
		
		// Cancels All Existing
		for (ContextRequest r : gcm.getRequests())
		{
			System.out.println("Canceling Request for " + r.getContextType());
			gcm.cancelRequest(r.getContextType());
		}
		
		System.out.println("Program Complete.  You may terminate the program.");
	}
	
	/**
	 * This Method is Called Whenever the GCM Receives Data
	 */
	@Override
	public void onContextData(ContextData data) 
	{
		if (!uniqueDevices.contains(data.getDeviceID()))
		{
			uniqueDevices.add(data.getDeviceID());
		}
		
		write(data);
	}

	//
	private void setupKeypress()
	{
		String  event    = "DEFAULT";
		String  input    = "";
		
		Scanner keyboard = new Scanner(System.in);
		do
		{
		   input 			= keyboard.nextLine().trim();
		   ContextData data = null;
		   
		   if (input.length() == 0)
		   {
			   System.out.println(new Date() + ":\n" + "LOGGING EVENT: " + event);
			   data = new ContextData(event, gcm.getDeviceID(), new String[] { "TIMESTAMP=" + System.currentTimeMillis() });
		   }
		   else if (!input.equalsIgnoreCase("EXIT"))
		   {
			   System.out.println("Setting EVENT to '" + input + "'");
			   event = input;
			   
			   data = new ContextData(event, gcm.getDeviceID(), new String[] { "TIMESTAMP=" + System.currentTimeMillis() });
		   }
		   
		   if (data != null)
		   {
			   write(data);
		   }
		} 
		while (!input.equalsIgnoreCase("EXIT"));
		
		keyboard.close();
	}
	
	// Data Logging Service	
	private void write(ContextData data)
	{
		try 
		{
			// Creates a Writer for Each Context Type Encountered
			if (!writers.containsKey(data.getContextType()))
			{
				writers.put(data.getContextType(), new PrintWriter(new BufferedWriter(new FileWriter(data.getContextType() + ".txt", true))));
			}

			// Saves the Context to a File
			System.out.println(new Date() + ": Saving Data [" + data.getContextType() + "] from " + 
					data.getDeviceID() + " (" + uniqueDevices.size() + " unique)");
			PrintWriter writer = writers.get(data.getContextType());
			writer.println(gson.toJson(new DataEntry(data)));
			writer.flush();
		} 
		catch (IOException e) 
		{
		    e.printStackTrace();
		}
	}
	
	// Helper Class to Log Received Context
	private class DataEntry
	{
		long 		timestamp;
		Date		currentDate;
		ContextData data;
		
		public DataEntry(ContextData data)
		{
			this.timestamp   = System.currentTimeMillis();
			this.currentDate = new Date();
			this.data        = data;
		}
	}
	
	/**
	 * This is the Main Application
	 * @param args
	 */
	public static void main(String[] args) 
	{
		new GCFDesktopApplication(args);
	}
}
