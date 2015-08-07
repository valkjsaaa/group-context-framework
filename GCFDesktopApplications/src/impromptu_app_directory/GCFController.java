package impromptu_app_directory;

import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.CommThread;
import com.adefreitas.gcf.Settings;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.DesktopGroupContextManager;
import com.adefreitas.gcf.impromptu.ApplicationSettings;

public class GCFController
{
	// Creates a Unique Computer Name (Needed for GCM to Operate, But You Can Change it to Anything Unique)
	public String COMPUTER_NAME = "LOS_DNS";
	
	// GCF Communication Settings (BROADCAST_MODE Assumes a Functional TCP Relay Running)
	public static final CommManager.CommMode COMM_MODE   = CommMode.MQTT;
	public static final String 				 IP_ADDRESS  = Settings.DEV_MQTT_IP;
	public static final int    				 PORT 	     = Settings.DEV_MQTT_PORT;
	
	// GCF Variables
	public DesktopGroupContextManager gcm;
	public String 					  connectionKey;
	
	/**
	 * Constructor
	 * @param appName
	 * @param useBluetooth
	 */
	public GCFController()
	{
		System.out.println("Starting Impromptu Directory Service");
		gcm = new DesktopGroupContextManager(COMPUTER_NAME, false);
		
		// Manages Debugging
		gcm.setDebugMode(false);
				
		// Subscribes to a Communications Channel
		connectionKey = gcm.connect(COMM_MODE, IP_ADDRESS, PORT);
		gcm.subscribe(connectionKey, ApplicationSettings.DNS_APP_CHANNEL);
		gcm.subscribe(connectionKey, ApplicationSettings.DNS_CHANNEL);
		
		try
		{
			Thread.sleep(1000);
			gcm.unsubscribe(connectionKey, CommThread.PUBLIC_CHANNEL);
		}
		catch (Exception ex)
		{
			
		}
		
		// Creates Context Providers
		gcm.registerContextProvider(new DNSProvider(gcm, connectionKey));
		
		System.out.println("READY FOR ACTION!\n");
	}
}
