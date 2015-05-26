package liveos_apps.creationfest;

import javax.xml.parsers.*;
import org.xml.sax.InputSource;
import org.w3c.dom.*;
import org.w3c.dom.CharacterData;

import java.io.*;

public class ItemInfo 
{
	private Element element;
	
	public ItemInfo(Element element)
	{
		this.element = element;
	}
	
	public String getDescription()
	{
		return getString("desc", element);
	}
	
	public String getTimestamp()
	{
		return getString("timestamp", element);
	}
	
	public double getLatitude()
	{
		return getDouble("latitude", element);
	}
	
	public double getLongitude()
	{
		return getDouble("longitude", element);
	}
	
	public String[] getRoles()
	{
		String roles = getString("roles", element);
		
		if (roles != null)
		{
			return roles.split(",");
		}
		else
		{
			return new String[0];
		}
	}
	
	private static String getString(String value, Element element)
	{
		NodeList node = element.getElementsByTagName(value);
		Element line = (Element)node.item(0);
		return getCharacterDataFromElement(line);
	}
	
	private static double getDouble(String value, Element element)
	{
		try
		{
			NodeList node = element.getElementsByTagName(value);
			Element line = (Element)node.item(0);
			return Double.parseDouble(getCharacterDataFromElement(line));
		}
		catch (Exception ex)
		{
			System.out.println("Problem Getting Double: " + ex.getMessage());
			return 0.0;
		}
	}
	
	private static String getCharacterDataFromElement(Element e) 
	  {
		  Node child = e.getFirstChild();
		  
		  if (child instanceof CharacterData) 
		  {
			  CharacterData cd = (CharacterData) child;
		      return cd.getData();
		  }
		  
		  return null;
	  }
}
