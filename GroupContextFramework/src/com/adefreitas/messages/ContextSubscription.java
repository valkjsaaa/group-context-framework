package com.adefreitas.messages;

import java.util.Arrays;


public class ContextSubscription extends CommMessage
{
	public enum SubscriptionUpdateType { Subscribe, Unsubscribe };
	
	private SubscriptionUpdateType updateType;
	private String 				   contextType;
	private int 				   refreshRate;
	private int  				   heartbeatRate;
	private String[] 			   parameters;
	
	public ContextSubscription(SubscriptionUpdateType updateType, String sourceDeviceID, String[] destinationDevices, 
			String contextType, int refreshRate, int heartbeatRate, String[] parameters)
	{
		super("CSUB");
		this.updateType 	= updateType;
		this.deviceID		= sourceDeviceID;
		this.destination 	= destinationDevices;
		this.contextType 	= contextType;
		this.refreshRate 	= refreshRate;
		this.heartbeatRate 	= heartbeatRate;
		this.parameters 	= parameters;
	}
	
	public SubscriptionUpdateType getUpdateType()
	{
		return updateType;
	}
		
	public String getContextType()
	{
		return contextType;
	}
	
	public int getRefreshRate()
	{
		return refreshRate;
	}

	public int getHeartbeatRate()
	{
		return heartbeatRate;
	}
	
	public String[] getParameters()
	{
		return parameters;
	}
	
	public String toString()
	{
		return String.format("SUBSCRIPTION [%s] (Operation=%s; Source=%s; Destination=%s)", contextType, updateType, deviceID, Arrays.toString(this.destination));
	}
}
