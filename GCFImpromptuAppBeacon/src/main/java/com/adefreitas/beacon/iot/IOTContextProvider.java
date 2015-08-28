package com.adefreitas.beacon.iot;

import org.json.JSONObject;

import android.content.Context;
import android.os.Vibrator;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.android.*;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.adefreitas.gcf.messages.ContextRequest;

public abstract class IOTContextProvider extends ContextProvider
{	
	// Information about this IOT Device
	private Context context;
	private String  category;
	
	/**
	 * Constructor.  Creates a New Instance of this Context Provider.  Note that this method does not register the context
	 * provider with the GCM.  You still need to do this manually.
	 * @param groupContextManager - a link to the Group Context Manager
	 */
	public IOTContextProvider(String contextType, String description, Context context, GroupContextManager groupContextManager, String category) 
	{
		super(contextType, description, groupContextManager);
		
		this.context  = context;
		this.category = category;
		
		// Allows ANY device to sent it compute instructions
//		this.setSubscriptionRequiredForCompute(false);
	}

	/**
	 * This method is used to Start a context provider (initialize sensors).  
	 * It is automatically called by the GCM when the FIRST device subscribes to it
	 * (i.e., numSubscriptions goes from 0->1)
	 */
	@Override
	public void start() 
	{
		this.getGroupContextManager().log(this.getContextType(), this.getContextType() + " Provider Started");
	}

	/**
	 * This method is used to Stop a context provider (turn off sensors).  
	 * It is automatically called by the GCM when the LAST device unsubscribes from it
	 * (i.e., numSubscriptions goes from 1->0) 
	 */
	@Override
	public void stop() 
	{
		this.getGroupContextManager().log(this.getContextType(), this.getContextType() + " Provider Stopped");
	}

	/**
	 * Returns a numeric value representing the "goodness" of this context provider for a specific Context Request.
	 * This value is used by the GCM to compare context providers of the same type.
	 */
	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	/**
	 * Sends context to all subscribed devices.  This method is automatically called by the framework as needed.
	 */
	@Override
	public void sendContext() 
	{
		AndroidGroupContextManager gcm = (AndroidGroupContextManager)this.getGroupContextManager();
		
		this.getGroupContextManager().sendContext(this.getContextType(), 
				this.getSubscriptionDeviceIDs(), 
				new String[] {"CONTEXT=" + gcm.getBluewaveManager().getPersonalContextProvider().getContext().toString()});
	}

	// -----------------------------------------------------------------------------------------------------------------------
	// IOT METHODS
	// These Methods are Specific to IOT Devices 
	// -----------------------------------------------------------------------------------------------------------------------
	public Context getApplicationContext()
	{
		return context;
	}
	
	public String getCategory()
	{
		return category;
	}
	
	public abstract JSONObject getJSON();
	
	// -----------------------------------------------------------------------------------------------------------------------
	// OPTIONAL METHODS
	// Check ContextProvider.java for a more complete list of optional methods.  The ones below are the ones that are 
	// most commonly modified by custom context providers
	// -----------------------------------------------------------------------------------------------------------------------
	/**
	 * Event that fires when the context provider receives a Compute Instruction from another device
	 * @param instruction The Compute Instruction from another device
	 */
	public abstract void onComputeInstruction(ComputeInstruction instruction);
	
	/**
	 * Specifies the Rate at which devices should resend their request (before getting automatically unsubscribed)
	 * @param request - The Context Request of the device
	 * @return - the Heartbeat Rate (in ms)
	 */
	public int getHeartbeatRate(ContextRequest request)
	{
		return super.getHeartbeatRate(request);
	}

	/**
	 * Event that fires when a device subscribes
	 * @param newSubscription The subscription details for the requesting device
	 */
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
	}
	
	/**
	 * Event that fires when a device unsubscribes (intentionally, or automatically by the system)
	 * @param subscription The subscription details for the unsubscribing device
	 */
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		super.onSubscriptionCancelation(subscription);
	}		
}
