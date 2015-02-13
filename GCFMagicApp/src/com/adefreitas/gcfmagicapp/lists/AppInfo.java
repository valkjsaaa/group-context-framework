package com.adefreitas.gcfmagicapp.lists;

import java.util.ArrayList;
import java.util.Date;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.liveos.ApplicationFunction;
import com.adefreitas.messages.ContextData;

public class AppInfo
{
	// Read Only Attributes
	private String 			  appID;
	private String			  appContextType;
	private String			  deviceID;
	private String 			  name;
	private String 		 	  description;
	private String 			  logo;
	private ArrayList<String> contexts;
	private ArrayList<String> preferences;
	private ArrayList<ApplicationFunction> functions;
	
	// Snap To It Values
	private double photoMatches;
	
	// Communications Settings
	private CommMode commMode;
	private String 	 ipAddress;
	private int 	 port;
	private String   channel;

	// Lifetime Variables
	private Date			  dateCreated;
	private Date			  dateExpires;
	
	// Runtime Attributes (Managed By the Application)
	private ArrayList<String> connections;
	private ContextData		  ui;
	private Date			  dateUIUpdated;
	
	/**
	 * Constructor
	 * @param appName
	 * @param logo
	 */
	public AppInfo(String appID, String appContextType, String deviceID, 
			String name, String description, String category, String logo, int lifetime, double photoMatches, ArrayList<String> contexts, ArrayList<String> preferences, 
			ArrayList<ApplicationFunction> functions, CommMode commMode, String ipAddress, int port, String channel)
	{
		this.appID		 	= appID;
		this.appContextType = appContextType;
		this.deviceID		= deviceID;
		this.name 		 	= name;
		this.description 	= description;
		this.logo 		 	= logo;
		this.contexts    	= (contexts == null) ? new ArrayList<String>() : contexts;
		this.preferences 	= (preferences == null) ? new ArrayList<String>() : preferences;
		this.functions		= (functions == null) ? new ArrayList<ApplicationFunction>() : functions;
		this.ipAddress   	= ipAddress;
		this.port        	= port;
		this.channel		= channel;
		this.commMode		= commMode;
		this.dateCreated 	= new Date();
		this.dateExpires 	= new Date(dateCreated.getTime() + lifetime * 1000);
		this.photoMatches   = photoMatches;
		
		this.connections = new ArrayList<String>();
		this.ui			 = null;
	}

	/**
	 * Gets the Application Identifier (Unique PER App)
	 * @return
	 */
	public String getAppID()
	{
		return appID;
	}
	
	/**
	 * Gets the Context Type of the Context Provider Representing this Application
	 * @return
	 */
	public String getAppContextType()
	{
		return appContextType;
	}
	
	/**
	 * Gets the ID of the Device Hosting this Application
	 * @return
	 */
	public String getDeviceID()
	{
		return deviceID;
	}
	
	/**
	 * Gets the Application Name
	 * @return
	 */
	public String getAppName()
	{
		return name;
	}

	/**
	 * Gets the Application Description
	 * @return
	 */
	public String getDescription()
	{
		return description;
	}
	
	/**
	 * Gets the Logo
	 * @return
	 */
	public String getLogo()
	{
		return logo;
	}

	public ArrayList<String> getContextsRequired()
	{
		return contexts;
	}
	
	public ArrayList<String> getPreferences()
	{
		return preferences;
	}
	
	public ArrayList<ApplicationFunction> getFunctions()
	{
		return functions;
	}
	
	public CommMode getCommMode()
	{
		return commMode;
	}
	
	public String getIPAddress()
	{
		return ipAddress;
	}
	
	public int getPort()
	{
		return port;
	}
	
	public String getChannel()
	{
		return channel;
	}
	
	public Date getDateCreated()
	{
		return dateCreated;
	}
	
	public Date getDateExpires()
	{
		return dateExpires;
	}
	
	public ArrayList<String> getConnections()
	{
		return connections;
	}
	
	public ContextData getUI()
	{
		return ui;
	}
	
	public void setUI(ContextData newUI)
	{
		this.ui 		   = newUI;
		this.dateUIUpdated = new Date();
	}

	public Date getDateUIUpdated()
	{
		return dateUIUpdated;
	}
	
	public double getPhotoMatches()
	{
		return photoMatches;
	}
	
	/**
	 * Replaces All of the Information About this App
	 * @param otherApp
	 */
	public void update(AppInfo otherApp)
	{
		this.appContextType = otherApp.getAppContextType();
		this.deviceID 		= otherApp.getDeviceID();
		this.name 			= otherApp.getAppName();
		this.description 	= otherApp.getDescription();
		this.logo 			= otherApp.getLogo();
		this.contexts 	 	= otherApp.getContextsRequired();
		this.preferences 	= otherApp.getPreferences();
		this.commMode 	 	= otherApp.getCommMode();
		this.ipAddress	 	= otherApp.getIPAddress();
		this.port		 	= otherApp.getPort();
		this.channel	 	= otherApp.getChannel();
		this.dateCreated 	= otherApp.getDateCreated();
		this.dateExpires 	= otherApp.getDateExpires();
		this.functions      = otherApp.getFunctions();
		this.photoMatches   = otherApp.getPhotoMatches();
	}
}
