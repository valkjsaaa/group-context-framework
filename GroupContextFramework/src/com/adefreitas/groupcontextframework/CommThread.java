package com.adefreitas.groupcontextframework;


import java.util.ArrayList;

import com.adefreitas.messages.CommMessage;

public abstract class CommThread extends Thread
{	
	private CommManager commManager;
	private String 		ipAddress;
	private int  		port;
	
	// DEBUG:  Tracks How Many Times a Comm Thread was Restarted
    protected int count = 0;
	
	private ArrayList<String> arp;
		
	/**
	 * Constructor
	 * @param commManager
	 */
	public CommThread(CommManager commManager)
	{
		this.commManager = commManager;
		this.arp		 = new ArrayList<String>();
	}
	
	/**
	 * Connects to the Specified IP address and Port
	 * @param ipAddress
	 * @param port
	 */
	public void connect(String ipAddress, int port)
	{
		this.ipAddress = ipAddress;
		this.port      = port;
		
		count++;
	}
	
	/**
	 * Closes the Connection
	 */
	public abstract void close();
	
	/**
	 * Sends a Message
	 * @param message
	 */
	public abstract void send(CommMessage message);

	/**
	 * Sends a Message to a Specific Channel
	 * @param message
	 * @param channel
	 */
	public void send(CommMessage message, String channel)
	{
		// DO NOTHING BY DEFAULT
	}
	
	/**
	 * Returns TRUE if this comm thread utilizes a channel mechanism
	 * @return
	 */
	public boolean supportsChannels()
	{
		return false;
	}
	
	/**
	 * Subscribe to a channel (e.g., MQTT topic).  Important:  Not all comm thread types support channels.  You need to check using the supportsChannels() method.
	 * @param channel
	 */
	public void subscribeToChannel(String channel)
	{
		// Do Nothing by Default
	}
	
	/**
	 * Unsubscribes from a channel.  Important:  Not all comm thread types support channels.  You need to check using the supportsChannels() method.
	 * @param channel
	 */
	public void unsubscribeToChannel(String channel)
	{
		// Do Nothing by Default
	}
	
	/**
	 * Returns TRUE if this comm thread is connected to the specified channel
	 * @param channel
	 * @return
	 */
	public boolean isSubscribedToChannel(String channel)
	{
		// By Default, Assume that the Channel is Not Subscribed
		// It is up to the developer to implement this for specific comm threads
		return false;
	}
	
	/**
	 * Accesses the Communications Manager that is Controlling this Thread
	 * @return
	 */
	public CommManager getCommManager()
	{
		return commManager;
	}
	
	/**
	 * Returns the IP Address that this comm thread is connected to.
	 * @return
	 */
	public String getIPAddress()
	{
		return ipAddress;
	}
	
	/**
	 * Returns the Port that this comm thread is connected to.
	 * @return
	 */
	public int getPort()
	{
		return port;
	}
	
	/**
	 * Adds a Device ID to the Comm Thread's ARP Table
	 * @param deviceID
	 */
	public void addToArp(String deviceID)
	{
		if (!arp.contains(deviceID))
		{
			System.out.println("Mapped " + deviceID + " to " + ipAddress + ":" + port);
			arp.add(deviceID);
		}
	}
	
	/**
	 * 
	 * @param deviceID
	 * @return
	 */
	public boolean receivesMessagesFrom(String deviceID)
	{
		return arp.contains(deviceID);
	}
	
	/**
	 * Removes all Illegal Characters from a String
	 * @param originalString
	 * @return
	 */
	public String processString(String originalString)
	{
		char[] result 		    = new char[originalString.length()];
		int    currentCharacter = 0;
			
		for (int c=0; c < originalString.length(); c++)
		{
			char character = originalString.charAt(c);
				
			if ((int)character > 0)
			{
				result[currentCharacter] = character;
				currentCharacter++;
			}
		}
		
		return String.valueOf(result).trim();
	}
}
