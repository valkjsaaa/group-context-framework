package impromptu_apps.desktop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_Schedule extends DesktopApplicationProvider
{	
	public static final String   CONTEXT_TYPE  	      = "SCH_";
	public static final String   DEFAULT_DESCRIPTION  = "No upcoming events.";
	public static final String   DEFAULT_CATEGORY     = "SCHEDULE";
	public static final String[] CONTEXTS_REQUIRED    = new String[] { };
	public static final String[] PREFERENCES_REQUIRED = new String[] { };
	public static final String   DEFAULT_LOGO_PATH    = "http://static1.squarespace.com/static/549db876e4b05ce481ee4649/t/54a3a313e4b0a068ceb0e5f8/1420010259452/_0002_schedule-visit-icon.png";
	public static final int      DEFAULT_LIFETIME	  = 900;
	
	private ArrayList<EventInfo> events    = new ArrayList<EventInfo>();
	private EventInfo			 lastEvent = null;                 
	
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
	public App_Schedule(String name, GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				CONTEXT_TYPE + name, 
				name, 
				DEFAULT_DESCRIPTION, 
				DEFAULT_CATEGORY, 
				CONTEXTS_REQUIRED, 
				PREFERENCES_REQUIRED, 
				DEFAULT_LOGO_PATH, 
				DEFAULT_LIFETIME,				
				commMode, ipAddress, port);
	}
		
	/**
	 * Adds a Custom Event to this Schedule
	 * @param eventName
	 * @param eventDescription
	 * @param date
	 * @param url
	 */
	public void addEvent(String eventName, String eventDescription, Date startDate, Date endDate, String url)
	{
		events.add(new EventInfo(eventName, startDate, endDate, url, eventDescription));
		Collections.sort(events);
		lastEvent = events.get(events.size()-1);
	}
	
	/**
	 * Returns the Most Upcoming Event
	 * @return
	 */
	public EventInfo getUpcomingEvent()
	{
		EventInfo upcomingEvent = null;
		
		for (EventInfo event : events)
		{
			if (event.startDate.getTime() > System.currentTimeMillis())
			{
				System.out.println("Upcoming Event: " + event.eventName + ": " + event.startDate);
				upcomingEvent = event;
				break;
			}
		}
		
		return upcomingEvent;
	}
	
	public EventInfo getCurrentEvent()
	{
		EventInfo currentEvent = null;
		
		for (EventInfo event : events)
		{
			if (event.startDate.getTime() < System.currentTimeMillis() && event.endDate.getTime() > System.currentTimeMillis())
			{
				System.out.println("Current Event: " + event.eventName + ": " + event.startDate);
				currentEvent = event;
				break;
			}
		}
		
		return currentEvent;
	}
	
	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{		
		EventInfo upcomingEvent = getUpcomingEvent();
		
		if (upcomingEvent != null)
		{
			return new String[] { "WEBSITE=" + upcomingEvent.url };
		}
		else
		{
			return new String[] { "UI=No upcoming events." };
		}
	}

	/**
	 * Event Called When a Client Connects
	 */
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		//Toast.makeText(this.getApplication(), newSubscription.getDeviceID() + " has subscribed.", Toast.LENGTH_LONG).show();
	}
	
	/**
	 * Event Called with a Client Disconnects
	 */
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		super.onSubscriptionCancelation(subscription);
		
		//Toast.makeText(this.getApplication(), subscription.getDeviceID() + " has unsubscribed.", Toast.LENGTH_LONG).show();
	}
	
	/**
	 * Determines Whether or Not to Send Application Data
	 */
	@Override
	public boolean sendAppData(String bluewaveContext)
	{
		// TODO:  Specify the EXACT conditions when this app should appear
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, bluewaveContext);
		return (this.hasEmailAddress(parser, new String[] { 
				"adrian.defreitas@gmail.com", 
				"anind@cs.cmu.edu",
				"roywant@google.com",
				"maxsenges@google.com",
				"ptone@google.com",
				"ninataft@google.com",
				"walz@google.com",
				"rhk@illinois.edu",
				"shmatikov@cornell.edu",
				"lujo.bauer@gmail.com",
				"lorrie@cs.cmu.edu",
				"sadeh@cs.cmu.edu",
				"yuvraj.agarwal@gmail.com",
				"jasonhong666@gmail.com",
				"cmusatya@gmail.com",
				"anthony.rowe2@gmail.com",
				"harrisonhci@gmail.com",
				"aninddey@gmail.com",
				"gdarakos@cs.cmu.edu",
				"edge.hayashi@gmail.com",
				"awmcmu@gmail.com",
				"lyou@google.com",
				"youngjoo@google.com"
				}) && getUpcomingEvent() != null);
	}

	/**
	 * Handles Compute Instructions Received from Clients
	 */
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
	}

	/**
	 * OPTIONAL:  Allows you to Customize the Name of the App on a Per User Basis
	 */
	public String getName(String userContextJSON)
	{
		return name;
	}
	
	/**
	 * OPTIONAL:  Allows you to Customize the Category of the App on a Per User Basis
	 */
	public String getCategory(String userContextJSON)
	{
		return category;
	}
	
	/**
	 * OPTIONAL:  Allows you to Customize the Description of the App on a Per User Basis
	 */
	public String getDescription(String userContextJSON)
	{
		EventInfo currentEvent  = getCurrentEvent();
		EventInfo upcomingEvent = getUpcomingEvent();
		
		String description = "";
		
		if (currentEvent != null)
		{
			description = "Current Event: " + upcomingEvent.eventName + "\nStarted at " + upcomingEvent.startDate + "\n\n";
		}
		
		if (upcomingEvent != null)
		{
			description = "Next Event: " + upcomingEvent.eventName + "\nOccurs at " + upcomingEvent.startDate;
		}
		
		return description;
	}
	
	/**
	 * OPTIONAL:  Allows you to Customize the Lifetime of an App on a Per User Basis
	 */
	public int getLifetime(String userContextJSON)
	{
		EventInfo upcomingEvent = getUpcomingEvent();
		
		if (upcomingEvent != null)
		{
			return (int)(upcomingEvent.startDate.getTime() - System.currentTimeMillis()) / 1000;
		}
		
		return 300;
	}

	/**
	 * OPTIONAL:  Allows you to Customize the Logo of an App on a Per User Basis
	 */
	public String getLogoPath(String userContextJSON)
	{
		return logoPath;
	}

	private class EventInfo implements Comparable
	{
		public String eventName;
		public Date   startDate;
		public Date   endDate;
		public String url;
		public String description;
		
		public EventInfo(String eventName, Date startDate, Date endDate, String url, String description)
		{
			this.eventName   = eventName;
			this.startDate   = startDate;
			this.endDate     = endDate;
			this.url         = url;
			this.description = description;
		}

		@Override
		public int compareTo(Object o) 
		{
			return startDate.compareTo(((EventInfo)o).startDate);
		}
	}
}
