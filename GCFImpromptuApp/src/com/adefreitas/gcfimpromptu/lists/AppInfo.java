package com.adefreitas.gcfimpromptu.lists;

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
	private String			  category;
	private String 			  logo;
	private ArrayList<String> contexts;
	private ArrayList<String> preferences;
	private ArrayList<ApplicationFunction> functions;
	
	// Snap To It Values
	private double photoMatches;
	private boolean favorite;
	
	// Communications Settings
	private CommMode commMode;
	private String 	 ipAddress;
	private int 	 port;
	private String   channel;

	// Lifetime Variables
	private int  lifetime;
	private Date dateCreated;
	private Date dateExpires;
	
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
		this.category	    = category;
		this.logo 		 	= logo;
		this.contexts    	= (contexts == null) ? new ArrayList<String>() : contexts;
		this.preferences 	= (preferences == null) ? new ArrayList<String>() : preferences;
		this.functions		= (functions == null) ? new ArrayList<ApplicationFunction>() : functions;
		this.ipAddress   	= ipAddress;
		this.port        	= port;
		this.channel		= channel;
		this.commMode		= commMode;
		this.lifetime       = lifetime;
		this.dateCreated 	= new Date();
		this.dateExpires 	= new Date(dateCreated.getTime() + lifetime * 1000);
		this.photoMatches   = photoMatches;
		
		// These are values that the app decides
		this.connections = new ArrayList<String>();
		this.ui			 = null;
		this.favorite    = false;
	}

	/**
	 * Gets the Application Identifier (Unique PER App)
	 * @return
	 */
	public String getID()
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
	public String getName()
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
	 * Gets the Application Category
	 * @return
	 */
	public String getCategory()
	{
		return category;
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
	
	public int getLifetime()
	{
		return lifetime;
	}
	
	public Date getDateCreated()
	{
		return dateCreated;
	}
	
	public Date getDateExpires()
	{
		return dateExpires;
	}
	
	public long getTimeToExpire()
	{
		return getDateExpires().getTime() - new Date().getTime();
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
	
	public boolean isFavorite()
	{
		return favorite;
	}
	
	public void setFavorite(boolean value)
	{
		favorite = value;
	}
	
	public boolean isExpired()
	{
		return new Date().getTime() > this.dateExpires.getTime();
	}
	
	/**
	 * Replaces All of the Information About this App
	 * @param otherApp
	 */
	public void update(AppInfo otherApp)
	{
		this.appContextType = otherApp.getAppContextType();
		this.deviceID 		= otherApp.getDeviceID();
		this.name 			= otherApp.getName();
		this.description 	= otherApp.getDescription();
		this.logo 			= otherApp.getLogo();
		this.contexts 	 	= otherApp.getContextsRequired();
		this.preferences 	= otherApp.getPreferences();
		this.commMode 	 	= otherApp.getCommMode();
		this.ipAddress	 	= otherApp.getIPAddress();
		this.port		 	= otherApp.getPort();
		this.channel	 	= otherApp.getChannel();
		this.lifetime	    = otherApp.getLifetime();
		this.dateCreated 	= otherApp.getDateCreated();
		this.dateExpires 	= otherApp.getDateExpires();
		this.functions      = otherApp.getFunctions();
		this.photoMatches   = otherApp.getPhotoMatches();
	}
}
