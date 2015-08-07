package impromptu_apps.creationfest;

import java.util.Calendar;
import java.util.Date;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_CreationFestSurvey extends DesktopApplicationProvider
{	
	public static final String   CONTEXT_TYPE  	      = "IMP_SURVEY";
	public static final String   DEFAULT_TITLE 	      = "Impromptu Survey";
	public static final String   DEFAULT_DESCRIPTION  = "Please fill out a survey to let us know about your experiences using Impromptu";
	public static final String   DEFAULT_CATEGORY     = "SURVEY";
	public static final String[] CONTEXTS_REQUIRED    = new String[] { };
	public static final String[] PREFERENCES_REQUIRED = new String[] { };
	public static final String   DEFAULT_LOGO_PATH    = "https://panorama-www.s3.amazonaws.com/sites/53d29249e511304c2f000002/theme/images/icon-admin.png";
	public static final int      DEFAULT_LIFETIME	  = 3600;
	
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
	public App_CreationFestSurvey(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
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
		return new String[] { "WEBSITE=https://docs.google.com/forms/d/1pTR6DRaNYU2cR1yQNGCTiOP5KTCln2danN216HJcc5k/viewform" };
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
						}));
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
		return description;
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
