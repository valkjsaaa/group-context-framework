package com.adefreitas.messages;

import java.util.ArrayList;

public class ContextRequest extends CommMessage
{	
	// Used to Denote the Type of Request
	public static final int LOCAL_ONLY      = 1;
	public static final int SINGLE_SOURCE   = 2;
	public static final int MULTIPLE_SOURCE = 3;
	public static final int SPECIFIC_SOURCE = 4;
	
	private String   		  contextType;
	private int		 		  requestType;
	private int 	 		  refreshRate;
	private ArrayList<String> parameters;	// Adrian:  Someday, you'll want to change this, but you're going to have to change GCM
	
	// Weights (Only Used by Single Source Arbiter)
	private double w_battery;
	private double w_sensorFitness;
	private double w_foreign;
	private double w_providing;
	private double w_reliability;
	
	/**
	 * Constructor
	 * @param deviceID
	 * @param contextType
	 * @param requestType
	 * @param refreshRate
	 * @param w_battery
	 * @param w_sensorFitness
	 * @param w_foreign
	 * @param w_providing
	 * @param w_reliability
	 * @param parameters
	 */
	public ContextRequest(String deviceID, String contextType, int requestType, int refreshRate, 
						  double w_battery, double w_sensorFitness, double w_foreign, double w_providing, 
						  double w_reliability, String[] parameters, String[] destination)
	{
		super("CREQ");
		this.deviceID        = deviceID;
		this.contextType     = contextType;
		this.requestType     = requestType;
		this.refreshRate     = refreshRate;
		this.w_battery       = w_battery;
		this.w_sensorFitness = w_sensorFitness;
		this.w_foreign 	  	 = w_foreign;
		this.w_providing 	 = w_providing;
		this.w_reliability 	 = w_reliability;
		
		// Creates Parameters
		this.parameters = new ArrayList<String>();
		
		// Sets Destination
		this.destination = destination;
		
		for (String parameter : parameters)
		{
			this.parameters.add(parameter);
		}
	}
	
	public String getContextType()
	{
		return contextType;
	}
	
	public int getRequestType()
	{
		return requestType;
	}

	public int getRefreshRate()
	{
		return refreshRate;
	}	
	
	public int getTimeoutDuration()
	{
		return Math.max(10000, refreshRate * 3);
	}

	public double getBatteryWeight()
	{
		return w_battery;
	}
	
	public double getSensorFitnessWeight()
	{
		return w_sensorFitness;
	}
	
	public double getForeignWeight()
	{
		return w_foreign;
	}
	
	public double getProvidingWeight()
	{
		return w_providing;
	}
	
	public double getReliabilityWeight()
	{
		return w_reliability;
	}
	
	public String[] getParameters()
	{
		return parameters.toArray(new String[0]);
	}
	
	public void setParameters(String[] newParameters)
	{
		parameters.clear();
		
		for (String parameter : newParameters)
		{
			this.parameters.add(parameter);
		}
	}
	
	public boolean equals(ContextRequest otherRequest)
	{
		return contextType.equals(otherRequest.getContextType()) && 
			   (requestType == otherRequest.getRequestType());
	}
	
	public String toString()
	{
		String parameterContents = "{";
		
		for (String parameter : parameters)
		{
			parameterContents += parameter + " ";
		}
		
		parameterContents += "}";
		
		return String.format("REQUEST [%s] (Device=%s; RequestType=%s; Refresh=%d):  %d bytes", contextType, deviceID, requestType, refreshRate, parameterContents.length());
	}
}