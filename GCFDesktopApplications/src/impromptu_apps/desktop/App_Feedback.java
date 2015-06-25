package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.desktopframework.toolkit.SQLToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_Feedback extends DesktopApplicationProvider
{
	public static final String   CONTEXT_TYPE  	      = "FEEDBACK";
	public static final String   DEFAULT_TITLE 	      = "Feedback App";
	public static final String   DEFAULT_DESCRIPTION  = "Have an idea for a new app?  Let us know!";
	public static final String   DEFAULT_CATEGORY     = "FEEDBACK";
	public static final String[] CONTEXTS_REQUIRED    = new String[] { };
	public static final String[] PREFERENCES_REQUIRED = new String[] { };
	public static final String   LOGO_PATH			  = "http://m.wmhomecoming.com/wp-content/themes/Touch-child/images/icon-feedback-256x256.png";
	public static final int      DEFAULT_LIFETIME	  = 7200;
	
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
			String query = "INSERT INTO feedback (deviceID, comment) VALUES ('" + instruction.getDeviceID() + "', '" + instruction.getPayload(0) + "');";
			sqlToolkit.runUpdateQuery(query);
		}
	}

	@Override
	public boolean sendAppData(String userContext)
	{
		return true;
	}
}
