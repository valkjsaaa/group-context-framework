package impromptu_apps.favors;

import impromptu_apps.DesktopApplicationProvider;

import java.util.Date;
import java.util.HashMap;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.desktop.toolkit.SQLToolkit;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_FavorListener extends DesktopApplicationProvider
{
	private int  entriesRecorded;
	private Date dateStarted;
		
	private SQLToolkit sqlToolkit;
	
	// Tracks Devices Listened To
	private HashMap<String, ListenInfo> log = new HashMap<String, ListenInfo>();
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 * @param sqlToolkit
	 */
	public App_FavorListener(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port, SQLToolkit sqlToolkit)
	{
		super(groupContextManager, 
				"LISTEN",
				"Context Listener Application",
				"This app records all context advertised by Impromptu and reports it to the favor banking system.",
				"IMPROMPTU",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"https://cdn1.iconfinder.com/data/icons/MetroStation-PNG/128/MB__listen.png", // LOGO
				3600,
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
			ListenInfo li = log.get(deviceID);
			ui += "<p>" + deviceID + ": " + li.getCount() + " (Updated: " + li.getLastUpdate().toString() + ")</p>";
		}
		
		ui += "</html>";
		
		return new String[] { "UI=" + ui };
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	@Override
	public boolean sendAppData(String json)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		
		// Records Context in DB
		recordContext(parser);
		
		return this.hasEmailAddress(parser, "adrian.defreitas@gmail.com");
	}

	// Custom Methods --------------------------------------------------------------------------------------------------------
	public void recordContext(JSONContextParser parser)
	{	
		try
		{
			String deviceID   = this.getDeviceID(parser);
			String time       = new java.sql.Timestamp(System.currentTimeMillis()).toString();			
			double latitude   = this.getLatitude(parser);
			double longitude  = this.getLongitude(parser);
			String activity   = this.getActivity(parser);
			int    confidence = this.getActivityConfidence(parser);
			String source     = (parser.getJSONObject("location") != null && parser.getJSONObject("location").has("SENSOR")) ? parser.getJSONObject("location").get("SENSOR").getAsString() : "APP_DIRECTORY";
			
			// Increments the Counter
			ListenInfo li = (log.containsKey(deviceID)) ? log.get(deviceID) : new ListenInfo();
			li.update(parser);
			log.put(deviceID, li);
			
			// Generates the History Query
			String historyQuery = 
					String.format("INSERT INTO impromptu_usercontext (device_id, timestamp, latitude, longitude, activity, confidence,source) VALUES ('%s','%s',%f,%f,'%s',%d,'%s');",
					deviceID, time, latitude, longitude, activity, confidence, source);
						
			// Generates the Real Time Query
			String realtimeQuery = (parser.getJSONObject("location") != null && parser.getJSONObject("location").has("SENSOR")) ?
					// This query occurs if there is a sensor
					String.format("INSERT INTO favors_profile (device_id, latitude, longitude, last_sensor, last_sensor_date, activity, confidence, last_update) VALUES ('%s',%f,%f,'%s',%d,'%s',%f,%d) ON DUPLICATE KEY UPDATE latitude=VALUES(latitude), longitude=VALUES(longitude), last_sensor=VALUES(last_sensor), last_sensor_date=VALUES(last_sensor_date), activity=VALUES(activity), confidence=VALUES(confidence), last_update=VALUES(last_update)", 
							deviceID, latitude, longitude, parser.getJSONObject("location").get("SENSOR").getAsString(), System.currentTimeMillis() / 1000, activity, (double)confidence, System.currentTimeMillis() / 1000) :
					// This query occurs if there is no sensor
					String.format("INSERT INTO favors_profile (device_id, latitude, longitude, activity, confidence, last_update) VALUES ('%s',%f,%f,'%s',%f,%d) ON DUPLICATE KEY UPDATE latitude=VALUES(latitude), longitude=VALUES(longitude), activity=VALUES(activity), confidence=VALUES(confidence), last_update=VALUES(last_update)", 
							deviceID, latitude, longitude, activity, (double)confidence, System.currentTimeMillis() / 1000);
					
			//System.out.println(realtimeQuery);
					
			// Runs the SQL Queries
			sqlToolkit.runUpdateQuery(historyQuery);
			sqlToolkit.runUpdateQuery(realtimeQuery);
			
			// Increments the Counter
			entriesRecorded++;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			
			System.out.println(" *** Problem Recording Context: " + parser.toString() + " ***");
		}
	}
	
	public String getDescription(String json)
	{
		return entriesRecorded + " entries obtained from " + log.keySet().size() + " devices.  Click for more details.";
	}

	private class ListenInfo
	{
		public int  			 count;
		public Date 			 lastUpdate;
		public JSONContextParser parser;
	
		public ListenInfo()
		{
			count 	   = 0;
			lastUpdate = new Date();
		}
		
		public int getCount()
		{
			return count;
		}
		
		public Date getLastUpdate()
		{
			return lastUpdate;
		}
		
		public JSONContextParser getParser()
		{
			return parser;
		}
		
		public void update(JSONContextParser parser)
		{
			count++;
			lastUpdate  = new Date();
			this.parser = parser;
		}
	}
}
