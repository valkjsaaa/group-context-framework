package template;

import java.util.Date;

import com.adefreitas.desktopframework.DesktopBatteryMonitor;
import com.adefreitas.desktopframework.DesktopGroupContextManager;
import com.adefreitas.desktopframework.MessageProcessor;
import com.adefreitas.desktopframework.RequestProcessor;
import com.adefreitas.groupcontextframework.CommManager;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ContextData;
import com.adefreitas.messages.ContextRequest;

public class GCFController implements MessageProcessor, RequestProcessor
{
	// Creates a Unique Computer Name (Needed for GCM to Operate, But You Can Change it to Anything Unique)
	public static final String COMPUTER_NAME = "DESKTOP_APP_" + new Date().getTime();
	
	// GCF Communication Settings (BROADCAST_MODE Assumes a Functional TCP Relay Running)
	public static final CommManager.CommMode COMM_MODE  = CommManager.CommMode.MQTT;
	public static final String 				 IP_ADDRESS = Settings.DEV_MQTT_IP;
	public static final int    				 PORT 	    = Settings.DEV_MQTT_PORT;
	public static final String				 CHANNEL    = "cmu/gcf_framework";
	
	// GCF Variables
	public DesktopGroupContextManager gcm;
	
	public GCFController()
	{
		// Creates the Group Context Manager
		gcm = new DesktopGroupContextManager(COMPUTER_NAME, new DesktopBatteryMonitor(), false);
		gcm.registerOnMessageProcessor(this);
		gcm.registerOnRequestProcessor(this);
		gcm.setDebug(false);
		
		// Connects to Channel
		String connectionKey = gcm.connect(COMM_MODE, IP_ADDRESS, PORT);
		
		// Requests Context
		gcm.sendRequest("BLUEWAVE", ContextRequest.MULTIPLE_SOURCE, new String[0], 300000, new String[0]);
	}

	public void onBluetoothData(ContextData data)
	{
		String deviceName = data.getPayload("FOUND");
		String scanStartTime = data.getPayload("SCAN_START");
	}
	
	@Override
	public void onMessage(CommMessage message) 
	{
		if (message instanceof ContextData)
		{	
			ContextData data = (ContextData)message;
		
			System.out.println(new Date().toString() + "\nReceived Data [" + data.getContextType() + "]: " + data.getDeviceID() + "\n" + data.toString());
			
			if (data.getContextType().equals("BLU"))
			{
				onBluetoothData(data);
			}
			
			System.out.println();
		}
	}

	@Override
	public void onSendingRequest(ContextRequest request) 
	{
		System.out.println("Sending Request: " + request.toString());
	}
}
