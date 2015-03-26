package com.adefreitas.androidframework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.groupcontextframework.CommManager;
import com.adefreitas.groupcontextframework.CommThread;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;
import com.google.gson.Gson;

public class MqttCommThread extends CommThread implements MqttCallback
{
	private static final String  LOG_NAME = "MqttCommThread";
	private static final boolean DEBUG    = false; 
	
	// No MQTT Messages are Ever Expected to be Guaranteed to Arrive
	private static final int QOS = 0;
	
	private String  serverIP;
	private int     port;
    private Gson    gson;
    private Handler commHandler;
    
    private MqttClient     	  client;
    private ArrayList<String> channels;
    private String         	  deviceID;
    private ContextWrapper    cw;
    
    ArrayList<MqttMessage>  sendBuffer = new ArrayList<MqttMessage>();
    HashMap<String, String> channelARP = new HashMap<String, String>();
    
    private boolean run;
    private int 	disconnectCount = 0;
    
    /**
     * Constructor
     * @param deviceID
     * @param serverIP
     * @param port
     * @param commHandler
     * @param cw
     */
    public MqttCommThread(CommManager commManager, String deviceID, String serverIP, int port, Handler commHandler, ContextWrapper cw)  
	{
    	super(commManager);
    	
		this.deviceID    = deviceID;
	    gson 	   	     = new Gson();
	    this.commHandler = commHandler;
	    this.cw 		 = cw;
	    this.channels	 = new ArrayList<String>();
	    this.run         = false;
	    
	    // Repeatedly Tries to Connect
		connect(serverIP, port);
    }
        
    public void run()
    {    	    	
    	try
    	{
    		while (run)
    		{
    			if (run && client != null && !client.isConnected())
    			{
    				sleep(2000);
    				connect(serverIP, port);
    			}
    			else
    			{
    				sleep(30000);
    				
    				if (DEBUG)
    				{
        				Log.d(LOG_NAME, "MQTT Channels: " + serverIP + ":" + port + ":" + Arrays.toString(channels.toArray(new String[0])) + " " + this.count + " attempt(s); " + this.disconnectCount + " disconnect(s).");	
    				}
    			}
    		}
    		
    		Log.d(LOG_NAME, "MQTT Channels: " + serverIP + ":" + port + ":" + Arrays.toString(channels.toArray(new String[0])) + " TERMINATED.");
    	}
    	catch (Exception ex)
    	{
    		ex.printStackTrace();
    		close();
    	}
    }
    
