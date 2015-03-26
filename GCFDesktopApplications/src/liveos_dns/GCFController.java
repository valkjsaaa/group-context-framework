package liveos_dns;

import com.adefreitas.desktopframework.DesktopBatteryMonitor;
import com.adefreitas.desktopframework.DesktopGroupContextManager;
import com.adefreitas.desktopframework.MessageProcessor;
import com.adefreitas.desktopframework.RequestProcessor;
import com.adefreitas.groupcontextframework.CommManager;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.liveos.ApplicationSettings;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ContextRequest;

public class GCFController implements MessageProcessor, RequestProcessor
{
	// Creates a Unique Computer Name (Needed for GCM to Operate, But You Can Change it to Anything Unique)
	public String COMPUTER_NAME = "LOS_DNS";
	
	// GCF Communication Settings (BROADCAST_MODE Assumes a Functional TCP Relay Running)
	public static final CommManager.CommMode COMM_MODE   = CommMode.MQTT;
	public static final String 				 IP_ADDRESS  = Settings.DEV_MQTT_IP;
	public static final int    				 PORT 	     = Settings.DEV_MQTT_PORT;
	
	// GCF Variables
	public DesktopBatteryMonitor      batteryMonitor;
	public DesktopGroupContextManager gcm;

	/**
	 * Constructor
	 * @param appName
	 * @param useBluetooth
	 */
	public GCFController()
	{
		System.out.println("Starting Impromptu Directory Service");
		
		batteryMonitor = new DesktopBatteryMonitor();
		gcm 		   = new DesktopGroupContextManager(COMPUTER_NAME, batteryMonitor, false);
		
		// Manages Debugging
		gcm.setDebug(false);
		
		// Registers Objects to Handle Requests and Messages
		gcm.registerOnMessageProcessor(this);
		gcm.registerOnRequestProcessor(this);
		
		// Creates Context Providers
		gcm.registerContextProvider(new DNSProvider(gcm));
		
		// Subscribes to a Communications Channel
		String connectionKey = gcm.connect(COMM_MODE, IP_ADDRESS, PORT);
		gcm.subscribe(connectionKey, ApplicationSettings.DNS_APP_CHANNEL);
		gcm.subscribe(connectionKey, ApplicationSettings.DNS_CHANNEL);
		
		System.out.println("READY FOR ACTION!\n");
	}
	
	@Override
	public void onMessage(CommMessage message) 
	{

	}

	@Override
	public void onSendingRequest(ContextRequest request) 
	{
		System.out.println(COMPUTER_NAME + ": " + "Sending Request: " + request.toString());
	}
}
