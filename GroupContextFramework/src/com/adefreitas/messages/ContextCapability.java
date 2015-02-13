package com.adefreitas.messages;

import java.util.HashMap;
import java.util.Locale;

public class ContextCapability extends CommMessage
{
	private String  				deviceType;
	private String  				contextType;
	private int 					heartbeatRate;
	private double 					batteryLife;
	private double  				sensorFitness;
	private boolean 				alreadyProviding;
	private HashMap<String, String> parameters;

	public ContextCapability(String deviceID, String deviceType, String contextType, int heartbeatRate, double batteryLife, double sensorFitness, boolean alreadyProviding, String[] destination) 
	{
		super("CCAP");
		this.deviceID 		  = deviceID;
		this.deviceType 	  = deviceType;
		this.contextType 	  = contextType;
		this.heartbeatRate    = heartbeatRate;
		this.batteryLife 	  = batteryLife;
		this.sensorFitness 	  = sensorFitness;
		this.alreadyProviding = alreadyProviding;
		this.parameters 	  = new HashMap<String, String>();
		this.destination	  = destination;
	}
		
	public String getDeviceType()
	{
		return deviceType;
	}
	
	public String getContextType()
	{
		return contextType;
	}
	
	public int getHeartbeatRate()
	{
		return heartbeatRate;
	}
	
	public double getBatteryLife()
	{
		return batteryLife;
	}
	
	public double getSensorFitness()
	{
		return sensorFitness;
	}
	
	public boolean isAlreadyProviding()
	{
		return alreadyProviding;
	}

	public void setParameter(String name, String value)
	{
		parameters.put(name, value);
	}
	
	public String getParameter(String name)
	{
		if (parameters.containsKey(name))
		{
			return parameters.get(name);
		}
		else
		{
			return null;
		}
	}
	
	public String toString()
	{
		String batteryPercent = (parameters.containsKey("Battery")) ? getParameter("Battery") : "UNK";
		
		return String.format(Locale.getDefault(), "CAPABILITY: %s (%s) [Device=%s Bat=%1.2f (%s) Fitness=%1.2f Providing=%s]", 
				contextType, deviceID,	deviceType, batteryLife, batteryPercent, sensorFitness, alreadyProviding);
	}
}
