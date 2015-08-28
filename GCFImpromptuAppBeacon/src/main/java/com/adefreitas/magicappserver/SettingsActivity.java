package com.adefreitas.magicappserver;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.Menu;

import com.adefreitas.gcf.GroupContextManager;

public class SettingsActivity extends PreferenceActivity
{
	private GCFApplication application;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.preferences);
		
		// Saves a Link to the Application
		this.application = (GCFApplication)this.getApplication();
		
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
}
