package com.adefreitas.messages;

import java.util.Arrays;

public class ComputeInstruction extends CommMessage
{
	private String   contextType;
	private String   command;
	private String[] parameters;
	
	public ComputeInstruction(String contextType, String sourceDeviceID, String[] destination, String command, String[] parameters) 
	{
		super("COMP");
		this.contextType = contextType;
		this.deviceID    = sourceDeviceID;
		this.destination = destination;
		this.command 	 = command;
		this.parameters  = parameters;
	}
	
	public String getContextType()
	{
		return contextType;
	}
			
	public String getCommand()
	{
		return command;
	}
	
	public String[] getParameters()
	{
		return parameters;
	}
	
	public void setParameters(String[] newParameters)
	{
		this.parameters = newParameters;
	}

	public String toString()
	{
		return String.format("DEVICE: %s; CONTEXT: %s; COMMAND: %s; VALUES: %s", deviceID, contextType, command, Arrays.toString(parameters));
	}
}
