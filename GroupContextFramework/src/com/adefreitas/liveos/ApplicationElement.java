package com.adefreitas.liveos;

import com.google.gson.Gson;

public abstract class ApplicationElement 
{
	private String name;

	public ApplicationElement(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String toJSON()
	{
		try
		{
			Gson gson = new Gson();
			return gson.toJson(this);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
	
	public static String toJSONArray(ApplicationElement[] elements)
	{
		String result = "[";
		
		for (int i=0; i<elements.length; i++)
		{
			// Adds the JSON String
			result += elements[i].toJSON();
			
			// Adds a Comma to Each Element
			if (i < elements.length - 1)
			{
				result += ",";
			}
		}
		
		// Adds the Final Bracket
		result += "]";
		
		return result;
	}
}
