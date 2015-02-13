package com.adefreitas.androidframework;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.groupcontextframework.BatteryMonitor;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ContextCapability;
import com.adefreitas.messages.ContextData;
import com.adefreitas.messages.ContextRequest;

public class AndroidGroupContextManager extends GroupContextManager
{
	private boolean DEBUG = true;
	
	// Intent Labels
	public static final String ACTION_GCF_DATA_RECEIVED 	= "ACTION_GCF_DATA_RECEIVED";
	public static final String ACTION_GCF_OUTPUT			= "ACTION_GCF_OUTPUT";
	public static final String GCF_OUTPUT					= "GCF_OUTPUT";
	public static final String ACTION_PROVIDER_SUBSCRIPTION = "GCF_CONTEXT_PROVIDER_SUBSCRIBED";
	
	// Constants
	private static final int REQUEST_DELAY = 2000;
	
	// Keeps a Link to the Context Wrapper (so we can send intent broadcasts)
	private ContextWrapper cw;
	
	// Handlers (Android Specific)
	private TimerHandler timerHandler; // Used by the GCM to Performed Scheduled Tasks
	
	/**
	 * Alternate Constructor (no active communications)
	 * @param cw
	 * @param deviceID
	 * @param batteryMonitor
	 * @param promiscuous
	 */
	public AndroidGroupContextManager(ContextWrapper cw, String deviceID, BatteryMonitor batteryMonitor, boolean promiscuous)
	{
		// Calls the Parent Class Constructor
		super(deviceID, GroupContextManager.DeviceType.Mobile, batteryMonitor, promiscuous);
						
		// Saves a Link to the Activity (to pass intents)
		this.cw = cw;
		
		// Creates the Comm Manager
		this.commManager = new AndroidCommManager(this, cw);
		
		// Creates the Scheduled Event Timer	
		timerHandler = new TimerHandler(this);
	}
	
	/**
	 * Constructor
	 * @param cw
	 * @param deviceID
	 * @param communicationsMode
	 * @param ipAddress
	 * @param port
	 * @param batteryMonitor
	 * @param promiscuous
	 */
	public AndroidGroupContextManager(ContextWrapper cw, String deviceID, CommMode communicationsMode, String ipAddress, int port, BatteryMonitor batteryMonitor, boolean promiscuous) 
	{
		this(cw, deviceID, batteryMonitor, promiscuous);

		connect(communicationsMode, ipAddress, port);
	}
	
	public void sendRequest(String type, int requestType, int refreshRate, String[] parameters)
	{
		super.sendRequest(type, requestType, refreshRate, parameters);
		
		// Initiates the Timer Delay
		if (timerHandler != null)
		{
			timerHandler.start(REQUEST_DELAY);
		}
	}
	
	public void sendRequest(String contextType, String[] deviceIDs, int refreshRate, String[] parameters)
	{
		super.sendRequest(contextType, deviceIDs, refreshRate, parameters);
		
		// Initiates the Timer Delay
		if (timerHandler != null)
		{
			timerHandler.start(REQUEST_DELAY);
		}
	}
	
	public void sendRequest(String type, int requestType, int refreshRate, double w_battery, double w_sensorFitness, double w_foreign, double w_providing, double w_reliability, String[] parameters)
	{
		super.sendRequest(type, requestType,  refreshRate,  w_battery, w_sensorFitness, w_foreign, w_providing, w_reliability, parameters, null);
		
		if (timerHandler != null)
		{
			timerHandler.start(REQUEST_DELAY);
		}
	}
	
	public void cancelRequest(String type)
	{
		super.cancelRequest(type);
		
		if (timerHandler != null)
		{
			timerHandler.start(0);			
		}
	}
	
	public void cancelRequest(String type, String deviceID)
	{
		super.cancelRequest(type, deviceID);
		
		if (timerHandler != null)
		{
			timerHandler.start(0);			
		}
	}

//	public void sendComputeInstruction(CommMode mode, String ipAddress, int port, String contextType, String[] destination, String command, String[] instructions)
//	{		
//		try
//		{
//			ComputeInstruction instruction = new ComputeInstruction(contextType, getDeviceID(), destination, command, instructions);
//			
//			commManager.send(mode, ipAddress, port, instruction);
//		}
//		catch (Exception ex)
//		{
//			ex.printStackTrace();
//		}
//	}
	
	public void setDebug(boolean newDebug)
	{
		DEBUG = newDebug;
	}
	
	public void setDeviceID(String newDeviceID)
	{
		super.setDeviceID(newDeviceID);
	}
	
	// OUTPUT METHODS -------------------------------------------------------------------------------------
	@Override
	protected void deliverDataToApp(ContextData data, ContextRequest request) 
	{
		// Creates the Intent
		Intent dataDeliveryIntent = new Intent(ACTION_GCF_DATA_RECEIVED);
		
		// Populates the Intent
		dataDeliveryIntent.putExtra(ContextData.CONTEXT_TYPE, data.getContextType());
		dataDeliveryIntent.putExtra(ContextData.DEVICE_ID, data.getDeviceID());
		dataDeliveryIntent.putExtra(ContextData.DESCRIPTION, data.getDescription());
		dataDeliveryIntent.putExtra(ContextData.VALUES, data.getValues());
		
		// Sends the Intent
		cw.sendBroadcast(dataDeliveryIntent);
	}

