package com.adefreitas.messages;

import java.util.Arrays;

public class ContextData extends CommMessage
{
	// Serialization Labels (Needed for Android Intents)
	public static final String CONTEXT_TYPE = "CONTEXT_TYPE";
	public static final String DEVICE_ID    = "DEVICE_ID";
	public static final String DESCRIPTION  = "DESCRIPTION";
	public static final String VALUES       = "VALUES";
	
	// Attributes
	private String 	 contextType;
	private String   description;
	private String[] values;
	
	/**
	 * Constructor
	 * @param contextType
	 * @param deviceID
	 * @param description
	 * @param values
	 */
	public ContextData(String contextType, String deviceID, String description, String[] values)
	{
		super("CDAT");
		this.contextType = contextType;
		this.deviceID    = deviceID;
		this.description = description;
		this.destination = new String[0];
		this.values		 = values;
	}
	
	public ContextData(String contextType, String deviceID, String description, String[] destinations, String[] values)
	{
		this(contextType, deviceID, description, values);
		this.destination = destinations;
	}
	
	public String getContextType()
	{
		return contextType;
	}
		
	public String getDescription()
	{
		return description;
	}
	
	public String[] getValues()
	{
		//checkValues();
		
		return values;
	}

	public Double[] getValuesAsDoubles()
	{
		//checkValues();
		
		Double[] valuesAsDoubles = new Double[values.length];
		
		for (int i=0; i<values.length; i++)
		{
			try
			{
				valuesAsDoubles[i] = Double.parseDouble(values[i]);
			}
			catch (NumberFormatException e)
			{
				valuesAsDoubles[i] = Double.NaN;
			}
		}
		
		return valuesAsDoubles;
	}
	
	public Integer[] getValuesAsIntegers()
	{
		//checkValues();
		
		Integer[] valuesAsIntegers = new Integer[values.length];
		
		for (int i=0; i<values.length; i++)
		{
			try
			{
				valuesAsIntegers[i] = Integer.parseInt(values[i].trim());
			}
			catch (NumberFormatException e)
			{
				System.out.println(" PROBLEM PARSING: " + values[i] + " (" + values[i].length() + " characters)");

				for (int j=0; j<values[i].length(); j++)
				{
					System.out.print("  Char " + j + ": ");
					System.out.println((int)values[i].charAt(j));
				}
				
				System.out.println("Assigning default value");
				valuesAsIntegers[i] = Integer.MIN_VALUE;
			}
		}
		
		return valuesAsIntegers;
	}
	
	public String toString()
	{
		String result = String.format("DEVICE: %s; CONTEXT: %s; DESC: %s; DEST: %s; VALUES: %s", deviceID, contextType, description, Arrays.toString(destination), Arrays.toString(values));
		
		return result;
	}
}
