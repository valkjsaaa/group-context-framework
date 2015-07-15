package com.adefreitas.gcfimpromptu.lists;

public class Theme 
{
	public static int getColor(String categoryName)
	{
		if (categoryName.equalsIgnoreCase("debug"))
		{
			return 0xFFFFFFFF;
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
		else if (categoryName.equalsIgnoreCase("bluewave"))
		{
			return 0xFF2676FF;
		}
		else if (categoryName.equalsIgnoreCase("feedback"))
		{
			return 0xFF339966;
		}
		else if (categoryName.equalsIgnoreCase("favor bank"))
		{
			return 0xFF993399;
		}
		else if (categoryName.equalsIgnoreCase("favors (yours)"))
		{
			return 0xFF993399;
		}
		else if (categoryName.equalsIgnoreCase("favors (others)"))
		{
			return 0xFF993399;
		}
		else if (categoryName.equalsIgnoreCase("navigation"))
		{
			return 0xFF0080FF;
		}
		else if (categoryName.equalsIgnoreCase("snap-to-it"))
		{
			return 0xFFFF8000;
		}
		
		// Default Color
		return 0xFF333333;
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
		else if (categoryName.equalsIgnoreCase("snap-to-it"))
		{
			return "Connect to Device";
		}
		
		// Default Message
		return "Run Application";
	}
}
