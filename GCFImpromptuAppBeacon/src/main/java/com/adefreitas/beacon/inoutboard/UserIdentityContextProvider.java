package com.adefreitas.beacon.inoutboard;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.messages.ContextRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * This is a template for a context provider.
 * COPY AND PASTE; NEVER USE
 * @author adefreit
 */
public class UserIdentityContextProvider extends ContextProvider
{
	// Context Configuration
	private static final String CONTEXT_TYPE       = "USER_ID";
	private static final String LOG_NAME           = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	private static final String PREFERENCE_USER_ID = "com.adefreitas.inoutboard.userids";
	
	// Provider Specific Variables
	private Gson gson;
	
	// Stores the Location that this Context Provider
	private String locationName;
	
	// Stores the Device Name to ID
	private HashMap<String, UserData> entries;
	
	// App Storage
	private SharedPreferences appSharedPreferences;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public UserIdentityContextProvider(GroupContextManager groupContextManager, SharedPreferences appSharedPreferences) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		
		this.locationName 		  = "UNKNOWN LOCATION [" + this.getGroupContextManager().getDeviceID() + "]";
		this.gson    	  		  = new Gson();
		this.entries 	  		  = new HashMap<String, UserData>();
		this.appSharedPreferences = appSharedPreferences;
		
		// Restores Entries from Preferences
		restoreEntries();
	}

	@Override
	public void start() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started");
	}

	@Override
	public void stop() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
	}

	public boolean sendCapability(ContextRequest request)
	{
		return super.sendCapability(request) && !request.getDeviceID().equals(this.getGroupContextManager().getDeviceID());
	}
	
	public void setLocationName(String locationName)
	{
		this.locationName = locationName;
	}
	
	public String getLocationName()
	{
		return this.locationName;
	}
	
	public void addEntry(String deviceID, JSONObject data)
	{
		try
		{
			UserData userData = new UserData(deviceID, data.getString("name"), locationName);
			entries.put(deviceID, userData);
			updateEntry(deviceID);
		}
		catch (Exception ex)
		{
			Log.e(LOG_NAME, "Could not add Entry: " + ex.getMessage());
		}
		
		saveEntries();
	}
	
	public void updateEntry(String deviceID)
	{
		if (entries.containsKey(deviceID))
		{
			entries.get(deviceID).updateLastEncountered(locationName);
		}
		
		saveEntries();
	}
	
	public void update(String json)
	{		
		ArrayList<UserData> data = gson.fromJson(json,new TypeToken<ArrayList<UserData>>(){}.getType());
		
		for (UserData userData : data)
		{
			entries.put(userData.getDeviceID(), userData);
		}
		
		saveEntries();
	}
	
	private void saveEntries()
	{
		Log.d(LOG_NAME, "Saving IDs (" + entries.size() + " entries)");
		SharedPreferences.Editor editor = appSharedPreferences.edit();
		editor.putString(PREFERENCE_USER_ID, gson.toJson(entries));
		editor.commit();
	}
	
	private void restoreEntries()
	{
		// Loads App Preferences
		if (appSharedPreferences.contains(PREFERENCE_USER_ID))
		{
			String json = appSharedPreferences.getString(PREFERENCE_USER_ID, "");
			
			if (json.length() > 0)
			{
				// Attempts to Extract the App Preferences from JSON
				try
				{
					entries = (HashMap<String, UserData>) gson.fromJson(json, new TypeToken<HashMap<String, UserData>>(){}.getType());
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
				
			}
		}
		else
		{
			Log.d(LOG_NAME, "Creating New App Preferences");
		}
	}
	
	public UserData getUserData(String deviceID)
	{
		return entries.get(deviceID);
	}
	
	public ArrayList<UserData> getCurrentUserData()
	{
		ArrayList<UserData> result = new ArrayList<UserData>();
				
		for (UserData u : entries.values())
		{
			long timeElapsed = System.currentTimeMillis() - u.getLastEncounteredDate().getTime(); 
			
			if (timeElapsed < 300000)
			{
				result.add(u);	
			}
		}
		
		return result;
	}
	
	public void cleanup(long maxAge)
	{
		for (UserData u : new ArrayList<UserData>(entries.values()))
		{
			long timeElapsed = new Date().getTime() - u.getLastEncounteredDate().getTime();
			
			if (timeElapsed > maxAge)
			{
				entries.remove(u.getDeviceID());
			}
		}
	}
	
	public JSONArray getData()
	{
		JSONArray result = new JSONArray();
		
		for (String deviceID : entries.keySet())
		{
			result.put(entries.get(deviceID));
		}
		
		return result;
	}

	public String[] getDataAsString()
	{
		ArrayList<String> result = new ArrayList<String>();
		
		for (String deviceID : entries.keySet())
		{
			result.add(entries.get(deviceID).toString());
		}
		
		return result.toArray(new String[0]);
	}
	
	public Boolean hasData(String deviceID)
	{
		return entries.containsKey(deviceID);
	}
	
	public void clear()
	{
		entries.clear();
	}
	
	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendContext() 
	{		
		ArrayList<String> deviceIDs = new ArrayList<String>();
		
		for (ContextSubscriptionInfo subscription : this.getSubscriptions())
		{
			deviceIDs.add(subscription.getDeviceID());
		}
	
		String json = gson.toJson(entries.values());
		this.getGroupContextManager().sendContext(this.getContextType(), deviceIDs.toArray(new String[0]), new String[] { "LOCATION=" + this.locationName, "VALUES=" + json});
		
	}
}
