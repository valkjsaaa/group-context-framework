package com.adefreitas.magicappserver;

import android.widget.Toast;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.liveos.ApplicationProvider;
import com.adefreitas.messages.CommMessage;

public abstract class AndroidApplicationProvider extends ApplicationProvider
{
	private GCFApplication application;
	
	/**
	 * Constructor
	 * @param application
	 * @param groupContextManager
	 * @param contextType
	 * @param name
	 * @param description
	 * @param contextsRequired
	 * @param preferencesToRequest
	 * @param logoPath
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 */
	public AndroidApplicationProvider(GCFApplication application,
			GroupContextManager groupContextManager,
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
		super(groupContextManager, contextType, name, description, category, contextsRequired, preferencesToRequest, logoPath, lifetime, commMode, ipAddress, port);
	
		this.application = application;
	}

	/**
	 * Returns a Reference to the Core Application
	 * @return
	 */
	public GCFApplication getApplication()
	{
		return application;
	}

	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		// Sends the UI Immediately
		//sendMostRecentReading();
		
		// Determines Credentials
//		String username = CommMessage.getValue(newSubscription.getParameters(), "credentials");
//		Toast.makeText(application, "Subscription: " + username, Toast.LENGTH_SHORT);
	}
	
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		// Determines Credentials
		//String username = CommMessage.getValue(subscription.getParameters(), "credentials");
		//Toast.makeText(application, "Unsubscription: " + username, Toast.LENGTH_SHORT);
	}
}
