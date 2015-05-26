package com.adefreitas.magicappserver;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.adefreitas.androidbluewave.BluewaveManager;
import com.adefreitas.androidframework.AndroidGroupContextManager;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.messages.ContextData;

/**
 * This is a template for a context provider.
 * COPY AND PASTE; NEVER USE
 * @author adefreit
 */
public class BluewaveContextProvider extends ContextProvider
{
	// Context Configuration
	private static final String CONTEXT_TYPE = "BLUEWAVE";
	private static final String LOG_NAME     = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	// Link to the Application
	private Context application;
	private AndroidGroupContextManager gcm;
	
	// Intent Receivers
	private IntentFilter   filter;
	private IntentReceiver intentReceiver;
	
	/**
	 * Constructor
	 * @param application
	 * @param groupContextManager
	 */
	public BluewaveContextProvider(Context application, AndroidGroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		
		// Stores Important Variables
		this.application = application;
		this.gcm		 = groupContextManager;
		
		// Initializes 
		this.intentReceiver = new IntentReceiver();
		this.filter 		= new IntentFilter();
		this.filter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
	}

	@Override
	public void start() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started");
		application.registerReceiver(intentReceiver, filter);
	}

	@Override
	public void stop() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
		application.unregisterReceiver(intentReceiver);
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendContext() 
	{
		// ArrayList of Subscriptions
		ArrayList<String> subscriptions = new ArrayList<String>();
		
		for (ContextSubscriptionInfo csi : getSubscriptions())
		{
			subscriptions.add(csi.getDeviceID());
		}
		
		this.getGroupContextManager().sendContext(this.getContextType(), subscriptions.toArray(new String[0]), new String[] { "" });
	}
	
	public int getRefreshRate()
	{
		return 300000;
	}
	
	// Intent Receiver --------------------------------------------------------------------------
	private class IntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED))
			{
				onBluewaveContextReceived(context, intent);
			}
			else
			{
				Log.e("", "Unknown Action: " + intent.getAction());
			}
		}
		
		private void onBluewaveContextReceived(Context context, Intent intent)
		{
			// This is the Raw JSON from the Device
			String  json  = intent.getStringExtra(BluewaveManager.OTHER_USER_CONTEXT);
			boolean isNew = intent.getBooleanExtra(BluewaveManager.NEW_CONTEXT, true);
			
			// ArrayList of Subscriptions
			ArrayList<String> subscriptions = new ArrayList<String>();
			
			for (ContextSubscriptionInfo csi : getSubscriptions())
			{
				subscriptions.add(csi.getDeviceID());
			}
			
			if (subscriptions.size() > 0)
			{
				getGroupContextManager().sendContext(getContextType(), subscriptions.toArray(new String[0]), new String[] { "CONTEXT=" + json, "NEW=" + isNew });
			}
		}
	}
}
