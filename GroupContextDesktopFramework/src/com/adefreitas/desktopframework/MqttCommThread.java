package com.adefreitas.desktopframework;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.adefreitas.groupcontextframework.CommManager;
import com.adefreitas.groupcontextframework.CommThread;
import com.adefreitas.messages.CommMessage;
import com.google.gson.Gson;

public class MqttCommThread extends CommThread implements MqttCallback
{
	private boolean DEBUG = false;
	private int 	QOS	  = 0;
	
	private String 			 serverIP;
	private int 			 port;
    private Gson 		     gson;
	private MessageProcessor processor;
	
	private MqttClient 		  client;
	private ArrayList<String> channels;
	private String 	   		  deviceID;
    
    private boolean run;
	
    /**
     * Constructor
     * @param port		- the port
     * @param serverIP	- the IP address of the destination machine (the TCP relay)
     * @param processor - where fully formed messages should be delivered once assembled
     */
	public MqttCommThread(CommManager commManager, String deviceID, String serverIP, int port, MessageProcessor processor)  
	{
		super(commManager);
		
		this.deviceID  = deviceID;
	    gson 	   	   = new Gson();
	    this.processor = processor;
	    this.channels  = new ArrayList<String>();
	    this.run	   = false;
	    
	    // Repeatedly Tries to Connect
		connect(serverIP, port);
    }
    
	/**
	 * Repeatedly Listens for Packets as they Arrive
	 */
    public void run()
    {
    	run = true;
    	
    	try
    	{
    		while (run)
    		{
    			if (client != null && !client.isConnected())
    			{
    				sleep(2000);
    				connect(serverIP, port);
    			}
    			else
    			{
    				sleep(30000);
    				System.out.println("MQTT Channels: " + serverIP + ":" + port + ":" + Arrays.toString(channels.toArray(new String[0])));
    			}
    		}
    	}
    	catch (Exception ex)
    	{
    		ex.printStackTrace();
    	}
    }
	
	/**
	 * Repeatedly attempts to connect to the destination
	 * @param serverIP - the IP address of the destination machine (the TCP relay?)
	 * @param port 	   - the port
	 */
	public void connect(String serverIP, int port)
	{				
		super.connect(serverIP, port);
		
		System.out.print("Attempting to connect to " + serverIP + ":" + port + " . . . ");
		
		// Tracking the Port and IP Address
		this.port      = port;
		this.serverIP  = serverIP;
		
		try
		{
			MqttConnectOptions options = new MqttConnectOptions();
			options.setUserName(deviceID);
			options.setKeepAliveInterval(0);
			options.setCleanSession(true);
			
			client = new MqttClient("tcp://" + serverIP + ":" + port, "mqtt_" + new Date().getTime(), null);
			client.connect(options);
			client.setCallback(this);
			
			// Creates a Backup List
			ArrayList<String> previousChannels = new ArrayList<String>(channels);
			channels.clear();
			
			// Reconnects to Previously Connected Channels
			for (String channel : previousChannels)
			{
				subscribeToChannel(channel);
			}
			
			System.out.println("SUCCESS!");
		}
		catch (Exception ex)
		{
			System.out.println("FAILED!");
			ex.printStackTrace();
		}
	}
		
	/**
	 * Closes the Socket Connection
	 */
    public void close() 
    {
    	try
    	{
    		client.disconnect();
    	}
    	catch (Exception ex)
    	{
    		ex.printStackTrace();
    	}
    	
    	run = false;
    }
	
    /**
     * Sends a Communications Message to the MQTT Destination
     */
	public void send(CommMessage message)
    {
		if (message != null)
		{
			for (String channel : channels)
			{
			    send(channel, gson.toJson(message));		
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
			client.getTopic(channel).publish(msg);
		}
		catch (Exception e)
		{
			connect(serverIP, port);
			e.printStackTrace();
		}
    }
    
    // PAHO Methods
	@Override
	public void connectionLost(Throwable arg0)
	{
		arg0.printStackTrace();
		
		System.out.println("MQTT Connection Lost.  Reconnecting.");
		connect(serverIP, port);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0)
	{
		// TODO Auto-generated method stub
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception
	{
		String 	    s   = new String(message.getPayload(), "UTF-8");
		CommMessage msg = CommMessage.jsonToMessage(s);
		
		if (processor != null && msg != null)
		{
			// Allows this Thread to Track WHO it has Seen Messages From
			this.addToArp(msg.getDeviceID());
			
			processor.onMessage(msg);
		}	
	}

	// Overridding Methods
	public boolean supportsChannels()
	{
		return true;
	}
	
	public void subscribeToChannel(String channel)
	{
		try
		{
			if (!channels.contains(channel))
			{
				System.out.println("MQTT Subscribing to " + channel);
				channels.add(channel);
				client.subscribe(channel);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void unsubscribeToChannel(String channel)
	{
		try
		{
			if (channels.contains(channel))
			{
				System.out.println("MQTT Unsubscribing to " + channel);
				channels.remove(channel);
				client.unsubscribe(channel);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public boolean isSubscribedToChannel(String channel)
	{
		return channels.contains(channel);
	}
	
}
