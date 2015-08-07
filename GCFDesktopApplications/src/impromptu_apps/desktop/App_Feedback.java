package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.desktop.toolkit.SQLToolkit;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_Feedback extends DesktopApplicationProvider
{
	public static final String   CONTEXT_TYPE  	      = "SUGGESTION";
	public static final String   DEFAULT_TITLE 	      = "Suggestion Box";
	public static final String   DEFAULT_DESCRIPTION  = "Have an idea for a new app, or a general suggestion?  Let us know!";
	public static final String   DEFAULT_CATEGORY     = "IMPROMPTU";
	public static final String[] CONTEXTS_REQUIRED    = new String[] { };
	public static final String[] PREFERENCES_REQUIRED = new String[] { };
	public static final String   LOGO_PATH			  = "http://gcf.cmu-tbank.com/apps/icons/suggestion.png";
	public static final int      DEFAULT_LIFETIME	  = 3600;
	
	public SQLToolkit sqlToolkit;
	public int 		  numSubmissions;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 */
	public App_Feedback(GroupContextManager groupContextManager, SQLToolkit sqlToolkit, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, CONTEXT_TYPE, DEFAULT_TITLE, DEFAULT_DESCRIPTION, DEFAULT_CATEGORY, CONTEXTS_REQUIRED, PREFERENCES_REQUIRED, LOGO_PATH, DEFAULT_LIFETIME,				
				commMode, ipAddress, port);
		
		this.sqlToolkit     = sqlToolkit;
		this.numSubmissions = 0;
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{							
		// Delivers the UI Code, as Well as the Objects
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/feedback/index.html" };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
		
		if (instruction.getCommand().equals("FEEDBACK"))
		{
			String query = "INSERT INTO impromptu_feedback (deviceID, comment) VALUES ('" + instruction.getDeviceID() + "', '" + instruction.getPayload(0) + "');";
			sqlToolkit.runUpdateQuery(query);
		}
	}

	@Override
	public boolean sendAppData(String userContext)
	{
		return true;
	}
}
