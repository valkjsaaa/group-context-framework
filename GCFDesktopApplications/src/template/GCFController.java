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
		gcm.setDebug(true);
		
		// Connects to Channel
		String connectionKey = gcm.connect(COMM_MODE, IP_ADDRESS, PORT);
		gcm.subscribe(connectionKey, CHANNEL);
		
		// Requests Context
		//gcm.sendRequest("ACC", ContextRequest.MULTIPLE_SOURCE, 1000, new String[0]);
		//gcm.sendRequest("LOC", ContextRequest.MULTIPLE_SOURCE, 5000, new String[0]);
		gcm.sendRequest("BLU", ContextRequest.MULTIPLE_SOURCE, 15000, new String[] { "HITLIST=Madhur’s MacBook Pro" });
	}

	public void onBluetoothData(ContextData data)
	{
		String deviceName = CommMessage.getValue(data.getValues(), "FOUND");
		String scanStartTime = CommMessage.getValue(data.getValues(), "SCAN_START");
	}
	
	@Override
	public void onMessage(CommMessage message) 
	{
		System.out.println("Received Message: " + message.toString());
		
		if (message instanceof ContextData)
		{
			ContextData data = (ContextData)message;
			
			if (data.getDescription().equals("BLU"))
			{
				onBluetoothData(data);
			}
		}
	}

	@Override
	public void onSendingRequest(ContextRequest request) 
	{
		System.out.println("Sending Request: " + request.toString());
	}
}
