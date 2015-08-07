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
		else if (categoryName.equalsIgnoreCase("administrative"))
		{
			return 0xFF993333;
		}		
		else if (categoryName.equalsIgnoreCase("bluewave"))
		{
			return 0xFF188CFF;
		}
		else if (categoryName.equalsIgnoreCase("devices"))
		{
			return 0xFF364261;
		}
		else if (categoryName.equalsIgnoreCase("favors"))
		{
			return 0xFF9933CC;
		}
		else if (categoryName.equalsIgnoreCase("favors (yours)"))
		{
			return 0xFF9933CC;
		}
		else if (categoryName.equalsIgnoreCase("favors (others)"))
		{
			return 0xFF9933CC;
		}
		else if (categoryName.equalsIgnoreCase("feedback"))
		{
			return 0xFFDEB201;
		}
		else if (categoryName.equalsIgnoreCase("impromptu"))
		{
			return 0xFF0186D5;
		}
		else if (categoryName.equalsIgnoreCase("navigation"))
		{
			return 0xFF0080FF;
		}
		else if (categoryName.equalsIgnoreCase("snap-to-it"))
		{
			return 0xFFFF8000;
		}
		else if (categoryName.equalsIgnoreCase("transportation"))
		{
			return 0xFF339966;
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
		else if (categoryName.equalsIgnoreCase("snap-to-it") || categoryName.equalsIgnoreCase("devices"))
		{
			return "Connect to Appliance";
		}
		
		// Default Message
		return "Run Application";
	}
}
