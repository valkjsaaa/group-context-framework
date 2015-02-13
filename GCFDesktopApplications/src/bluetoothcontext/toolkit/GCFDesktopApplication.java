package bluetoothcontext.toolkit;

import java.util.Date;
import java.util.HashMap;

import com.adefreitas.desktopframework.DesktopBatteryMonitor;
import com.adefreitas.desktopframework.DesktopGroupContextManager;
import com.adefreitas.desktopframework.MessageProcessor;
import com.adefreitas.desktopframework.RequestProcessor;
import com.adefreitas.desktoptoolkits.CloudStorageToolkit;
import com.adefreitas.desktoptoolkits.HttpToolkit;
import com.adefreitas.desktoptoolkits.SftpToolkit;
import com.adefreitas.groupcontextframework.CommManager;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ContextData;
import com.adefreitas.messages.ContextRequest;

public abstract class GCFDesktopApplication implements MessageProcessor, RequestProcessor
{	
	// Debug Flag
	private static boolean DEBUG = true;
	
	// GCF Communication Settings (BROADCAST_MODE Assumes a Functional TCP Relay Running)
	public static final CommManager.CommMode COMM_MODE  = CommManager.CommMode.TCP;
	public static final String 				 IP_ADDRESS = Settings.DEV_TCP_IP;
	public static final int    				 PORT 	    = Settings.DEV_TCP_PORT;
	
	// GCF Variables
	private DesktopBatteryMonitor      batteryMonitor;
	private DesktopGroupContextManager gcm;
	
	// Dropbox Settings
	private final static String APP_KEY         = "e0c5st27ef37smy";
	private final static String APP_SECRET      = "ib32717oahl9esn";
	private final static String AUTH_TOKEN 	    = "sPolNy0CQ5IAAAAAAAAAAf2NWw6W4vuT5EXBmzPpi9KZISd9j_a_wKOTu6ZQqzUF";
	private static final String APP_DATA_FOLDER = "appData/iAm/";
	private CloudStorageToolkit cloudToolkit;
	
	// Bluetooth Context Download History
	private HashMap<String, Long>   downloadHistory = new HashMap<String, Long>();	 // KEY = deviceID
	private HashMap<String, String> downloadedJSON  = new HashMap<String, String>(); // KEY = deviceID
	
	/**
	 * Constructor
	 * @param computerName - the GCF Application Name
	 */
	public GCFDesktopApplication(String computerName)
	{
		System.out.println("GCF Application Started: " + computerName);
		
		batteryMonitor = new DesktopBatteryMonitor();
		gcm 		   = new DesktopGroupContextManager(computerName, batteryMonitor, false);
		gcm.registerOnMessageProcessor(this);
		gcm.registerOnRequestProcessor(this);
		gcm.setDebug(true);
		
		// Requests Bluetooth Data
		// This is needed since (to my knowledge) there is no easy way to get Bluetooth data
		gcm.sendRequest("BLU", ContextRequest.MULTIPLE_SOURCE, 15000, new String[] { "realtime=true" });
		
		//cloudToolkit = new DropboxToolkit(APP_KEY, APP_SECRET, AUTH_TOKEN);
		cloudToolkit = new SftpToolkit();
	}
	
	@Override
	public void onMessage(CommMessage message) 
	{		
		if (message instanceof ContextData)
		{
			onContextData((ContextData)message);
		}
	}

	@Override
	public void onSendingRequest(ContextRequest request) 
	{
		// TODO:  Something
	}

	public void setDebug(boolean newValue)
	{
		this.DEBUG = newValue;
		print("GCFDesktopApplication Debug set to " + DEBUG);
	}
	
	public DesktopGroupContextManager getGroupContextManager()
	{
		return gcm;
	}
	
	public CloudStorageToolkit getCloudToolkit()
	{
		return cloudToolkit;
	}
	
	protected void print(String s)
	{
		if (DEBUG)
		{
			System.out.println(s);
			gcm.log("GCFDesktopApp", s);
		}
	}
	
	public String[] getJSON()
	{
		return downloadedJSON.values().toArray(new String[0]);
	}
	
	// METHODS TO OVERLOAD ------------------------------------------------------------
	protected void onContextData(ContextData data)
	{
		print(new Date() + ":  Received: " + data);
		
		// DESKTOP BLUEWAVE PROTOTYPE CODE
		if (data.getContextType().equals("BLU"))
		{						
			for (String bluetoothID : data.getValues())
			{	
				if (bluetoothID != null)
				{
					// Extracts the Components
					// Should be in the form GCF::<NAME>::<DATA SOURCE>::<FILE NAME>::<TIMESTAMP>
					String[] components = bluetoothID.split("::");
								
					// Processes Bluetooth Devices with the Above Naming Convention
					if (components.length >= 5 && components[0].equalsIgnoreCase("GCF") && !downloadHistory.containsKey(bluetoothID))
					{
						print("  Found Valid Bluetooth: " + bluetoothID);
						
						// Extracts Fields from the Name
						String deviceID       = components[1];
						String downloadMethod = components[2];
						String downloadPath   = components[3];
						long   lastUpdate     = Long.parseLong(components[4]);
					   	
						// Determines if we need to download a new copy of the file
						// (This occurs if the lastUpdate field is incremented)
						if (!downloadHistory.containsKey(deviceID) || downloadHistory.get(deviceID) < lastUpdate)
						{
							if (downloadMethod.equalsIgnoreCase("sftp"))
						    {			
								print("    Downloading Update: " + downloadPath + " [" + lastUpdate + "]");
								String filename	   = downloadPath.substring(downloadPath.lastIndexOf("/") + 1);
								String destination = APP_DATA_FOLDER + filename;
						    	cloudToolkit.downloadFile(downloadPath, destination);
						    	
						    	JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_FILE, destination);
						    	this.downloadedJSON.put(deviceID, parser.toString());
						    	onContext(parser);
						    }
							else if (downloadMethod.equalsIgnoreCase("http"))
							{
								print("    Downloading Update: " + downloadPath + " [" + lastUpdate + "]");
								String json = HttpToolkit.get(downloadPath.replace(" ", "%20"));		
								
								JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
								this.downloadedJSON.put(deviceID, json);
								onContext(parser);
							}
						    else
						    {
						    	print("    Unknown Download Mechanism: " + downloadMethod);
						    }
							
							// Remembers What Happened to Prevent Duplicate Work
					    	downloadHistory.put(deviceID, lastUpdate);
						}
						else
						{
							//print("  Already Up to Date");
						}
					}	
				}
			}
		}
	}
	
	protected abstract void onContext(JSONContextParser parser);
}
