package com.adefreitas.gcfimpromptu.lists;

public class Theme 
{
	public static int getColor(String categoryName)
	{
		if (categoryName.equalsIgnoreCase("debug"))
		{
			return 0xFF333333;
		}
		else if (categoryName.equalsIgnoreCase("automation"))
		{
			return 0xFF996633;
		}
		else if (categoryName.equalsIgnoreCase("transportation"))
		{
			return 0xFF339966;
		}
		else if (categoryName.equalsIgnoreCase("administrative"))
		{
			return 0xFF993333;
		}
		else if (categoryName.equalsIgnoreCase("favor"))
		{
			return 0xFF993399;
		}
		
		// Default Color
		return 0xFF336699;
	}

	public static String getRunMessage(String categoryName)
	{
		if (categoryName.equalsIgnoreCase("favor"))
		{
			return "View Details";
		}
		else if (categoryName.equalsIgnoreCase("task"))
		{
			return "View Details";
		}
		
		// Default Message
		return "Run Application";
	}
}
