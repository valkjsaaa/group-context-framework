package com.adefreitas.androidbluewave;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.androidframework.toolkit.HttpToolkit;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.ContextType;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

/**
 *
 */
public class PersonalContextProvider extends ContextProvider
{
	// Intent
	public static final String COMPUTE_SENDER 	  = "SENDER";
	public static final String COMPUTE_COMMAND 	  = "COMMAND";
	public static final String COMPUTE_PARAMETERS = "PARAMETERS";
	
	// Context Configuration
	private static final String FRIENDLY_NAME = "PersonalContext";	
	private static final String CONTEXT_TYPE  = ContextType.PERSONAL;
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	// Link to the Cloud Location Containing JSON Context
	private String contextCloudPath;
	
	// Link to the Context
	private Context context;
	
	// Link to the Bluewave Manager
	private BluewaveManager bluewaveManager;
	
	// The JSON Representing this Object
	private JSONContextParser parser;
	
	// TODO:  Testing New Network Code
	private String		   contextFilename;
	private String		   contextFolder;
	private HttpToolkit httpToolkit;
		
	// Intent Filters
	private IntentFilter   filter;
	private IntentReceiver receiver;
	
	private HashMap<String, JSONObject> buffer;
	private String 						lastPublishedJSON;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public PersonalContextProvider(Context context, BluewaveManager bluewaveManager, GroupContextManager groupContextManager, HttpToolkit httpToolkit, String contextCloudPath) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		
		this.bluewaveManager = bluewaveManager;
		
		contextFolder   = contextCloudPath.substring(0, contextCloudPath.lastIndexOf("/") + 1).replace(" ",  "%20");
		contextFilename = contextCloudPath.substring(contextCloudPath.lastIndexOf("/") + 1).replace(" ",  "%20");
		Log.i(LOG_NAME, "Folder: " + contextFolder + "; Filename: " + contextFilename);
		
		// Initializes Variables
		this.setSubscriptionDependentForCompute(false);
		
		this.contextCloudPath = contextCloudPath;
		this.context      	  = context;
		this.httpToolkit      = httpToolkit;
		
		// Sets Up Intent Filtering and Listening
		this.receiver = new IntentReceiver();
		this.filter   = new IntentFilter();
		filter.addAction(BluewaveManager.ACTION_USER_CONTEXT_DOWNLOADED);
		filter.addAction(BluewaveManager.ACTION_USER_CONTEXT_UPDATED);
		this.context.registerReceiver(receiver, filter);
		
		// Creates a Buffer to Store Context Data while waiting for the parser to be populated
		this.buffer = new HashMap<String, JSONObject>();
		
		// Tracks the Last Publish
		lastPublishedJSON = "";
		
