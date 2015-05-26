package com.adefreitas.liveos;

import java.util.ArrayList;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;

public abstract class ApplicationProvider extends ContextProvider
{	
	// Context Configuration
	protected String APP_CHANNEL  = "cmu/gcf_application";
	protected String CONTEXT_TYPE = "UNKNOWN_APP";
	protected String LOG_NAME 	  = "";
	
	// App Configuration and Settings
	protected String   appID;
	protected String   name;
	protected String   description;
	protected String   category;
	protected String[] contextsRequired;
	protected String[] preferencesToRequest;
	protected String   logoPath;
	protected int	   lifetime;
	protected CommMode commMode;
	protected String   ipAddress;
	protected int      port;
	protected String   channel;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public ApplicationProvider(GroupContextManager groupContextManager, 
			String contextType, 
			String name, 
			String description, 
			String category,
			String[] contextsRequired, 
			String[] preferencesToRequest, 
			String logoPath, 
			int lifetime,
			CommMode commMode,
			String ipAddress, 
			int port) 
	{
		super(contextType, groupContextManager);
	
		this.setSubscriptionDependentForCompute(false);
		
		// Saves Values
		CONTEXT_TYPE = contextType;
		LOG_NAME 	 = "GCF-MagicApp [" + CONTEXT_TYPE    + "]";
		
		// Creates a Unique ID
		this.appID = "APP_" + contextType;
		
		// Saves App Settings
		this.name 				  = name;
		this.description 		  = description;
		this.category 			  = category;
		this.contextsRequired 	  = contextsRequired;
		this.preferencesToRequest = preferencesToRequest;
		this.logoPath 			  = logoPath;
		this.lifetime			  = lifetime;
		this.commMode			  = commMode;
		this.ipAddress 			  = ipAddress;
		this.port 				  = port;
		this.channel			  = contextType;
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
	
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		System.out.println("NEW SUB: " + newSubscription.toString());
		
		// Sends the UI Immediately
		sendContext();
		
		// Stores Context
		String context = CommMessage.getValue(newSubscription.getParameters(), "context");
		
		// Determines Credentials
		//String username = CommMessage.getValue(newSubscription.getParameters(), "credentials");
		//System.out.println("Subscription: " + username);
	}
	
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
//		// Determines Credentials
//		System.out.print("Unsubscription: ");
//		String username = CommMessage.getValue(subscription.getParameters(), "credentials");
//		System.out.println(username + "\n");
	}
	
	public void sendContext()
	{
		for (ContextSubscriptionInfo info : this.getSubscriptions())
		{
			//System.out.println("Sending Interface to: " + info.getDeviceID());
			this.getGroupContextManager().sendContext(CONTEXT_TYPE, new String[] { info.getDeviceID() }, getInterface(info));
		}
	}
	
	@Override
	public double getFitness(String[] parameters)
	{
		return 1.0;
	}
		
	// GETTERS/SETTERS --------------------------------------------------------------------------------
	public String getAppID()
	{
		return appID;
	}
	
	/**
	 * Returns a String Array Containing Information Needed by the Mobile App
	 * @return
	 */
	public ArrayList<String> getInformation(String userContextJSON)
	{		
		ArrayList<String> result = new ArrayList<String>();
		
		result.add("APP_ID=" + appID);
		result.add("APP_CONTEXT_TYPE=" + CONTEXT_TYPE);
		result.add("DEVICE_ID=" + this.getGroupContextManager().getDeviceID());
		result.add("NAME=" + getName(userContextJSON));
		result.add("DESCRIPTION=" + getDescription(userContextJSON));
		result.add("CATEGORY=" + category);
		result.add("CONTEXTS=" + arrayToString(contextsRequired));
		result.add("PREFERENCES=" + arrayToString(preferencesToRequest));
		result.add("LOGO=" + logoPath);
		result.add("LIFETIME=" + getLifetime(userContextJSON));
		result.add("FUNCTIONS="	+ getFunctions());
		result.add("COMM_MODE="	+ commMode.toString());
		result.add("APP_ADDRESS=" + ipAddress);
		result.add("APP_PORT=" + Integer.toString(port));
		result.add("APP_CHANNEL=" + channel);
		
		return result;
	}
	
	public String getName(String userContextJSON)
	{
		return name;
	}
	
	public void setName(String newName)
	{
		this.name = newName;
	}
	
	public String getCategory()
	{
		return category;
	}
	
	public String getDescription(String userContextJSON)
	{
		return description;
	}
	
	public void setDescription(String newDescription)
	{
		this.description = newDescription;
	}
	
	public int getLifetime(String userContextJSON)
	{
		return this.lifetime;
	}
	
	public void setLifetime(int newLifetime)
	{
		this.lifetime = newLifetime;
	}
	
	// METHODS TO IMPLEMENT/OVERRIDE ------------------------------------------------------------------
	public abstract String[] getInterface(ContextSubscriptionInfo subscription);
	
	public String getFunctions()
	{
		return null;
	}
	
	public abstract boolean sendAppData(String bluewaveContextJSON);
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
	}
	
	// HELPER METHODS ---------------------------------------------------------------------------------	
	public void setUserPreference(String deviceID, String key, String value)
	{
		this.getGroupContextManager().sendComputeInstruction("PREF", new String[] { deviceID }, "SET_PREFERENCE", new String[] { key + "=" + value });
	}
	
	private String arrayToString(String[] array)
	{
		String result = "";
		
		if (array != null)
		{
			for (String s : array)
			{
				result += s + ",";
			}
			
			if (result.length() > 0)
			{
				result = result.substring(0, result.length()-1);
			}	
		}
		
		return result;
	}
}
