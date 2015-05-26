package com.adefreitas.androidbluewave;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JSONContextParser 
{	
	// Log Name
	private static final String LOG_NAME = "JSONContextParser";
	
	// The JSON Being Parsed
	private JSONObject jsonRoot;
		
	// An Enumerated Type for the Type of Thing being Input
	public static final int JSON_TEXT = 1;
	public static final int JSON_FILE = 2;
	
	/**
	 * Constructor
	 * @param json
	 */
	public JSONContextParser(int mode, String data)
	{
		if (mode == JSON_TEXT)
		{
			parseJSON(data);
		}
		else if (mode == JSON_FILE)
		{
			parseJSONFile(data);
		}
	}
	
	public void parseJSON(String json)
	{
		try
		{
			jsonRoot = new JSONObject(json);
			//Log.d(LOG_NAME, "JSON Parsed Successfully");
		}
		catch (Exception ex)
		{
			Log.e(LOG_NAME, "Could not parse " + json + " (from string).  Creating Blank");
			jsonRoot = new JSONObject();
		}
	}
	
	public void parseJSONFile(String filePath)
	{
		//Log.d(LOG_NAME, "Parsing File: " + filePath);
		File downloadedFile = new File(filePath);
		
		if (downloadedFile.exists())
		{
			try
			{
				// Extract the Contents of the File
				Scanner scanner  = new Scanner(downloadedFile);
				String  json     = "";
				
				while (scanner.hasNextLine())
				{
					json += scanner.nextLine();
				}
				
				scanner.close();
				
				parseJSON(json);
			}
			catch (Exception ex)
			{
				Log.e(LOG_NAME, "Could not parse " + filePath + " (from file).  Creating Blank");
				ex.printStackTrace();
			}
		}
		else
		{
			Log.e(LOG_NAME, "No File Specified");
		}
	}

	public JSONObject getJSONRoot()
	{
		return jsonRoot;
	}

	public JSONObject getJSONObject(String name)
	{
		try
		{
			JSONObject result = jsonRoot.getJSONObject(name);
			return result;
		}
		catch (Exception ex)
		{
			return null;
		}
	}
	
	public void setJSONObject(String name, JSONObject obj)
	{
		try
		{
			jsonRoot.put(name, obj);
		}
		catch (Exception ex)
		{
			Log.e(LOG_NAME, "Problem occurred while loading personal context: " + ex.getMessage());
		}
	}
	
	public void removeJSONObject(String name)
	{
		if (jsonRoot != null && jsonRoot.has(name))
		{
			jsonRoot.remove(name);	
		}
	}
	
	// Gets Common Entries
	public String getDeviceID()
	{
		try
		{
			JSONObject deviceContext = jsonRoot.getJSONObject(BluewaveManager.DEVICE_TAG);
			return deviceContext.getString(BluewaveManager.DEVICE_ID);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Converts this Parser Back to JSON
	 */
	public String toString()
	{
		try
		{
			return jsonRoot.toString(2);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
}
