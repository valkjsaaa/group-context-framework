package com.adefreitas.gcfimpromptu;

import java.util.Iterator;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.util.Log;
import android.view.Menu;

import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.android.bluewave.JSONContextParser;
import com.adefreitas.gcfmagicapp.R;

public class SettingsActivity extends PreferenceActivity
{
	private GCFApplication application;
	
	/**
	 * Android Method:  Used when an Activity is Created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		setTheme(R.style.AppBaseTheme);
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.preferences);
		
		// Saves a Link to the Application
		this.application = (GCFApplication)this.getApplication();
		
		try
		{
			PreferenceCategory appCategory = (PreferenceCategory)findPreference("category_app_details");
			
			// Sets the Application Version
			String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			EditTextPreference versionPref = (EditTextPreference)appCategory.findPreference("app_version");
			versionPref.setSummary(version);
			
			PreferenceCategory gcfCategory = (PreferenceCategory)findPreference("category_gcf");
			
			// Sets the GCF Version
			EditTextPreference gcfVersionPref = (EditTextPreference)gcfCategory.findPreference("gcf_version");
			gcfVersionPref.setSummary(GroupContextManager.FRAMEWORK_VERSION + "");

			// Sets the GCF Service
			EditTextPreference gcfServicePref = (EditTextPreference)gcfCategory.findPreference("gcf_service");
			gcfServicePref.setSummary("ID: " + application.getGCFService().getServiceID() + "\nStarted: " + application.getGCFService().getDateStarted());
			
			// Sets the GCF Comm Version
			EditTextPreference commPref = (EditTextPreference)gcfCategory.findPreference("gcf_comm");
			commPref.setSummary(GCFApplication.IP_ADDRESS + "::" + GCFApplication.PORT);
			
			// Sets the GCF IDs
			EditTextPreference idPref = (EditTextPreference)gcfCategory.findPreference("gcf_id");
			idPref.setSummary("GCF: " + application.getGroupContextManager().getDeviceID() + "\n" + "Android: " + android.os.Build.SERIAL);
			
			//createBluewavePreferences();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Android Method:  Used when an Activity Menu is Generated
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{		
		return true;
	}
	
	/**
	 * Android Method:  Used when an Activity is Resumed
	 */
	protected void onResume()
	{
		super.onResume();	
		application.setInForeground(true);
	}
	
	/**
	 * Android Method:  Used when an Activity is Paused
	 */
	@Override
	protected void onPause() 
	{
	    super.onPause();
	    application.setInForeground(false);
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
			Iterator<String> contextCategories = parser.getJSONRoot().keys();
			
			while (contextCategories.hasNext())
			{
				try
				{
					String contextCategory = (String)contextCategories.next();
					String contextSummary  = "Sharing information about: {";
					
					Iterator<String> contextSubcategories = parser.getJSONRoot().getJSONObject(contextCategory).keys();
					
					// Generates a Friendly Text Field to Describe the Context Being Shared
					while (contextSubcategories.hasNext())
					{
						contextSummary += contextSubcategories.next() + "; ";
					}
					
					contextSummary = contextSummary.trim();
					contextSummary += "}";
					
					// Generates a Checkbox Preference
					String categoryName = "BLUEWAVE_" + contextCategory;
					CheckBoxPreference checkBoxPreference = new CheckBoxPreference(this);
					checkBoxPreference.setKey(categoryName);
					checkBoxPreference.setChecked(application.getAppSharedPreferences().getBoolean(categoryName, true));					
					checkBoxPreference.setTitle(contextCategory);
					checkBoxPreference.setSummary(contextSummary);
					checkBoxPreference.setOnPreferenceChangeListener(onBluewaveCheckListener);
					
					bluewaveCategory.addPreference(checkBoxPreference);		
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}	
		}
	}
	
	/**
	 * This Automatically
	 */
	final CheckBoxPreference.OnPreferenceChangeListener onBluewaveCheckListener = new Preference.OnPreferenceChangeListener() 
	{            
        public boolean onPreferenceChange(Preference preference, Object newValue) 
        {
            Log.d("IMPROMPTU", "Bluewave Preference " + preference.getKey() + " changed to " + newValue.toString());    
            application.getAppSharedPreferences().edit().putBoolean(preference.getKey(), Boolean.parseBoolean(newValue.toString())).commit();
            return true;
        }
    }; 
}
