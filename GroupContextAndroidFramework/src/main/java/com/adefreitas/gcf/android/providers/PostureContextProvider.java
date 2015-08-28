package com.adefreitas.gcf.android.providers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.adefreitas.gcf.messages.ContextRequest;

/**
 * This is a template for a standard GCF context provider.  Copy and paste this code into your 
 * own application, and modify as needed.
 * @author adefreit
 */
public class PostureContextProvider extends ContextProvider
{
	private static final String ACTION_POSTURE_UPDATE = "ACTION_POSTURE_UPDATE";
	private static final String EXTRA_POSTURE_VALUE   = "POSTURE";
	
	// CONFIGURATION VARIABLES
	private static final String CONTEXT_TYPE = "POSTURE";
	private static final String LOG_NAME     = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	private static final String DESCRIPTION  = "No description provided.";
	
	// VALUE
	private int value = 0;
	
	// Intent Filter Variables
	private Context		   context;
	private IntentFilter   intentFilter;
	private CustomReceiver receiver;
	
	/**
	 * Constructor.  Creates a New Instance of this Context Provider.  Note that this method does not register the context
	 * provider with the GCM.  You still need to do this manually.
	 * @param groupContextManager - a link to the Group Context Manager
	 */
	public PostureContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, DESCRIPTION, groupContextManager);
		
		this.context 	  = context;
		this.intentFilter = new IntentFilter();
		this.receiver 	  = new CustomReceiver();
				
		intentFilter.addAction(ACTION_POSTURE_UPDATE);
	}

	/**
	 * This method is used to Start a context provider (initialize sensors).  
	 * It is automatically called by the GCM when the FIRST device subscribes to it
	 * (i.e., numSubscriptions goes from 0->1)
	 */
	@Override
	public void start() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started");
		context.registerReceiver(receiver, intentFilter);
	}

	/**
	 * This method is used to Stop a context provider (turn off sensors).  
	 * It is automatically called by the GCM when the LAST device unsubscribes from it
	 * (i.e., numSubscriptions goes from 1->0) 
	 */
	@Override
	public void stop() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
		context.unregisterReceiver(receiver);
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
		// This is a generic example of how to send context to all subscribed devices.
		// Note the CONTEXT=VALUE format of the 3rd parameter.  This is how all string parameters are specified in GCF.
		this.sendContext(this.getSubscriptionDeviceIDs(), new String[] { "VALUE=" + value });
	}

	// -----------------------------------------------------------------------------------------------------------------------
	// OPTIONAL METHODS
	// Check ContextProvider.java for a more complete list of optional methods.  The ones below are the ones that are 
	// most commonly modified by custom context providers
	// -----------------------------------------------------------------------------------------------------------------------
	/**
	 * Event that fires when the context provider receives a Compute Instruction from another device
	 * @param instruction The Compute Instruction from another device
	 */
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		// Does Nothing for Most Context Providers
	}
	
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

	/**
	 * Broadcast Receiver for Posture Intents
	 * @author adefreit
	 *
	 */
	private class CustomReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(ACTION_POSTURE_UPDATE))
			{
				int newValue = intent.getIntExtra(EXTRA_POSTURE_VALUE, 0);
				value = newValue;
			}
		}
	}
}
