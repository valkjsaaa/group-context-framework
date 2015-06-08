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
		gcm.registerEventReceiver(this);

		// Sets the Debug Mode Flags (Default is False)
		gcm.setDebugMode(true);
		
		// Connects to Channel
		String connectionKey = gcm.connect(COMM_MODE, IP_ADDRESS, PORT);
		gcm.subscribe(connectionKey, "TEST_CHANNEL");
		
		// Requests Context
		gcm.sendRequest("PCP", ContextRequest.SINGLE_SOURCE, new String[0], 60000, new String[] { "CHANNEL=TEST_CHANNEL" });
		//gcm.sendRequest("BLU", ContextRequest.SINGLE_SOURCE, new String[] { "Nexus 5-A" }, 60000, new String[] { "CHANNEL=TEST_CHANNEL" });
	}
	
	/**
	 * This Method is Called Whenever the GCM Receives Data
	 */
	@Override
	public void onContextData(ContextData data) 
	{
		System.out.println(new Date().toString() + "\nReceived Data [" + data.getContextType() + "]: " + data.getDeviceID() + "\n" + data.toString());
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