	@Override
	public void log(String category, String message) 
	{
		if (DEBUG)
		{
			if (category.equalsIgnoreCase(LOG_TIMEOUT) || category.equalsIgnoreCase(LOG_ERROR))
			{
				Log.e(category, message);
			}
			else if (category.equalsIgnoreCase(LOG_PERFORMANCE))
			{
				Log.i(category, message);
				this.print(message);
			}
			else if (category.equalsIgnoreCase(LOG_COMPARISON))
			{
				Log.i(category, message);
			}
			else
			{
				Log.d(category, message);
			}	
		}
	}

	public void print(String s) 
	{
		// Creates the Intent
		Intent printIntent = new Intent(ACTION_GCF_OUTPUT);
		
		// Populates the Intent
		printIntent.putExtra(GCF_OUTPUT, s);
		
		// Sends the Intent
		cw.sendBroadcast(printIntent);
	}

	// PRIVATE METHODS
	private void createNotification(String title, String subtitle)
	{
		boolean success = false;
		
		// Tries to Create a Notification using the OS Tools
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) 
		{
			//Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			
			 Intent 			  intent 			  = new Intent(ACTION_PROVIDER_SUBSCRIPTION);
			 PendingIntent 		  pendingIntent 	  = PendingIntent.getActivity(cw, 0, intent, 0);
			 NotificationManager  notificationManager = (NotificationManager)cw.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
			 Notification.Builder builder 			  = new Notification.Builder(cw)
			 	.setSmallIcon(com.adefreitas.groupcontextandroidframework.R.drawable.gcf)
			 	.setContentTitle(title)
			 	.setContentText(subtitle)
			 	.setAutoCancel(true)
			 	//.setSound(soundUri)
			 	.setContentIntent(pendingIntent);
			 
			 if (builder != null && notificationManager != null)
			 {
				 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				 {
					 notificationManager.notify(0, builder.getNotification());
				 }
				 else
				 {
					 notificationManager.notify(0, builder.build());
				 }
				 
				 success = true;
			 }
		}
		
		// Creates a Generic Toast if No Notification Can Be Created
		if (!success)
		{
			Toast.makeText(cw, title + ": " + subtitle, Toast.LENGTH_LONG).show();
		}
	}
	
	private void createSubscriptionNotification()
	{
		String providerStatus = "";
		
		for (ContextProvider p : this.getRegisteredProviders())
		{
			if (p.getNumSubscriptions() > 0)
			{
				providerStatus += String.format("[%s]: %d  ", p.getContextType(), p.getNumSubscriptions());	
			}
		}
		
		providerStatus = (providerStatus.length() == 0) ? "No Subscriptions" : providerStatus;
		
		createNotification("GCF Subscription Update", providerStatus);
	}

	// GCM Events -----------------------------------------------------------------------------------------
	protected boolean allowCapabilitySubscribe(ContextCapability capability)
	{
		return true;
	}
	
	@Override
	protected void onCapabilityReceived(ContextCapability capability) {
		// TODO Auto-generated method stub	
	}
	
	@Override
	protected void onCapabilitySubscribe(ContextCapability capability) {
		print("Subscribing to " + capability.getDeviceID() + " [" + capability.getContextType() + "]");
	}

	@Override
	protected void onCapabilityUnsubscribe(ContextCapability capability) {
		print("Unsubscribing from " + capability.getDeviceID() + " [" + capability.getContextType() + "]");
	}

	protected void onSendingData(ContextData data)
	{
		
	}
	
	@Override
	protected void onRequestTimeout(ContextRequest request, ContextCapability capability) {
		print("REQ TIMEOUT: " + capability.getDeviceID() + " [" + capability.getContextType() + "]");
	}

	@Override
	protected void onSubscriptionTimeout(ContextSubscriptionInfo subscription) {
		print("SUB TIMEOUT: " + subscription.getDeviceID());
	}

	@Override
	protected void onProviderSubscribe(ContextProvider provider) {
		String message = "SUB:  Context Provider " + provider.getContextType() + " has " + provider.getNumSubscriptions() + " subscriptions.";
		print(message);
		
		// Creates a Notification to Say How Many Subscriptions are In Progress
		createSubscriptionNotification();
		
		if (timerHandler != null)
		{
			timerHandler.start(0);
		}
	}

	@Override
	protected void onProviderUnsubscribe(ContextProvider provider) {
		String message = "UNSUB:  Context Provider " + provider.getContextType() + " has " + provider.getNumSubscriptions() + " subscriptions.";
		print(message);
		
		// Creates a Notification to Say How Many Subscriptions are In Progress
		createSubscriptionNotification();
	}
		
	/**
	 * This Class Allows the GCM to Run its Scheduled Tasks
	 * @author adefreit
	 */
	static class TimerHandler extends Handler
	{
		private Runnable 				   scheduledTask;
		private AndroidGroupContextManager gcm;
		//private boolean 				   running;
		
		public TimerHandler(AndroidGroupContextManager gcm)
		{
			this.gcm 							  = gcm;
			final AndroidGroupContextManager agcm = gcm;
			
			scheduledTask = new Runnable() 
			{
				public void run() 
				{ 	
					// Runs all Scheduled Tasks ONCE
					long delayTime = agcm.runScheduledTasks();
					
					agcm.log("GCM-ScheduledTask", "Next Scheduled Task in " + delayTime + " ms");
					//agcm.print("Next Scheduled Task in " + delayTime + " ms");
					postDelayed(this, delayTime);
				}
			};
		}
		
		public void start(long delayTime)
		{
			// Stops Any Existing Delays
			stop();
			
			// Creates the Next Task Instance
			postDelayed(scheduledTask, delayTime);
		}
		
		public void stop()
		{
			removeCallbacks(scheduledTask);	
		}
	}

}
