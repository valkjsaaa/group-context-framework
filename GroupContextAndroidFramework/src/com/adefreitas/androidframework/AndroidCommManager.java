package com.adefreitas.androidframework;

import java.lang.ref.WeakReference;

import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.adefreitas.groupcontextframework.CommManager;
import com.adefreitas.groupcontextframework.CommThread;
import com.adefreitas.messages.CommMessage;

public class AndroidCommManager extends CommManager
{
	// Static Intents
	public static final String ACTION_COMMTHREAD_CONNECTED = "comm_connected";
	public static final String ACTION_CHANNEL_SUBSCRIBED   = "comm_subscribed";
	
	private CommHandler    commHandler;  // Used by the Communications Thread to Deliver Messages Received
	private ContextWrapper cw;
	
	public AndroidCommManager(AndroidGroupContextManager gcm, ContextWrapper cw)
	{
		super(gcm);

		commHandler = new CommHandler(gcm, cw);
		this.cw     = cw;
	}

	@Override
	public String connect(CommMode commMode, String ipAddress, int port)
	{
		// Closes the Comm Thread if it Already Exists	
		try
		{
			String socketKey = this.getCommThreadKey(commMode, ipAddress, port);
			
			// Generates the Correct Kind of Comm Thread
			if (this.getCommThread(socketKey) == null)
			{
				if (commMode == CommMode.UDP_MULTICAST)
				{
					MulticastCommThread t = new MulticastCommThread(this, this.getGroupContextManager().getDeviceID(), port, ipAddress, commHandler);
					t.start();
					
					this.commThreads.put(socketKey, t);
				}
				else if (commMode == CommMode.TCP)
				{
					TCPCommThread t = new TCPCommThread(this, this.getGroupContextManager().getDeviceID(), port, ipAddress, commHandler);
					t.start();
					
					this.commThreads.put(socketKey, t);
				}
				else if (commMode == CommMode.MQTT)
				{
					MqttCommThread t = new MqttCommThread(this, this.getGroupContextManager().getDeviceID(), ipAddress, port, commHandler, cw);
					t.start();
					
					this.commThreads.put(socketKey, t);
				}
				else
				{
					System.out.println("Communications Mode Not Recognized " + commMode);
				}	
			}
			else
			{
				System.out.println("Socket " + socketKey + " Already Created");
				this.notifyConnected(this.getCommThread(socketKey));
			}
			
			return socketKey;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return null;
	}
	
	public void notifyConnected(CommThread ct)
	{
		Intent intent = new Intent(ACTION_COMMTHREAD_CONNECTED);
		intent.putExtra("IP_ADDRESS", ct.getIPAddress());
		intent.putExtra("PORT", ct.getPort());
		cw.getBaseContext().sendBroadcast(intent);
	}
	
	public void notifySubscribed(CommThread ct, String channel)
	{
		Intent intent = new Intent(ACTION_CHANNEL_SUBSCRIBED);
		intent.putExtra("CHANNEL", channel);
		cw.getBaseContext().sendBroadcast(intent);
	}
	
	/**
	 * This Class Handles Messages Returned from the Comm Threads 
	 * @author adefreit
	 */
	static class CommHandler extends Handler
	{
		private final WeakReference<AndroidGroupContextManager> gcmReference; 

		public CommHandler(AndroidGroupContextManager gcm, ContextWrapper cw) 
		{
			gcmReference = new WeakReference<AndroidGroupContextManager>(gcm);
			//cwReference  = new WeakReference<ContextWrapper>(cw);
	    }
		
		public void handleMessage(Message msg)
		{
			AndroidGroupContextManager gcm = gcmReference.get();
			
			if (msg.obj instanceof CommMessage)
			{
				CommMessage message = (CommMessage)msg.obj;				
				gcm.onMessage(message);	
			}
		}
	}
}
