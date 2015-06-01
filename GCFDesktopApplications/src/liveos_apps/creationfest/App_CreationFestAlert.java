package liveos_apps.creationfest;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.*;
import org.xml.sax.InputSource;
import org.w3c.dom.*;
import java.io.*;

import liveos_apps.DesktopApplicationProvider;

import cern.colt.Arrays;

import com.adefreitas.desktopframework.toolkit.HttpToolkit;
import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_CreationFestAlert extends DesktopApplicationProvider
{	
	// Constants
	private static final String UPDATE_URL  = "http://gcf.cmu-tbank.com/apps/creationfest/getRecentProblems.php?timestamp=";
	private static final int    UPDATE_RATE = 60000;
	
	private Date     						   timestamp       = new Date(0);
	private ArrayList<ItemInfo> 			   items           = new ArrayList<ItemInfo>();
	private HashMap<String, JSONContextParser> userContext     = new HashMap<String, JSONContextParser>();
	private HashMap<String, String> 		   userDescription = new HashMap<String, String>();
	
	/**
	 * Constructor
	 * @param application
	 * @param groupContextManager
	 * @param contextType
	 * @param name
	 * @param description
	 * @param contextsRequired
	 * @param preferencesToRequest
	 * @param logoPath
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 */
	public App_CreationFestAlert(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(	groupContextManager, 
				"CF_ALERT", 
				"Alert", 
				"You are in range of a reported problem.  Click for more details.", 
				"CREATIONFEST",
				new String[] { }, 
				new String[] { }, 
				"http://25.media.tumblr.com/tumblr_kvewyajk5c1qzxzwwo1_500.jpg",
				30,
				commMode, 
				ipAddress, 
				port);
	}
		
	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		System.out.println("Generating Interface for " + subscription.getDeviceID());
		
		String ui  = "<html>";
		ui		  += "<title>Reported Problems</title>";
		ui 		  += "<h4>Nearby Problems (Click for More Details)</h4>";
		
		JSONContextParser parser = userContext.get(subscription.getDeviceID());
		
		if (parser != null)
		{
			for (ItemInfo item : items)
			{
				double distance = this.getDistance(parser, item.getLatitude(), item.getLongitude());
				
				if (distance < 0.250)
				{
					ui += "<p>" + item.getDescription() + " [" + distance + " km]</p>";
					ui += "<a href=\"http://gcf.cmu-tbank.com/apps/creationfest/viewProblem.php?timestamp=" + item.getTimestamp() + "\">Show Details</a>";
				}
			}
			
			ui += "<h4>Subscription Info</h4>";
			ui += "<p>" + parser.toString() + "</p>";
		}
		else
		{
			ui += "An unexpected error occurred.";
		}
		
		ui += "</html>";
		
		return new String[] { "UI=" + ui };
	}

	/**
	 * Logic for when a User Subscribes
	 */
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		System.out.println(newSubscription.getDeviceID() + " has subscribed.");
	}
	
	/**
	 * Logic for when a User Unsubscribes
	 */
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		super.onSubscriptionCancelation(subscription);
		
		System.out.println(subscription.getDeviceID() + " has unsubscribed.");
	}
	
	/**
	 * Determines Whether or Not to Send Application Data
	 */
	@Override
	public boolean sendAppData(String bluewaveContext)
	{
		update();
		
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, bluewaveContext);
		userContext.put(this.getDeviceID(parser), parser);
		
		int count = 0;
		
		String description = "";
		
        for (ItemInfo item : items) 
        {
           if (this.getDistance(parser, item.getLatitude(), item.getLongitude()) < 0.250)
           {
        	   count++;
        	   description += item.getDescription() + " " + Arrays.toString(item.getRoles()) + "\n";
           }
        }
		
        description = "Found " + count + " reported problems nearby:\n" + description;
        userDescription.put(this.getDeviceID(parser), description);
        
		return (count > 0);
	}

	/**
	 * Handles Compute Instructions Received from Clients
	 */
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
		
		if (instruction.getCommand().equalsIgnoreCase("accept"))
		{
			
		}
	}

	public String getDescription(String userContextJSON)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, userContextJSON);
		String description = userDescription.get(this.getDeviceID(parser));
		
		if (description != null)
		{
			return description;
		}
		
		else
		{
			return "";
		}
	}
	
	public int getLifetime(String userContextJSON)
	{
		if (getDescription(userContextJSON).length() > 0)
		{
			return 120;
		}
		else
		{
			return 0;
		}
	}
	
	private void update()
	{
		Date currentDate = new Date();
		
		if (currentDate.getTime() - timestamp.getTime() > UPDATE_RATE)
		{
			 String xml = HttpToolkit.get(UPDATE_URL + 0);
			 
			 try {
			        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			        DocumentBuilder 	   db  = dbf.newDocumentBuilder();
			        InputSource 		   is  = new InputSource();
			        is.setCharacterStream(new StringReader(xml));

			        Document doc = db.parse(is);
			        
			        NodeList nodes = doc.getElementsByTagName("item");
			        
			        items.clear();
			        
			        for (int i=0; i<nodes.getLength(); i++)
			        {
			        	Element element = (Element) nodes.item(i);
			        	items.add(new ItemInfo(element));
			        }
			    }
			    catch (Exception e) {
			        System.out.println("Problem Downloading File: " + e.getMessage());
			    }
			 
			 timestamp = new Date();
		}
	}
	
}
