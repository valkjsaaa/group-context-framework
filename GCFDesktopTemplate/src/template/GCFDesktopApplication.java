package template;

import java.util.Date;

import com.adefreitas.desktopframework.DesktopBatteryMonitor;
import com.adefreitas.desktopframework.DesktopGroupContextManager;
import com.adefreitas.desktopframework.EventReceiver;
import com.adefreitas.groupcontextframework.CommManager;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.messages.ContextData;
import com.adefreitas.messages.ContextRequest;

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
	
	// GCF Variables
	public DesktopGroupContextManager gcm;
	
	/**
	 * Constructor:  Initializes the GCM
	 */
	public GCFDesktopApplication(String[] args)
	{
		// Assigns the Desktop Application's Name
		deviceID = (args.length >= 1) ? args[0] : "DESKTOP_APP_" + (System.currentTimeMillis() % 1000);
		
		// Creates the Group Context Manager
		gcm = new DesktopGroupContextManager(deviceID, false);
		String connectionKey = gcm.connect(COMM_MODE, IP_ADDRESS, PORT);

		// GCM Settings
		gcm.registerEventReceiver(this);
		gcm.setDebugMode(false);
		
		try
		{
			System.out.println("Initializing Framework . . .");
			Thread.sleep(5000);
			System.out.println("DONE!");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		// Requests Context
		//gcm.sendRequest("PCP", ContextRequest.SINGLE_SOURCE, new String[0], 60000, new String[] { "CHANNEL=TEST_CHANNEL" });
		gcm.sendRequest("ACT", ContextRequest.MULTIPLE_SOURCE, new String[] { }, 60000, new String[] { "CHANNEL=dev/" + deviceID});
	}
	
	/**
	 * This Method is Called Whenever the GCM Receives Data
	 */
	@Override
	public void onContextData(ContextData data) 
	{
		System.out.println("\nReceived Data [" + data.getContextType() + "] (" +  new Date().toString() + ": " + data.getDeviceID() + "\n" + data.toString() + "\n");
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
