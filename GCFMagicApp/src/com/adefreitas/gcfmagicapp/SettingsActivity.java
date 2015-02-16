package com.adefreitas.gcfmagicapp;

import java.util.Iterator;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.Menu;

import com.adefreitas.androidbluewave.JSONContextParser;
import com.adefreitas.groupcontextframework.GroupContextManager;

public class SettingsActivity extends PreferenceActivity
{
	private GCFApplication application;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		setTheme(R.style.AppBaseTheme);
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.preferences);
		
		// Saves a Link to the Application
		this.application = (GCFApplication)this.getApplication();
		
//		// Clears the Application
//		this.application.clearInteractions();
//		Toast.makeText(application, "Removing Interaction Queue", Toast.LENGTH_SHORT).show();
		
		// Sets Read-Only Preferences
		try
		{
			PreferenceCategory gcfCategory = (PreferenceCategory)findPreference("category_app_details");
			
			// Sets the Application Version
			String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			EditTextPreference versionPref = (EditTextPreference)gcfCategory.findPreference("app_version");
			versionPref.setSummary(version);
			
			// Sets the GCF Version
			EditTextPreference gcfVersionPref = (EditTextPreference)gcfCategory.findPreference("gcf_version");
			gcfVersionPref.setSummary(GroupContextManager.FRAMEWORK_VERSION + "");
			
			// Sets the GCF Comm Version
			EditTextPreference commPref = (EditTextPreference)gcfCategory.findPreference("gcf_comm");
			commPref.setSummary(GCFApplication.IP_ADDRESS + "::" + GCFApplication.PORT);
			
			// Sets the GCF IDs
			EditTextPreference idPref = (EditTextPreference)gcfCategory.findPreference("gcf_id");
			idPref.setSummary("GCF: " + application.getGroupContextManager().getDeviceID() + "\n" + "Android: " + android.os.Build.SERIAL);
			
			createBluewavePreferences();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{		
		return true;
	}
	
	/**
	 * Generates Automatic Bluewave Preferences
	 */
	private void createBluewavePreferences()
	{
		PreferenceCategory bluewaveCategory = (PreferenceCategory)findPreference("category_bluewave_details");
		
		JSONContextParser parser = application.getBluewaveManager().getPersonalContextProvider().getContext();
		
		if (parser != null)
		{
			Iterator<String> keys = parser.getJSONRoot().keys();
			
			while (keys.hasNext())
			{
				try
				{
					String contextLevel   = (String)keys.next();
					String contextSummary = parser.getJSONRoot().getJSONObject(contextLevel).toString(2);
					
					CheckBoxPreference checkBoxPreference = new CheckBoxPreference(this);

					//checkBoxPreference.setKey("test");
					checkBoxPreference.setChecked(true);
					
//					// Assigns a Friendly Title
//					if (contextLevel.equals("device"))
//					{
//						contextLevel = "Nearby Devices";
//						contextSummary = "Device A, Device B, ZTE-Office";
//					}
//					else if (contextLevel.equals("magic"))
//					{
//						contextLevel = "Realtime Connection Settings";
//						contextSummary = "IP_ADDRESS:\"123.45.67.89\"\nPORT: 12345\nPROTOCOL:\"MQTT\"";
//					}
//					else if (contextLevel.equals("Debug"))
//					{
//						contextLevel = "Debug Information";
//					}
					
					checkBoxPreference.setTitle(contextLevel);
					checkBoxPreference.setSummary(contextSummary);
					
					bluewaveCategory.addPreference(checkBoxPreference);		
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}	
		}
	}
}
