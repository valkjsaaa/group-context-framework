package com.adefreitas.groupcontextframework;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.adefreitas.messages.ComputeInstruction;
import com.adefreitas.messages.ContextCapability;
import com.adefreitas.messages.ContextRequest;

public abstract class ContextProvider 
{	
	// Internal Context Variables
	private GroupContextManager 					 groupContextManager;
	private String 									 contextType;
	private boolean 								 sharable;
	private boolean									 subscriptionDependentForCompute;
	private HashMap<String, ContextSubscriptionInfo> subscriptions;
	private int 									 refreshRate;
	private Date 									 lastTransmission;
	private ArrayList<String>						 accessList;
	private ArrayList<String>						 subscriptionParameters;
	
	public ContextProvider(String contextType, GroupContextManager groupContextManager)
	{
		this.contextType 		    		 = contextType;
		this.groupContextManager    		 = groupContextManager;
		this.sharable 		        		 = true;
		this.subscriptionDependentForCompute = true;
		this.subscriptions 		    		 = new HashMap<String, ContextSubscriptionInfo>();
		this.refreshRate 		    		 = 60000;
		this.lastTransmission       		 = new Date(0);
		this.accessList						 = new ArrayList<String>();
		this.subscriptionParameters 		 = new ArrayList<String>();
	}
	
	public String getContextType()
	{
		return contextType;
	}
	
	public boolean isInUse() 
	{
		return (subscriptions.keySet().size() > 0);
	}

	public boolean isSharable()
	{
		return sharable;
	}
	
	public void setSharable(boolean sharable)
	{
		this.sharable = sharable;
	}
	
	public void addToAccessList(String deviceID)
	{
		if (!accessList.contains(deviceID))
		{
			accessList.add(deviceID);
		}
	}
	
	public void removeFromAccessList(String deviceID)
	{
		if (accessList.contains(deviceID))
		{
			accessList.remove(deviceID);
		}
	}
	
	public boolean hasAccess(String deviceID)
	{
		if (accessList.size() == 0)
		{
			// If the Access List is Empty, then Assume Everyone Can Access This
			return true;
		}
		else
		{
			// Otherwise, Follow the Access List to the Letter
			return accessList.contains(deviceID);
		}
	}
	
	public boolean isSubscriptionDependentForCompute()
	{
		return subscriptionDependentForCompute;
	}
	
	public void setSubscriptionDependentForCompute(boolean newValue)
	{
		subscriptionDependentForCompute = newValue;
	}
	
	public void addSubscription(String deviceID, int refreshRate, int heartbeatRate, String[] parameters)
	{
		if (!subscriptions.containsKey(deviceID))
		{
			subscriptions.put(deviceID, new ContextSubscriptionInfo(deviceID, contextType, refreshRate, heartbeatRate * 3, parameters));
			updateConfiguration();
			
			// Raises an Event
			onSubscription(subscriptions.get(deviceID));
		}
	}
	
	public void removeSubscription(String deviceID)
	{
		if (subscriptions.containsKey(deviceID))
		{
			ContextSubscriptionInfo subscription = subscriptions.get(deviceID);
			
			subscriptions.remove(deviceID);
			updateConfiguration();
			
			// Raises an Event
			onSubscriptionCancelation(subscription);
		}
	}
	
	public void onHeartbeat(ContextRequest request)
	{
		if (subscriptions.containsKey(request.getDeviceID()))
		{
			// Updates the Contact History of the Device
			ContextSubscriptionInfo csi = subscriptions.get(request.getDeviceID());
			csi.setLastContact(new Date());
			csi.setParameters(request.getParameters());
		}
	}
	
	public boolean isSubscribed(String deviceID)
	{
		return subscriptions.containsKey(deviceID);
	}
	
	public int getNumSubscriptions()
	{
		return subscriptions.size();
	}
	
	public int getRefreshRate()
	{
		return refreshRate;
	}
	
	public String[] getSubscriptionParameters()
	{
		return subscriptionParameters.toArray(new String[0]);
	}
			
	public ContextSubscriptionInfo getSubscription(String deviceID)
	{
		return subscriptions.get(deviceID);
	}
	
	public ContextSubscriptionInfo[] getSubscriptions()
	{
		ContextSubscriptionInfo[] result = new ContextSubscriptionInfo[subscriptions.keySet().size()];
		
		int i=0;
		
		for (String deviceID : subscriptions.keySet())
		{
			result[i] = subscriptions.get(deviceID);
			i++;
		}
		
		return result;
	}
	
	public void reboot()
	{
		subscriptions.clear();
		subscriptionParameters.clear();
		stop();
		updateConfiguration();
	}
	
	// PROTECTED METHODS ---------------------------------------------------------------------
	protected GroupContextManager getGroupContextManager()
	{
		return groupContextManager;
	}
	
	protected void setLastTransmissionDate(Date newDate)
	{
		lastTransmission = newDate;
	}
	
	protected Date getLastTransmissionDate()
	{
		return lastTransmission;
	}
	
	// OVERRIDABLE METHODS -------------------------------------------------------------------
	public boolean sendCapability(ContextRequest request)
	{
		if (this.hasAccess(request.getDeviceID()))
		{
			// Checks to See if this Context Request is LOCAL_ONLY
			//boolean isLocalRequest = (request.getRequestType() == ContextRequest.LOCAL_ONLY) && request.getDeviceID().equals(groupContextManager.getDeviceID());
						
			// Specifies the Conditions By Which this Provider Should NOT Send Context
			if (request.getRequestType() == ContextRequest.MULTIPLE_SOURCE && isSubscribed(request.getDeviceID()))
			{
				// This code prevents capabilities from being sent if the device is already subscribed and is querying multiple devices
				// Note:  This only works for multiple source requests!  Single source needs to be able to dynamically switch to the "best" ones
				return false;
			}
			else if (request.getRequestType() == ContextRequest.SPECIFIC_SOURCE && !request.isDestination(this.getGroupContextManager().getDeviceID()))
			{
				return false;
			}
			
			// Otherwise, Send Back a Capability Message
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public int getHeartbeatRate(ContextRequest request)
	{
		if (request.getRequestType() >= ContextRequest.MULTIPLE_SOURCE)
		{
			return 10000;
		}
		else
		{
			return 60000;
		}
	}
	
	/**
	 * Allows a Context Provider to Add Custom Configuration Settings
	 * @param capability
	 */
	public void setCapabilityParameters(ContextCapability capability)
	{

	}
	
	/**
	 * Determines the next time this provider should do something
	 */
	public void updateConfiguration()
	{
		refreshRate = Integer.MAX_VALUE;
		subscriptionParameters.clear();
		
		// Examines Each Remaining Subscriptions
		// Determines Which Parameters to Add
		for (String id : subscriptions.keySet())
		{
			ContextSubscriptionInfo csi = subscriptions.get(id);
			
			// Populates Parameters
			for (String parameter : csi.getParameters())
			{
				if (!subscriptionParameters.contains(parameter))
				{
					subscriptionParameters.add(parameter);
				}
			}
			
			// Calculates the Minimum Refresh Rate
			refreshRate = Math.min(refreshRate, csi.getRefreshRate());
		}
	}

	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		this.sendMostRecentReading();
	}
	
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		// Do Nothing by Default
	}
	
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		// Does Nothing for Most Context Providers
	}
	
	// ABSTRACT METHODS ----------------------------------------------------------------------
	public abstract void start();
	
	public abstract void stop();
	
	public abstract double getFitness(String[] parameters);

	public abstract void sendMostRecentReading();
}
