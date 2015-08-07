package impromptu_apps.desktop;

import java.util.Calendar;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_Porch extends DesktopApplicationProvider
{	
	public static final String   CONTEXT_TYPE  	      = "PORCH";
	public static final String   DEFAULT_TITLE 	      = "Menu at the Porch";
	public static final String   DEFAULT_DESCRIPTION  = "This app provides you with the menu for your particular meal at the porch.";
	public static final String   DEFAULT_CATEGORY     = "DINING";
	public static final String[] CONTEXTS_REQUIRED    = new String[] { };
	public static final String[] PREFERENCES_REQUIRED = new String[] { };
	public static final String   DEFAULT_LOGO_PATH    = "http://thecity.guide/sites/default/files/pdf_menu_categories/menu-icon-250.png";
	public static final int      DEFAULT_LIFETIME	  = 300;
	
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
	public App_Porch(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				CONTEXT_TYPE, 
				DEFAULT_TITLE, 
				DEFAULT_DESCRIPTION, 
				DEFAULT_CATEGORY, 
				CONTEXTS_REQUIRED, 
				PREFERENCES_REQUIRED, 
				DEFAULT_LOGO_PATH, 
				DEFAULT_LIFETIME,				
				commMode, ipAddress, port);
	}
		
	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		Calendar cal  = Calendar.getInstance();
		int      hour = cal.get(Calendar.HOUR_OF_DAY);
		
		if (hour >= 7 && hour < 11)
		{
			return new String[] { "WEBSITE=http://www.theporchatschenley.com/content.aspx?pid=20&ppid=2" };	
		}
		else if (hour >= 11 && hour < 16)
		{
			return new String[] { "WEBSITE=http://www.theporchatschenley.com/content.aspx?pid=7&ppid=2" };	
		}
		else if (hour >= 16)
		{
			return new String[] { "WEBSITE=http://www.theporchatschenley.com/content.aspx?pid=8&ppid=2" };	
		}
		
		return new String[] { "WEBSITE=http://www.theporchatschenley.com/home.aspx" };
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
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, bluewaveContext);
		
		if (this.getDistance(parser, 40.442730, -79.953100) < 0.5)
		{
			return true;	
		}
		
		return false;
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
		Calendar cal  = Calendar.getInstance();
		int      hour = cal.get(Calendar.HOUR_OF_DAY);
		
		if (hour >= 7 && hour < 11)
		{
			return "Provides you with the Porch's breakfast menu.";
		}
		else if (hour >= 11 && hour < 16)
		{
			return "Provides you with the Porch's lunch menu.";	
		}
		else if (hour >= 16)
		{
			return "Provides you with the Porch's dinner menu.";	
		}
		
		return "Provides you with the Porch's menu.";
	}
	
	/**
	 * OPTIONAL:  Allows you to Customize the Lifetime of an App on a Per User Basis
	 */
	public int getLifetime(String userContextJSON)
	{
		return this.lifetime;
	}

	/**
	 * OPTIONAL:  Allows you to Customize the Logo of an App on a Per User Basis
	 */
	public String getLogoPath(String userContextJSON)
	{
		return logoPath;
	}
}
