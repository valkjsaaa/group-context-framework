package com.adefreitas.desktopframework;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.adefreitas.groupcontextframework.BatteryMonitor;
import com.adefreitas.groupcontextframework.CommManager;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ContextCapability;
import com.adefreitas.messages.ContextData;
import com.adefreitas.messages.ContextRequest;

public class DesktopGroupContextManager extends GroupContextManager implements MessageProcessor
{
	private boolean DEBUG = false;
	
	private MessageProcessor messageProcessor;
	private RequestProcessor requestProcessor;
	private ScheduledTask    scheduledTimerTask;
	
	// Request Delay
	private static final int REQUEST_DELAY_TIME = 5000;
	
	public DesktopGroupContextManager(String deviceID, BatteryMonitor batteryMonitor, boolean promiscuous)
	{
		super(deviceID, GroupContextManager.DeviceType.Desktop, batteryMonitor, promiscuous);

		// Creates a Basic Communications Manager
		this.commManager = new DesktopCommManager(this);
		
		// Starts the Scheduled Events Thread
		scheduledTimerTask = new ScheduledTask();
		scheduledTimerTask.start();
	}
	
	public DesktopGroupContextManager(String deviceID, CommManager.CommMode commMode, String ipAddress, int port, BatteryMonitor batteryMonitor, boolean promiscuous) 
	{
		// Calls the Base Constructor
		this(deviceID, batteryMonitor, promiscuous);
		
		this.commManager.connect(commMode, ipAddress, port);
	}
	
	public DesktopGroupContextManager(String deviceID, CommManager.CommMode commMode, String ipAddress, int port, String channel, BatteryMonitor batteryMonitor, boolean promiscuous)
	{
		this(deviceID, batteryMonitor, promiscuous);
		
		String connectionKey = this.commManager.connect(commMode, ipAddress, port);
		
		this.commManager.subscribe(connectionKey, channel);
	}
	
	public void setDebug(boolean value)
	{
		DEBUG = value;
	}
	
	public void registerOnMessageProcessor(MessageProcessor processor)
	{
		this.messageProcessor = processor;
	}
	
	public void registerOnRequestProcessor(RequestProcessor processor)
	{
		this.requestProcessor = processor;
	}
	
	@Override
	protected void deliverDataToApp(ContextData data, ContextRequest request) 
	{
		if (messageProcessor != null)
		{
			messageProcessor.onMessage(data);
		}
	}

	@Override
	public void log(String category, String s) 
	{	
		if (DEBUG)
		{
			SimpleDateFormat sdf 	      = new SimpleDateFormat("MM-dd-yyyy @ HH:mm:ss");	
			String 			 outputString = sdf.format(new Date()) + ": [" + category + "]  " + s;
			
			System.out.println(outputString);	
		}
	}

	public void sendRequest(String type, int requestType, int refreshRate, String[] parameters)
	{
		super.sendRequest(type, requestType, refreshRate, parameters);
		scheduledTimerTask.interrupt();
	}

	public void sendRequest(String contextType, int requestType, String[] deviceIDs, int refreshRate, String[] parameters)
	{
		super.sendRequest(contextType, requestType, deviceIDs, refreshRate, parameters);
		scheduledTimerTask.interrupt();
	}
	
	@Override
	protected void onRequestReceived(ContextRequest request) {
		
	}
	
	public void cancelRequest(String type)
	{
		super.cancelRequest(type);
	}
	
	public void cancelRequest(String type, String deviceID)
	{
		super.cancelRequest(type, deviceID);
	}
	
	@Override
	protected void onCapabilitySubscribe(ContextCapability capability) {
		log("GCM-Sub", "Subscribing [" + capability.getContextType() + "]: " + capability.getDeviceID());
	}

	@Override
	protected void onCapabilityUnsubscribe(ContextCapability capability) {
		log("GCM-Sub", "Unsubscribing [" + capability.getContextType() + "]: " + capability.getDeviceID());
	}

	protected void onSendingData(ContextData data)
	{
		
	}
	
	protected void onSendingRequest(ContextRequest request) {
		if (requestProcessor != null)
		{
			requestProcessor.onSendingRequest(request);
		}
	};
	
	@Override
	protected void onRequestTimeout(ContextRequest request, ContextCapability capability) {
		log("GCM-Request", "Request timeout [" + request.getContextType() + "]: " + capability.getDeviceID());
	}

	@Override
	protected void onSubscriptionTimeout(ContextSubscriptionInfo subscription) {
		log("GCM-Sub","Subscription timeout [" + subscription.getContextType() + "]: " + subscription.getDeviceID());
	}

	@Override
	protected void onProviderSubscribe(ContextProvider provider) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void onProviderUnsubscribe(ContextProvider provider) {
		// TODO Auto-generated method stub
	}	
		
	class ScheduledTask extends Thread
	{
		private boolean run;
		private long    scheduledTaskDelay;
		
		public ScheduledTask()
		{
			run    = false;
		}
		
		public void run() 
		{	
			run = true;
			
			while (run)
			{
				try
				{
					// Runs all Scheduled Tasks ONCE
					scheduledTaskDelay = runScheduledTasks();
					long newDelayTime  = Math.max(Math.min(scheduledTaskDelay, REQUEST_DELAY_TIME), 0);
					
					// Lets the Thread Sleep
					sleep(newDelayTime);
				}
				catch (Exception ex)
				{
					System.out.println("Coming out of sleep . . . YAWN");
				}
			}	
		}
	}

	@Override
	protected void onCapabilityReceived(ContextCapability capability) {
		// TODO Auto-generated method stub
		
	}


}
