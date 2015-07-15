package com.adefreitas.gcfimpromptu.lists;

import java.util.ArrayList;
import java.util.Collections;

public class AppCategoryInfo implements Comparable<AppCategoryInfo>
{
	private String 			   name;
	private ArrayList<AppInfo> apps;
	private boolean			   renderAll;
	
	/**
	 * Constructor
	 * @param name
	 */
	public AppCategoryInfo(String name)
	{
		this.name 	   = name;
		this.apps 	   = new ArrayList<AppInfo>();
		this.renderAll = false;
	}
	
	public void addApp(AppInfo app)
	{
		if (!apps.contains(app))
		{
			apps.add(app);
		}
	}
	
	public void removeApp(AppInfo app)
	{
		apps.remove(app);
	}
	
	public void updateApp(AppInfo newApp)
	{
		for (AppInfo app : apps)
		{
			if (app.getID().equalsIgnoreCase(newApp.getID()))
			{
				//System.out.println("Updating " + newApp.getName());
				app.update(newApp);
			}
		}
	}
	
	public boolean containsAvailableApps()
	{
		for (AppInfo app : apps)
		{
			if (!app.isExpired())
			{
				return true;
			}
		}
		
		return false;
	}
	
	public boolean hasApp(String appID)
	{
		for (AppInfo app : apps)
		{
			if (app.getID().equalsIgnoreCase(appID))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public String getName()
	{
		return name;
	}
	
	public boolean shouldRenderAll()
	{
		return renderAll;
	}
	
	public void setRenderAll(boolean newValue)
	{
		renderAll = newValue;
	}
	
	public ArrayList<AppInfo> getApps()
	{
		Collections.sort(apps);
		return apps;
	}
	
	public ArrayList<AppInfo> getAvailableApps()
	{
		ArrayList<AppInfo> result = new ArrayList<AppInfo>();
		
		for (AppInfo app : apps)
		{
			if (!app.isExpired())
			{
				result.add(app);
			}
		}
		
		// Sorts the List :)
		Collections.sort(result);
		
		return result;
	}

	
	/**
	 * Allows this object to be sorted
	 */
	@Override
	public int compareTo(AppCategoryInfo another) 
	{	
		// Returns the Name Comparison
		return this.name.compareTo(another.name);
	}
}
