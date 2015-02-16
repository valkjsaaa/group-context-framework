package com.adefreitas.desktopframework;

import com.adefreitas.groupcontextframework.CommManager;
import com.adefreitas.messages.CommMessage;

public class DesktopCommManager extends CommManager implements MessageProcessor
{

	public DesktopCommManager(DesktopGroupContextManager gcm)
	{
		super(gcm);
	}

	@Override
	public String connect(CommMode commMode, String ipAddress, int port)
	{
		try
		{
			String socketKey = this.getCommThreadKey(commMode, ipAddress, port);
			
			// Generates the Correct Kind of Comm Thread
			if (!this.commThreads.containsKey(socketKey))
			{
				if (commMode == CommManager.CommMode.UDP_MULTICAST)
				{
					MulticastCommThread t = new MulticastCommThread(this, port, ipAddress, this);
					t.start();
					
					this.commThreads.put(socketKey, t);
				}
				else if (commMode == CommManager.CommMode.TCP)
				{
					TCPCommThread t = new TCPCommThread(this, this.getGroupContextManager().getDeviceID(), port, ipAddress, this);
					t.start();
					
					this.commThreads.put(socketKey, t);
				}
				else if (commMode == CommManager.CommMode.MQTT)
				{
					MqttCommThread t = new MqttCommThread(this, this.getGroupContextManager().getDeviceID(), ipAddress, port, this);
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
			}
			
			return socketKey;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return null;
	}

	@Override
	public void onMessage(CommMessage message)
	{		
		// All Messages Get Forwarded to the GCM
		if (this.getGroupContextManager() instanceof MessageProcessor)
		{
			MessageProcessor p = (MessageProcessor)this.getGroupContextManager();
			p.onMessage(message);
		}
	}

}
