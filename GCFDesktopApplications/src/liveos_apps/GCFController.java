package liveos_apps;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import bluetoothcontext.toolkit.JSONContextParser;

import com.adefreitas.desktopframework.DesktopBatteryMonitor;
import com.adefreitas.desktopframework.DesktopGroupContextManager;
import com.adefreitas.desktopframework.MessageProcessor;
import com.adefreitas.desktopframework.RequestProcessor;
import com.adefreitas.groupcontextframework.CommManager;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextType;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.liveos.ApplicationSettings;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ContextData;
import com.adefreitas.messages.ContextRequest;
import com.google.gson.JsonObject;


public class GCFController implements MessageProcessor, RequestProcessor
{
	// Creates a Unique Computer Name (Needed for GCM to Operate, But You Can Change it to Anything Unique)
	public String COMPUTER_NAME = "";
	
	// GCF Communication Settings (BROADCAST_MODE Assumes a Functional TCP Relay Running)
	public static final CommManager.CommMode COMM_MODE       = CommMode.MQTT;
	public static final String 				 IP_ADDRESS      = Settings.DEV_MQTT_IP;
	public static final int    				 PORT 	         = Settings.DEV_MQTT_PORT;
	
	// GCF Variables
	public DesktopBatteryMonitor      batteryMonitor;
	public DesktopGroupContextManager gcm;
	
	// Context Provider Representing the Application
	public ArrayList<DesktopApplicationProvider> appProviders = new ArrayList<DesktopApplicationProvider>();
	public QueryApplicationProvider			     queryProvider;
	
	private HashMap<String, Date> receivedJSON     = new HashMap<String, Date>();
	private Date				  lastTransmission = new Date(0);
	
	/**
	 * Constructor
	 * @param appName
	 * @param useBluetooth
	 */
	public GCFController(String appName, boolean useBluetooth)
	{
		System.out.println("Starting App: " + appName);
		COMPUTER_NAME += "APP_" + appName + "_" + new Date().getTime();
				
		batteryMonitor = new DesktopBatteryMonitor();
		gcm 		   = new DesktopGroupContextManager(COMPUTER_NAME, batteryMonitor, false);
		
		// Opens the Connection
		String connectionKey = gcm.connect(COMM_MODE, IP_ADDRESS, PORT);
		gcm.subscribe(connectionKey, ApplicationSettings.DNS_APP_CHANNEL);
		
		// Manages Debugging
		gcm.setDebug(false);
		
		// Registers Objects to Handle Requests and Messages
		gcm.registerOnMessageProcessor(this);
		gcm.registerOnRequestProcessor(this);
		
		// Creates the Query Manager
		queryProvider = new QueryApplicationProvider(gcm);
		gcm.registerContextProvider(queryProvider);

		// Snap-To-It Apps
		//appProviders.add(new Sti_Diagnostics(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new Sti_Printer(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new Sti_Map(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new Sti_Game(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new Sti_PowerPoint(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new Sti_DoorPlate(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new Sti_Paint(gcm, COMM_MODE, IP_ADDRESS, PORT));
		
		// Impromptu Apps
		appProviders.add(new App_Diagnostics(gcm, COMM_MODE, IP_ADDRESS, PORT));
		appProviders.add(new App_Bus(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_Target(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_Printer(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_PlayFeedly(gcm, COMM_MODE, IP_ADDRESS, PORT));
		appProviders.add(new App_Away(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_Game_PiratePig(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_Flickr(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_Game_Simon(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_PlayGmail(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_PlayAngryBirds(gcm, COMM_MODE, IP_ADDRESS, PORT));
		
		for (DesktopApplicationProvider app : appProviders)
		{
			gcm.registerContextProvider(app);
			gcm.subscribe(connectionKey, app.getContextType());
			System.out.println("Application [" + app.toString() + "] Ready.");
		}
	}

	public void onPersonalContextData(final String deviceID, final JSONContextParser parser)
	{			
		// Attempts to Extract Connection Information
		JsonObject context   = parser.getJSONObject("magic");
		
		if (context != null)
		{
			final CommMode commMode  = CommMode.valueOf(context.get("COMM_MODE").getAsString());
			final String   ipAddress = context.get("IP_ADDRESS").getAsString();
			final int      port      = context.get("PORT").getAsInt();
			
			// Checks to see if this is a new device
			boolean newEntry = !receivedJSON.containsKey(deviceID);
			
			// Adds the Date to the Log
			receivedJSON.put(deviceID, new Date());
			
			for (DesktopApplicationProvider app : appProviders)
			{
				if (ipAddress != null && app.sendAppData(parser.toString()))
				{
					System.out.println(COMPUTER_NAME + ": " + "Sending app advertisement data to " + deviceID);
					gcm.sendComputeInstruction(ContextType.PERSONAL, new String[] { deviceID }, "APPLICATION", app.getInformation().toArray(new String[0]));
				}	
			}
		}
		else
		{
			System.out.println(COMPUTER_NAME + ": " + "ERROR:  JSON Did Not Contain Magic Data for " + deviceID);
		}
	}
	
	@Override
	public void onMessage(CommMessage message) 
	{
		if (message instanceof ContextData)
		{
			ContextData data = (ContextData)message;
			
			System.out.println(COMPUTER_NAME + ": " + "Received " + data.getContextType() + " from " + data.getDeviceID());
			
			if (data.getContextType().equals("PCP"))
			{
				String json = CommMessage.getValue(data.getValues(), "context");
				onPersonalContextData(data.getDeviceID(), new JSONContextParser(JSONContextParser.JSON_TEXT, json));
			}
		}
	}

	@Override
	public void onSendingRequest(ContextRequest request) 
	{
		System.out.println(COMPUTER_NAME + ": " + "Sending Request: " + request.toString());
	}
}
