package impromptu_apps.favors;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.Settings;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.SQLToolkit;
import com.adefreitas.gcf.messages.ContextRequest;

public class FavorDispatcher 
{
	// SQL Database
	private SQLToolkit toolkit;
	
	// Connection Settings
	public static final CommManager.CommMode COMM_MODE  = CommMode.MQTT;
	public static final String 				 IP_ADDRESS = Settings.DEV_MQTT_IP;
	public static final int    				 PORT 	    = Settings.DEV_MQTT_PORT;
	
	// GCF
	private GroupContextManager gcm;
	private String 				connectionKey;
	
	// Record of all Active Tasks
	private HashMap<String, App_Favor> favors = new HashMap<String, App_Favor>();
	
	/**
	 * Constructor
	 * @param toolkit
	 * @param gcm
	 */
	public FavorDispatcher(SQLToolkit toolkit, GroupContextManager gcm)
	{
		this.toolkit 	   = toolkit;
		this.gcm 	 	   = gcm;
		this.connectionKey = this.gcm.getConnectionKey(COMM_MODE, IP_ADDRESS, PORT);
		
		gcm.sendRequest("BLUEWAVE", ContextRequest.MULTIPLE_SOURCE, 60000, new String[] { "CHANNEL=dev/" + gcm.getDeviceID() });
	}
	
	/**
	 * Adds a Task to the Queue
	 * @param id
	 * @param resultSet
	 */
	public void createOrUpdateTask(String id, ResultSet resultSet)
	{		
		try
		{
			int    timestamp   = resultSet.getInt("timestamp");
			String deviceID	   = resultSet.getString("device_id");
			String userName    = resultSet.getString("userName");
			String desc 	   = resultSet.getString("desc");
			String desc_perf   = resultSet.getString("desc_performance_location");
			String desc_turnin = resultSet.getString("desc_turnin_location");
			double latitude    = resultSet.getDouble("latitude");
			double longitude   = resultSet.getDouble("longitude");
			String[] tags	   = resultSet.getString("tags").split(",");
			String[] sensors   = resultSet.getString("sensors").split(",");
			String[] matches   = (resultSet.getString("matches") != null) ? resultSet.getString("matches").split(",") : new String[] { "NOTHING" };
			String status	   = resultSet.getString("status");
						
			if (!favors.containsKey(id))
			{		
				App_Favor newFavor = new App_Favor(id, this, timestamp, deviceID, userName, desc, desc_perf, desc_turnin, latitude, longitude, tags, sensors, status, matches, gcm, COMM_MODE, IP_ADDRESS, PORT, toolkit);
				newFavor.setSQLEventLogger(toolkit);
				
				// Adds a Task (Assuming it has not already been created before)
				ResultSet result = toolkit.runQuery("SELECT * FROM impromptu_eventLog WHERE app='" + newFavor.getAppID() + "' && tag='FAVOR_CREATED'");
				if (!result.next())
				{
					// This query attempts to get information about the user
					ResultSet creatorInfo = toolkit.runQuery("SELECT * FROM favors_profile WHERE device_id='" + deviceID + "'");
					
					if (creatorInfo.next())
					{
						String sensor   = creatorInfo.getString("last_sensor") != null ? creatorInfo.getString("last_sensor") : "";
						String contents = String.format("favorID=%s,timestamp=%d,submissionType=request,creatorDeviceID=%s,creatorLat=%f,creatorLon=%f,creatorSensor=%s", 
								newFavor.getAppID(), timestamp, deviceID, creatorInfo.getDouble("latitude"), creatorInfo.getDouble("longitude"), sensor);
						newFavor.log("FAVOR_CREATED", contents);
					}
				}
				
				gcm.registerContextProvider(newFavor);
				favors.put(id, newFavor);
				gcm.subscribe(connectionKey, newFavor.getChannel());
			}
			else
			{
				favors.get(id).update(deviceID, userName, desc, desc_perf, desc_turnin, latitude, longitude, tags, sensors, status, matches);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Removes a Task from the Queue
	 * @param id
	 */
	public void removeTask(String id)
	{		
		if (gcm.getContextProvider(id) != null)
		{
			System.out.println("  Removing Task: " + id);
			gcm.unregisterContextProvider(id);
			favors.remove(id);
		}
	}

	/**
	 * Marks a Task as Being Complete
	 * @param id
	 */
	public void markComplete(int timestamp, boolean completed)
	{
		// Query 1:  Update Favor in Database
		String    newStatus = (completed) ? "completed" : "canceled";
		String    query     = "UPDATE favors_submitted SET status = \"" + newStatus + "\" WHERE timestamp=" + timestamp;
		toolkit.runUpdateQuery(query);		
	}
	
	/**
	 * Processes Database Contents to Generate New Favor Apps
	 */
	public void run()
	{
		try
		{
			System.out.println("Favor Dispatcher Running . . . ");
			
			String    query  = "SELECT favors_submitted.timestamp, " +
									"favors_submitted.device_id, " +
									"favors_submitted.desc, " +
									"favors_submitted.desc_performance_location, " +
									"favors_submitted.desc_turnin_location, " +
									"favors_submitted.latitude, " +
									"favors_submitted.longitude, " +
									"favors_submitted.tags, " +
									"favors_submitted.sensors, " +
									"favors_submitted.status, " +
									"favors_submitted.matches, " +
									"favors_profile.username, " +
									"favors_profile.telephone " +
							   "FROM favors_submitted INNER JOIN favors_profile on favors_submitted.device_id = favors_profile.device_id " +
							   "WHERE favors_submitted.status = \"active\" && CHAR_LENGTH(favors_submitted.tags)>0 && favors_submitted.destination_time>" + (System.currentTimeMillis()/1000-7200);
			System.out.println(query);
			ResultSet result = toolkit.runQuery(query);
			
			ArrayList<String> favorsUpdated = new ArrayList<String>();
			
			while (result.next())
			{
				String id = "FAVOR_" + result.getInt("timestamp");;
				
				// Keeps a Log of Apps that are Considered ACTIVE
				favorsUpdated.add(id);
				
				// Creates or Updates a Task
				createOrUpdateTask(id, result);
			}
			
			// Removes all Inactive Favors
			for (String generatedTaskID : favors.keySet().toArray(new String[0]))
			{
				if (!favorsUpdated.contains(generatedTaskID))
				{
					removeTask(generatedTaskID);
				}
			}
			
			System.out.println("  Favor Dispatcher DONE!");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
