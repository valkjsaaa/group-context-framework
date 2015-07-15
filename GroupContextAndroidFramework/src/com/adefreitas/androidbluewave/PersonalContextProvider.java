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
	// Context Configuration
	private static final String FRIENDLY_NAME = "PersonalContext";	
	private static final String CONTEXT_TYPE  = ContextType.PERSONAL;
	private static final String LOG_NAME      = "Bluewave-PCP";
	
	// Link to the Android Application Context Hosting this PCP
	private Context context;
	
	// Link to the Bluewave Manager
	private BluewaveManager bluewaveManager;
	
	// The JSON Representing this Object
	private JSONContextParser parser;
	
	// URLs to Bluewave Cloud Framework
	private String		contextFolder;
	private HttpToolkit httpToolkit;
		
	// Intent Filters
	private IntentFilter   filter;
	private IntentReceiver receiver;
	
	private HashMap<String, JSONObject> buffer;
	private String						key;
	private String 						lastPublishedJSON;
	
	/**
	 * Constructor
	 * @param context
	 * @param bluewaveManager
	 * @param groupContextManager
	 * @param httpToolkit
	 * @param bluewaveURLFolder
	 */
	public PersonalContextProvider(Context context, BluewaveManager bluewaveManager, GroupContextManager groupContextManager, HttpToolkit httpToolkit, String bluewaveURLFolder, String masterKey) 
	{
		super(CONTEXT_TYPE, groupContextManager);

		this.context      	 = context;
		this.bluewaveManager = bluewaveManager;
		
		this.contextFolder   = (bluewaveURLFolder.endsWith("/")) ? bluewaveURLFolder : bluewaveURLFolder + "/";
		this.httpToolkit 	 = httpToolkit;
		// Log.i(LOG_NAME, "Folder: " + contextFolder + "; Filename: " + contextFilename);
		
		// Initializes Variables
		this.setSubscriptionDependentForCompute(false);
		
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
		key				  = "";
		
		// Downloads This Device's Current Cloud Context
		httpToolkit.get(getPrivateContextURL(), BluewaveManager.ACTION_USER_CONTEXT_DOWNLOADED);
	}
	
	/**
	 * Context Provider Method:  Starts the Context Provider
	 */
	@Override
	public void start() 
	{
		Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Started ");
	}

	/**
	 * Context Provider Method:  Stops the Context Provider
	 */
	@Override
	public void stop() 
	{
		Log.d(LOG_NAME, FRIENDLY_NAME + " Sensor Stopped ");
	}

	/**
	 * Context Provider Method:  Returns the "Quality" of this Context Provider
	 */
	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	/**
	 * Context Provider Method:  Delivers Context to all Subscribed Devices
	 */
	@Override
	public void sendContext() 
	{
		if (parser != null)
		{
			this.getGroupContextManager().sendContext(getContextType(), new String[0], new String[] { "context=" + parser.toString() });	
		}
	}

	/**
	 * Context Provider Method:  Responds to Compute Instructions Sent by Other GCF Devices
	 */
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{		
		// Creates and Sends a Broadcast Containing Compute Instructions
		Intent intent = new Intent(BluewaveManager.ACTION_COMPUTE_INSTRUCTION_RECEIVED);
		intent.putExtra(ComputeInstruction.COMPUTE_CONTEXT_TYPE, instruction.getContextType());
		intent.putExtra(ComputeInstruction.COMPUTE_SENDER, instruction.getDeviceID());
		intent.putExtra(ComputeInstruction.COMPUTE_COMMAND, instruction.getCommand());
		intent.putExtra(ComputeInstruction.COMPUTE_PARAMETERS, instruction.getPayload());
		context.sendBroadcast(intent);
	}
	
	// URLS --------------------------------------------------------------------------------------------------------------
	private String getPrivateContextURL()
	{
		return contextFolder + "context/" + this.getGroupContextManager().getDeviceID().replace(" ", "%20") + ".blu";
	}
	
	public String getPublicContextURL()
	{
		return contextFolder + "getContext.php";
	}
	
	public String getUpdateURL()
	{
		return contextFolder + "update.php";
	}
	
	public String getPublicKey()
	{
		return key;
	}
	
	// Personal Context Management ---------------------------------------------------------------------------------------	
	public void loadJSONString(String json)
	{
		if (json == null || json.length() == 0)
		{
			// Downloads This Device's Current Cloud Context
			httpToolkit.get(getPrivateContextURL(), BluewaveManager.ACTION_USER_CONTEXT_DOWNLOADED);
			Toast.makeText(context, "Error Getting Context. Trying Again", Toast.LENGTH_SHORT).show();
		}
		else
		{
			try
			{
				parser   = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
				this.key = parser.getJSONObject("_bluewave_key").getString("key");	
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			
			//publish();
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
				//publish();
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

				//publish();	
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
			//publish();	
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
				String json     = this.getContext().toString();
				
				if (!lastPublishedJSON.equals(json))
				{
					// TODO:  Assumes that PHP is Used to Write/Update Files!
					String url = String.format(contextFolder + "upload.php?deviceID=%s", this.getGroupContextManager().getDeviceID().replace(" ", "%20"));
					httpToolkit.post(url, "json=" + json, BluewaveManager.ACTION_USER_CONTEXT_UPDATED);
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
			
			// Adds the Device Tag
			parser.setJSONObject(BluewaveManager.DEVICE_TAG, deviceContext);
		}
		catch (Exception ex)
		{
			Log.e(LOG_NAME, "Problem occurred while updating device context: " + ex.getMessage());
			
			// Downloads This Device's Current Cloud Context
			httpToolkit.get(this.getPrivateContextURL(), BluewaveManager.ACTION_USER_CONTEXT_DOWNLOADED);
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
				String responseJSON = intent.getStringExtra(HttpToolkit.HTTP_RESPONSE);
				
				try
				{
					JSONObject jsonObj = new JSONObject(responseJSON);
					key = jsonObj.getString("key");	
					bluewaveManager.updateBluetoothName();
					Log.d(LOG_NAME, "PERSONAL CONTEXT UPDATED: " + responseJSON);
				}
				catch (Exception ex)
				{
					Log.d(LOG_NAME, "Problem Occurred Updating Personal Context: " + ex.getMessage());
				}
			}
			else
			{
				Log.e("GCFApplication", "Unknown Action: " + intent.getAction());
			}
		}
	}
}
