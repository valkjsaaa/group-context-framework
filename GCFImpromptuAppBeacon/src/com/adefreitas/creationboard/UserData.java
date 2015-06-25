package com.adefreitas.creationboard;

import java.util.Date;

public class UserData 
{
	private String deviceID;
	private String name;
	private String phone;
	private Date   lastEncountered;
	private String sensingDevice;
	
	public UserData(String deviceID, String name, String phone, String sensingDevice)
	{
		this.deviceID 		 = deviceID;
		this.name     		 = name;
		this.phone			 = phone;
		this.lastEncountered = new Date();
		this.sensingDevice   = sensingDevice;
	}
	
	public void updateLastEncountered(String sensingDevice)
	{
		this.lastEncountered = new Date();
		this.sensingDevice	 = sensingDevice;
	}
	
	public String getDeviceID()
	{
		return this.deviceID;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public String getPhone()
	{
		return phone;
	}
	
	public Date getLastEncounteredDate()
	{
		return this.lastEncountered;
	}
	
	public String getSensingDevice()
	{
		return sensingDevice;
	}

	public String toString()
	{
		return "Bluetooth Device: " + deviceID + "\n" +
			   "User Name: " + name + "\n" +
			   "Sensing Device: " + sensingDevice + "\n";
	}
}