	@Override
	public void connect(final String serverIP, final int port)
	{			
		super.connect(serverIP, port);
		
		close();
		
    	run = true;
		
		// Tracking the Port and IP Address
		this.port      = port;
		this.serverIP  = serverIP;
		this.gson      = new Gson();

		Log.d(LOG_NAME, "Attempting to connect to " + serverIP + ":" + port + " . . . ");
		
		// Creates a Networking Thread to Establish the Connection
		Thread connectThread = new Thread()
		{
			public void run()
			{
				try
				{
		    		Log.d(LOG_NAME, "Attempting to connect to " + serverIP + ":" + port + " . . . ");
		    		
					MqttConnectOptions options = new MqttConnectOptions();
					options.setUserName(deviceID);
					options.setKeepAliveInterval(120);
					options.setCleanSession(true);
					
					client = new MqttClient("tcp://" + serverIP + ":" + port, "mqtt_" + new Date().getTime(), null);
					client.connect(options);
					client.setCallback(MqttCommThread.this);
					
					if (client.isConnected())
					{
						// Creates a Backup List of Channels
						ArrayList<String> previousChannels = new ArrayList<String>(channels);
						channels.clear();
						
						// Reconnects to Previously Connected Channels
						for (String channel : previousChannels)
						{
							Log.d(LOG_NAME, "Reconnecting to: " + channel);
							subscribeToChannel(channel);
						}
						
						// Reports Success
						Log.d(LOG_NAME, "SUCCESS");
						
						// Reports that the Connection was Successful
						((AndroidCommManager)(getCommManager())).notifyConnected(MqttCommThread.this);	
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					Log.d(LOG_NAME, "FAILURE!");
					
					if (client != null)
					{
						close();
					}
				}		
			}
		};
		connectThread.start();
	}
	
	@Override
	public void close()
	{
		// Stops the Maintenance Thread
		run = false;
		
		try
    	{
			if (client != null)
			{
				client.disconnect();
	    		client.close();
	    		client = null;	
			}
		}
    	catch (Exception ex)
    	{
    		ex.printStackTrace();
    	}
	}

	// Communication Methods
	@Override
	public void send(CommMessage message)
	{
		ArrayList<String> channelsToSend = new ArrayList<String>();
		boolean 		  broadcast      = message.getDestination().length == 0;
		
		// Looks for Channels for Specific Destinations
		for (String deviceID : message.getDestination())
		{
			if (channelARP.containsKey(deviceID) && !channelsToSend.contains(channelARP.get(deviceID)))
			{
				channelsToSend.add(channelARP.get(deviceID));
			}
			else
			{
				broadcast = true;
				break;
			}
		}

		// TODO:  Evaluate whether or not this is actually needed
		broadcast = broadcast || channelsToSend.size() == 0;
		
		// Determines Whether to Broadcast the Message or to Only Send it to Select Channels
		if (broadcast)
		{
			for (String channel : channels)
			{
				send(channel, gson.toJson(message));	
			}
		}
		else
		{
			if (channelsToSend.size() == 0)
			{
				Log.d(LOG_NAME,  "No channels found for " + message.toString());
			}
			else
			{
				for (String channel : channelsToSend)
				{
					send(channel, gson.toJson(message));
				}	
			}
		}
	}
	
	public void send(CommMessage message, String channel)
	{
		if (message != null)
		{
			send(channel, gson.toJson(message));		
		}
	}

	private void send(String channel, String message)
	{
		try
		{
			MqttMessage msg = new MqttMessage();
			msg.setPayload(message.getBytes());
			msg.setQos(QOS);
			msg.setRetained(false);
			sendBuffer.add(msg);
			
			if (sendBuffer.size() > 5)
			{
				Log.d(LOG_NAME,  "Buffer exceeding max size.  Removing the oldest message.");
				sendBuffer.remove(0);
			}
			
			if (client != null && client.isConnected())
			{
				for (MqttMessage m : sendBuffer)
				{
					Log.d(LOG_NAME,  "Sending " + message.length() + " bytes to channel " + channel);
					client.getTopic(channel).publish(m);
				}
		
				sendBuffer.clear();
			}
			else
			{
				Log.d(LOG_NAME,  "Buffering " + message.length() + " bytes.");
				close();
				connect(serverIP, port);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
   	
	// PAHO Methods
	@Override
	public void connectionLost(Throwable arg0)
	{
		Toast.makeText(cw, "MQTT Connection Lost: " + serverIP + ":" + port, Toast.LENGTH_SHORT).show();
		close();
		
		disconnectCount++;
		connect(serverIP, port);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken t)
	{

	}

	@Override
	public void messageArrived(String channel, MqttMessage message) throws Exception
	{
		String 	    s   = new String(message.getPayload(), "UTF-8");
		CommMessage msg = CommMessage.jsonToMessage(s);
				
		if (commHandler != null && msg != null)
		{			
			if (!(msg instanceof ComputeInstruction))
			{
				// Allows this Thread to Track WHO it has Seen Messages From
				this.addToArp(msg.getDeviceID());
				
				// Allows this Thread to Track WHICH CHANNEL a Device is On
				channelARP.put(msg.getDeviceID(), channel);
				Log.d(LOG_NAME, "Associating " + msg.getDeviceID() + " with channel " + channel);
			}
			
			Message m = commHandler.obtainMessage();
			m.obj = msg;
			commHandler.sendMessage(m);
		}
	}
	
	// Channel Methods
	public boolean supportsChannels()
	{
		return true;
	}
	
	public void subscribeToChannel(final String channel)
	{	
		Thread subscribeThread = new Thread()
		{
			boolean success = false;
			int 	i		= 1;
			
			public void run()
			{
				try
				{
					if (channel != null && !channels.contains(channel))
					{
						while (!success)
						{
							// Makes Sure the Client it Good to Go Before Officially Subscribing
							if (client != null && client.isConnected())
							{
								Log.d(LOG_NAME, "ATTEMPT " + i + ":  MQTT subscribing to: " + channel);
								channels.add(channel);
								client.subscribe(channel);
								
								// Reports that the Subscription was Successful
								((AndroidCommManager)(getCommManager())).notifySubscribed(MqttCommThread.this, channel);
								
								success = true;
								break;
							}
							else
							{
								Log.d(LOG_NAME, "ATTEMPT " + i + ":  Could Not Subscribe to " + channel + "; Client=" + client);
								sleep(1000);
								//connect(serverIP, port);
							}	
							
							i++;
						}
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}	
			}
		};
		
		subscribeThread.start();
	}
	
	public void unsubscribeToChannel(final String channel)
	{
		Thread unsubscribeThread = new Thread()
		{
			boolean success = false;
			int     count	= 1;
			
			public void run()
			{
				try
				{
					if (channel != null && channels.contains(channel))
					{
						while (!success)
						{
							// Makes Sure the Client it Good to Go Before Officially Subscribing
							if (client != null && client.isConnected())
							{
								Log.d(LOG_NAME, "ATTEMPT " + count + ":  MQTT unsubscribing from channel: " + channel);
								client.unsubscribe(channel);	
								channels.remove(channel);
								
								// Cleaning the channelARP
								HashMap<String, String> newARP = new HashMap<String, String>(channelARP);
								
								for (String devID : channelARP.keySet())
								{
									if (newARP.get(devID).equals(channel))
									{
										newARP.remove(devID);
										Log.d(LOG_NAME, "Disassociating " + devID + " with channel " + channel);
									}
								}
								
								// Replaces the Old ChannelARP with a New Version
								channelARP = newARP;
								
								success = true;
							}
							else
							{
								Log.d(LOG_NAME, "ATTEMPT " + count + ":  Could Not Unsubscribe from " + channel);
								sleep(100);
							}
							
							count++;
						}
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}				
			}
		};
		
		unsubscribeThread.start();
	}
	
	public boolean isSubscribedToChannel(String channel)
	{
		return channels.contains(channel);
	}

}