		// Downloads This Device's Current Cloud Context
		httpToolkit.get(contextFolder + contextFilename, BluewaveManager.ACTION_USER_CONTEXT_DOWNLOADED);
	}
	
	@Override
	public void start() 
	{
		Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Started ");
	}

	@Override
	public void stop() 
	{
		Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Stopped ");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendMostRecentReading() 
	{
		if (parser != null)
		{
			this.getGroupContextManager().sendContext(getContextType(), "", new String[0], new String[] { "context=" + parser.toString() });	
		}
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		Log.d(LOG_NAME, "Received: " + instruction);
		
		// Creates and Sends a Broadcast Containing Compute Instructions
		Intent intent = new Intent(BluewaveManager.ACTION_COMPUTE_INSTRUCTION_RECEIVED);
		intent.putExtra(COMPUTE_SENDER, instruction.getDeviceID());
		intent.putExtra(COMPUTE_COMMAND, instruction.getCommand());
		intent.putExtra(COMPUTE_PARAMETERS, instruction.getParameters());
		context.sendBroadcast(intent);
	}
	
	// Personal Context Management ---------------------------------------------------------------------------------------
	/**
	 * Loads JSON for this Particular Device
	 * @param json
	 */
	public void loadJSONFile(String filePath)
	{
		parser = new JSONContextParser(JSONContextParser.JSON_FILE, filePath);
		publish();
	}
	
	public void loadJSONString(String json)
	{
		if (json == null || json.length() == 0)
		{
			// Downloads This Device's Current Cloud Context
			httpToolkit.get(contextFolder + contextFilename, BluewaveManager.ACTION_USER_CONTEXT_DOWNLOADED);
			Toast.makeText(context, "Error Getting Context. Trying Again", Toast.LENGTH_SHORT).show();
		}
		else
		{
			parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
			publish();	
		}
	}
	
	public JSONObject getJSONObject(String name)
	{
		if (parser != null)
		{
			return parser.getJSONObject(name);
		}
		else
		{
			return null;
		}
	}

	public void setContext(String name, JSONObject obj)
	{
		try
		{
			if (parser == null)
			{
				buffer.put(name,  obj);
			}
			else
			{
				parser.setJSONObject(name, obj);
				publish();
			}
		}
		catch (Exception ex)
		{
			Log.e(LOG_NAME, "Problem setting JSON: " + ex.getMessage());
		}
	}
	
	public void setContext(String name, String value)
	{
		try
		{
			if (parser == null)
			{
				JSONObject context = new JSONObject();
				context.put(name, value);
				buffer.put(name, context);
			}
			else
			{
				// Looks for a Preexisting Item
				JSONObject context = parser.getJSONObject(name);
				
				if (context == null)
				{
					context = new JSONObject();
					parser.setJSONObject(name, context);
				}
				
				context.put(name, value);

				publish();	
			}
		}
		catch (Exception ex)
		{
			Log.e(LOG_NAME, "Problem setting JSON: " + ex.getMessage());
		}
	}
	
	public void removeContext(String name)
	{
		if (parser != null)
		{
			parser.removeJSONObject(name);
			publish();	
		}
	}
	
	public JSONContextParser getContext()
	{
		this.setDefaultContext();
		return parser;
	}
	
	/**
	 * Uploads the File Back to the Cloud
	 */
	public void publish()
	{	
		try
		{
			if (parser != null)
			{				
				// Updates the Device Configuration Too
				this.setDefaultContext();
				
				// Adds All Items from Buffer
				for (String key : buffer.keySet())
				{
					JSONObject obj = buffer.get(key);
					parser.setJSONObject(key, obj);
				}
				
				// Uploads the File
				String dev_name = contextFilename.substring(0, contextFilename.lastIndexOf("."));
				String json     = this.getContext().toString();
				
				if (!lastPublishedJSON.equals(json))
				{
					// TODO:  Assumes that PHP is Used to Write/Update Files!
					String url = String.format(contextFolder + "upload.php?deviceID=%s", dev_name);
					httpToolkit.post(url, "json=" + json, BluewaveManager.ACTION_USER_CONTEXT_UPDATED);
					
					bluewaveManager.updateBluetoothName();
				}
				
				buffer.clear();
				lastPublishedJSON = json;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Obtains Basic Device Settings
	 */
	private void setDefaultContext()
	{
		try
		{	
//			// Device Settings
//			JSONObject deviceContext = new JSONObject();
//			deviceContext.put("deviceID", this.getGroupContextManager().getDeviceID());
//			deviceContext.put("commMode", GCFApplication.COMM_MODE);
//			deviceContext.put("commIP", GCFApplication.IP_ADDRESS);
//			deviceContext.put("commPort", 12345);
//			deviceContext.put("callbackProvider", CONTEXT_TYPE);
//			
//			// User Settings
//			JSONObject userContext = new JSONObject();
//			
//			// Grabs Account Information
//			Account[] accounts = AccountManager.get(application).getAccountsByType("com.google");
//			if (accounts.length > 0)
//			{
//				userContext.put("name", accounts[0].name.split("@")[0]);
//				userContext.put("email", accounts[0].name);
//			}
//			
//			// Grabs the Default Language
//			userContext.put("language", Locale.getDefault().getDisplayLanguage());
//			
//			// Get Telephone Number
//			TelephonyManager tMgr = (TelephonyManager)application.getSystemService(Context.TELEPHONY_SERVICE);
//			if (tMgr.getLine1Number() != null)
//			{
//				userContext.put("telephone", tMgr.getLine1Number());
//			}
//			else
//			{
//				userContext.put("telephone", "NOT AVAILABLE");
//			}
//			
//			// Grabs the User's Calendar
//			userContext.put("calendar", getCalendarData(3));
//			
//			// Adds the JSON Objects to the Overall Context
//			parser.setJSONObject(DEVICE_TAG, deviceContext);
//			parser.setJSONObject(USER_TAG,userContext);
//			parser.setJSONObject(COMM_TAG, new JSONObject());

			// Device Settings
			JSONObject deviceContext = new JSONObject();
			deviceContext.put(BluewaveManager.DEVICE_ID, this.getGroupContextManager().getDeviceID());
			
			// Describes All Available Context Providers
			JSONArray contexts = new JSONArray();
			for (ContextProvider provider : this.getGroupContextManager().getRegisteredProviders())
			{
				contexts.put(provider.getContextType());
			}
			deviceContext.put(BluewaveManager.CONTEXTS, contexts);
			
			// Describes All Nearby Devices (According to Bluetooth)
			JSONArray devices = new JSONArray();
			for (String deviceID : bluewaveManager.getNearbyDevices(30))
			{
				devices.put(deviceID);
			}
			deviceContext.put(BluewaveManager.DEVICES, devices);
			
			// Adds the Device Tag
			parser.setJSONObject(BluewaveManager.DEVICE_TAG, deviceContext);
		}
		catch (Exception ex)
		{
			Log.e(LOG_NAME, "Problem occurred while updating device context: " + ex.getMessage());
			
			// Downloads This Device's Current Cloud Context
			httpToolkit.get(contextFolder + contextFilename, BluewaveManager.ACTION_USER_CONTEXT_DOWNLOADED);
		}
	}
	
	private class IntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(BluewaveManager.ACTION_USER_CONTEXT_DOWNLOADED))
			{	
				// Grabs the Newly Downloaded JSON
				String json = intent.getStringExtra(HttpToolkit.HTTP_RESPONSE);
				
				// Loads the File Into the Personal Context Provider
				loadJSONString(json);
			}
			else if (intent.getAction().equals(BluewaveManager.ACTION_USER_CONTEXT_UPDATED))
			{
				// Grabs the Newly Downloaded JSON
				String response = intent.getStringExtra(HttpToolkit.HTTP_RESPONSE);
				Log.d(LOG_NAME, "PERSONAL CONTEXT UPDATE: " + response);
				//Toast.makeText(context, "UPDATE COMPLETE: " + response, Toast.LENGTH_SHORT).show();
			}
			else
			{
				Log.e("GCFApplication", "Unknown Action: " + intent.getAction());
			}
		}
	}
}
