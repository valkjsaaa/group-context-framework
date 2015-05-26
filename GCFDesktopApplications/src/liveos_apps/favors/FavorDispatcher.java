package liveos_apps.favors;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

import com.adefreitas.desktopframework.toolkit.SQLToolkit;
import com.adefreitas.groupcontextframework.CommManager;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.messages.ContextRequest;

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
	
	// Record of all Active Tasks
	private HashMap<String, App_Favor> tasks = new HashMap<String, App_Favor>();
	
	/**
	 * Constructor
	 * @param toolkit
	 * @param gcm
	 */
	public FavorDispatcher(SQLToolkit toolkit, GroupContextManager gcm)
	{
		this.toolkit = toolkit;
		this.gcm 	 = gcm;
		
		gcm.sendRequest("BLUEWAVE", ContextRequest.MULTIPLE_SOURCE, 300000, new String[0]);
	}
	
	/**
	 * Adds a Task to the Queue
	 * @param id
	 * @param resultSet
	 */
	public void createTask(String id, ResultSet resultSet)
	{		
		try
		{
			int    timestamp   = resultSet.getInt("timestamp");
			String description = resultSet.getString("desc");
			String photo       = resultSet.getString("photo");
			double latitude    = resultSet.getDouble("latitude");
			double longitude   = resultSet.getDouble("longitude");
			String status	   = resultSet.getString("status");
			String[] tags	   = resultSet.getString("tags").split(",");
			
			if (!tasks.containsKey(id))
			{		
				App_Favor newFavor = new App_Favor(id, this, timestamp, description, photo, latitude, longitude, status, tags, gcm, COMM_MODE, IP_ADDRESS, PORT, toolkit);
				gcm.registerContextProvider(newFavor);
				tasks.put(id, newFavor);
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
			tasks.remove(id);
		}
	}

	/**
	 * Marks a Task as Being Complete
	 * @param id
	 */
	public void markComplete(int timestamp)
	{
		String    query  = "UPDATE problems SET status = \"completed\" WHERE timestamp=" + timestamp;
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
			
			String    query  = "SELECT * FROM favors WHERE status = \"active\" && CHAR_LENGTH(tags)>0";
			ResultSet result = toolkit.runQuery(query);
			
			ArrayList<String> tasksUpdated = new ArrayList<String>();
			
			while (result.next())
			{
				Integer timestamp = result.getInt("timestamp");
				String  status    = result.getString("status");
				String  id		  = "FAVOR_" + timestamp;
				
				// Keeps a Log of Apps that are Considered ACTIVE
				tasksUpdated.add(id);
				
				if (!tasks.containsKey(id))
				{
					createTask(id, result);
				}
				else
				{
					// TODO: Updates a Task
					//tasks.get(id).update();
				}
			}
			
			for (String generatedTaskID : tasks.keySet().toArray(new String[0]))
			{
				if (!tasksUpdated.contains(generatedTaskID))
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
