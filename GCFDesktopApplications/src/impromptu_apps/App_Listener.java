package impromptu_apps;

import java.util.Date;
import java.util.HashMap;

import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.desktopframework.toolkit.SQLToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_Listener extends DesktopApplicationProvider
{
	private String 		lastOutput;
	private int 	 	entriesRecorded;
	private Date		dateStarted;
		
	private SQLToolkit sqlToolkit;
	
	// Tracks Devices Listened To
	private HashMap<String, Integer> log = new HashMap<String, Integer>();
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 * @param sqlToolkit
	 */
	public App_Listener(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port, SQLToolkit sqlToolkit)
	{
		super(groupContextManager, 
				"LISTEN",
				"Listener Application",
				"This app records all context advertised by Impromptu.",
				"DEV TOOLS",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"https://cdn1.iconfinder.com/data/icons/MetroStation-PNG/128/MB__listen.png", // LOGO
				600,
				commMode,
				ipAddress,
				port);
		
		// Initializes Objects
		this.sqlToolkit = sqlToolkit;
		dateStarted 	= new Date();
		entriesRecorded	= 0;
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String ui = "<html>";
		ui       += "<title>Listener App</title>";
		ui       += "<p><b>Started:</b> " + dateStarted + "</p>";
		ui       += "<p><b>Current Time:</b> " + new Date() + "</p>";
		ui		 += "<p><b># Records:</b> " + entriesRecorded + "</p>";
		
		for (String deviceID : log.keySet())
		{
			ui += "<p>" + deviceID + ": " + log.get(deviceID) + "</p>";
		}
				
		ui		 += "<p><b>Last Entry:</b><br/>" + lastOutput + "</p>";
		ui       += "</html>";
		
		return new String[] { "UI=" + ui };
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	public void recordContext(JSONContextParser parser)
	{	
		try
		{
			String deviceID   = this.getDeviceID(parser);
			String time       = new java.sql.Timestamp(System.currentTimeMillis()).toString();			
			double latitude   = this.getLatitude(parser);
			double longitude  = this.getLongitude(parser);
			String activity   = this.getActivity(parser);
			int    confidence = this.getConfidence(parser);
			int    numEntries = (log.containsKey(deviceID)) ? log.get(deviceID) : 0;
			
			// Increments the Counter
			log.put(deviceID, numEntries + 1);
			
			// Generates the Query
			String updateQuery = String.format("INSERT INTO usercontext (deviceID, timestamp, latitude, longitude, activity, confidence) VALUES ('%s','%s',%f,%f,'%s',%d);",
					deviceID, time, latitude, longitude, activity, confidence);
			
			//System.out.println(updateQuery);
			
			// Runs the SQL Query
			sqlToolkit.runUpdateQuery(updateQuery);
					
			// Increments the Counter
			entriesRecorded++;
			
			// Remembers the Last Thing Output
			lastOutput = updateQuery;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			
			System.out.println(" *** Problem Recording Context: " + parser.toString() + " ***");
		}
	}
	
	@Override
	public boolean sendAppData(String json)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		
		// Records Context in DB
		recordContext(parser);
		
		return this.hasEmailAddress(parser, "adrian.defreitas@gmail.com");
	}

	public String getDescription(String json)
	{
		return entriesRecorded + " entries obtained from " + log.keySet().size() + " devices.  Click for more details.";
	}
	
}
