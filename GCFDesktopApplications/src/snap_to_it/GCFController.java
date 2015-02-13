package snap_to_it;

import com.adefreitas.desktopframework.DesktopBatteryMonitor;
import com.adefreitas.desktopframework.DesktopGroupContextManager;
import com.adefreitas.desktopframework.MessageProcessor;
import com.adefreitas.desktopframework.RequestProcessor;
import com.adefreitas.groupcontextframework.CommManager;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ContextRequest;

public class GCFController implements MessageProcessor, RequestProcessor
{
	// Creates a Unique Computer Name (Needed for GCM to Operate, But You Can Change it to Anything Unique)
	public static String COMPUTER_NAME = "";
	
	// GCF Communication Settings (BROADCAST_MODE Assumes a Functional TCP Relay Running)
	public static final CommManager.CommMode COMM_MODE  = CommManager.CommMode.MQTT;
	public static final String 				 IP_ADDRESS = Settings.DEV_MQTT_IP;
	public static final int    				 PORT 	    = Settings.DEV_MQTT_PORT;
	
	// GCF Variables
	private DesktopBatteryMonitor      batteryMonitor;
	private DesktopGroupContextManager gcm;
		
	// Context Providers
	private RemoteControlProvider remoteProvider;
	
	/**
	 * Constructor
	 * Initializes the Current Application
	 */
	public GCFController(String computerName)
	{			
		COMPUTER_NAME = computerName;
		
		// Sets Up GCF
		batteryMonitor = new DesktopBatteryMonitor();
		gcm 		   = new DesktopGroupContextManager(COMPUTER_NAME, batteryMonitor, false);
		gcm.registerOnMessageProcessor(this);
		gcm.registerOnRequestProcessor(this);
		
		String connectionKey = gcm.connect(COMM_MODE, IP_ADDRESS, PORT);
		gcm.subscribe(connectionKey, "cmu/snaptoit");
		
		// Creates the Remote Control Provider
		if (computerName.equals("RCP_SOUND"))
		{
			remoteProvider = new RCP_Sound(gcm);
		}
		else if (computerName.equals("RCP_POWERPOINT"))
		{
			remoteProvider = new RCP_Powerpoint(gcm);
		}
		else if (computerName.equals("RCP_PRINT"))
		{
			remoteProvider = new RCP_Print(gcm);
		}
		else if (computerName.equals("RCP_PAINT"))
		{
			remoteProvider = new RCP_Paint(gcm);
		}
		else if (computerName.equals("RCP_GAME"))
		{
			remoteProvider = new RCP_Game(gcm);
		}
		else if (computerName.equals("RCP_DIAGNOSTICS"))
		{
			remoteProvider = new RCP_Diagnostics(gcm);
		}
		else if (computerName.equals("RCP_EXPERIMENTAL"))
		{
			remoteProvider = new RCP_Experimental(gcm);
		}
		else if (computerName.equals("RCP_LIGHT"))
		{
			remoteProvider = new RCP_Light(gcm);
		}
		
		// Registers the Remote Service Provider
		if (remoteProvider != null)
		{
			gcm.registerContextProvider(remoteProvider);
			System.out.println("GCM Ready [Provider " + remoteProvider.getClass().getName() + "]");	
		}
		else
		{
			System.out.println("Unknown Provider: " + computerName);
		}
	}
	
	@Override
	public void onMessage(CommMessage message) 
	{
		System.out.println("Received Message: " + message.toString());
	}

	@Override
	public void onSendingRequest(ContextRequest request) 
	{
		System.out.println("Sending Request: " + request.toString());
	}
}
