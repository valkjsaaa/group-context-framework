package misc;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.Settings;
import com.adefreitas.gcf.desktop.DesktopGroupContextManager;
import com.adefreitas.gcf.desktop.EventReceiver;
import com.adefreitas.gcf.desktop.providers.PhillipsHueProvider;
import com.adefreitas.gcf.desktop.toolkit.HttpToolkit;
import com.adefreitas.gcf.messages.ContextData;
import com.adefreitas.gcf.messages.ContextRequest;
import com.google.gson.Gson;

/**
 * This is a simple example of how to use GCF in a desktop application
 * @author adefreit
 *
 */
public class LightControlApplication implements EventReceiver
{
	// Creates a Unique Device ID (Needed for GCM to Operate, But You Can Change it to Anything Unique)
	public String deviceID;
	public String ipAddress = "192.168.137.104";
	
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
	public LightControlApplication(String[] args)
	{
		// Assigns the Desktop Application's Name
		deviceID  = (args.length >= 1) ? args[0] : "DESKTOP_APP_" + (System.currentTimeMillis() % 1000);
		ipAddress = (args.length >= 2) ? args[1] : "192.168.137.192";
		
		// Creates the Group Context Manager
		gcm = new DesktopGroupContextManager(deviceID, false);
		String connectionKey = gcm.connect(COMM_MODE, IP_ADDRESS, PORT);
		gcm.subscribe(connectionKey, "TEST_CHANNEL");

		// GCM Settings
		gcm.registerEventReceiver(this);
		//gcm.setDebugMode(true);
			
		// Registers a Context Provider
		gcm.registerContextProvider(new PhillipsHueProvider(gcm, "http://" + ipAddress + "/api/gcfdeveloper/", new Integer[] { 1, 2, 3 }));
		
		// Requests Context
		//gcm.sendRequest("PCP", ContextRequest.SINGLE_SOURCE, new String[] { "Nexus 5-A" }, 60000, new String[] { "CHANNEL=TEST_CHANNEL" });
		//gcm.sendRequest("ACT", ContextRequest.MULTIPLE_SOURCE, new String[] { }, 120000, new String[] { "CHANNEL=dev/" + deviceID});
		//gcm.sendRequest("LOC_GPS", ContextRequest.MULTIPLE_SOURCE, new String[] { }, 30000, new String[] { "CHANNEL=TEST_CHANNEL"});
		//gcm.sendRequest("BLU", ContextRequest.MULTIPLE_SOURCE, new String[] { }, 60000, new String[] { "CHANNEL=TEST_CHANNEL", "TARGET=ZTE-3"});
		//gcm.sendRequest("LOC", ContextRequest.MULTIPLE_SOURCE, new String[] { }, 60000, new String[] { "CHANNEL=dev/" + deviceID, "TARGET=ZTE-3"});
		//gcm.sendRequest("AUD", ContextRequest.MULTIPLE_SOURCE, new String[] { }, 60000000, new String[] { "CHANNEL=TEST_CHANNEL" });
		//gcm.sendRequest("COMPASS", ContextRequest.SINGLE_SOURCE, new String[] { "Nexus 5-A", "Device 1" }, 1000, new String[] { "CHANNEL=TEST_CHANNEL" })		
		//gcm.sendRequest("TEMP", ContextRequest.SINGLE_SOURCE, new String[] { "Nexus 5-A", "Device 1" }, 1000, new String[] { "CHANNEL=TEST_CHANNEL" });
		
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
		
		System.out.println(data);
		// write(data);
		
		if (data.getContextType().equals("POSTURE"))
		{
			int    value = Integer.parseInt(data.getPayload("VALUE"));
			double angle = value;
			
			double  xy = 0.5;
			boolean on = true;
			
			if (angle >= 40 && angle <= 60)
			{
				on = false;
				xy = 0.0;
			}
			else if (angle >= 60 && angle <= 120)
			{
				xy = (angle-60)/60.0;
			}
			
			setLight(1, on, 0.3, xy);
			setLight(2, on, 0.25, xy);
			setLight(3, on, 0.35, xy);
		}
	}

	double normalizeAngle(double angle)
	{
	    double newAngle = angle;
	    if (newAngle < 0) 
	    {
	    	newAngle += 360;
	    }
	    else if (newAngle > 360) 
	    {
	    	newAngle -= 360;
	    }
	    
//	    System.out.println(" Normalizing: " + angle + " -> " + newAngle);
	    
	    return newAngle;
	}
	
	private void setLight(int lightID, boolean on, double x, double y)
	{
		final String UI = "http://" + ipAddress + "/api/gcfdeveloper/lights/%d/state";
		
		String url      = String.format(UI, lightID);
		String json     = String.format("{\"on\":%s, \"xy\":[%f,%f]}", on, x, y);
		System.out.println(json);
		
		HttpToolkit.put(url, json);
	}
	
	//
	private void setupKeypress()
	{
		String  event    = "DEFAULT";
		String  input    = "";
		double y = 0.1;
		
		Scanner keyboard = new Scanner(System.in);
		do
		{
			input 			 = keyboard.nextLine().trim();
			ContextData data = null;
			System.out.println("Doing something!");
			
			setLight(1, true, 0.3, y);
			setLight(2, true, 0.3, y);
			setLight(3, true, 0.3, y);
			
			y += 0.1;
			
			if (y >= 0.9)
			{
				y = 0.1;
			}
		   
			if (input.equalsIgnoreCase("posture"))
			{
				gcm.sendRequest("POSTURE", ContextRequest.SINGLE_SOURCE, new String[] { }, 1000, new String[] { "CHANNEL=TEST_CHANNEL" });
			}
			else
			{
				System.out.println("Sending Command to Hue");
				gcm.sendComputeInstruction("HUE", new String[] { "IMPROMPTU_LIGHTS" }, "LIGHT_RGB", new String[] { "ON=true", "R=0", "G=255", "B=0" });
			}
			
		   
//		   gcm.sendComputeInstruction("IOT_TABLET_LIGHT", new String[] { }, "speak", new String[] { "TEXT=Hello how are you doing?" });
		   
//		   if (input.length() == 0)
//		   {
//			   System.out.println(new Date() + ":\n" + "LOGGING EVENT: " + event);
//			   data = new ContextData(event, gcm.getDeviceID(), new String[] { "TIMESTAMP=" + System.currentTimeMillis() });
//		   }
//		   else if (!input.equalsIgnoreCase("EXIT"))
//		   {
//			   System.out.println("Setting EVENT to '" + input + "'");
//			   event = input;
//			   
//			   data = new ContextData(event, gcm.getDeviceID(), new String[] { "TIMESTAMP=" + System.currentTimeMillis() });
//		   }
//		   
//		   if (data != null)
//		   {
//			   write(data);
//		   }
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
		new LightControlApplication(args);
	}
}
