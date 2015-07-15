package impromptu_apps.desktop;

import java.sql.ResultSet;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.desktopframework.toolkit.SQLToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_BluewavePermissions extends DesktopApplicationProvider
{
	public static final String   CONTEXT_TYPE  	      = "BLU_PERMISSION";
	public static final String   DEFAULT_TITLE 	      = "Bluewave Permissions";
	public static final String   DEFAULT_DESCRIPTION  = "Manage Your Bluewave Permissions";
	public static final String   DEFAULT_CATEGORY     = "BLUEWAVE";
	public static final String[] CONTEXTS_REQUIRED    = new String[] { };
	public static final String[] PREFERENCES_REQUIRED = new String[] { };
	public static final String   LOGO_PATH			  = "http://www.clipartbest.com/cliparts/Rcd/g4B/Rcdg4B5Xi.jpeg";
	public static final int      DEFAULT_LIFETIME	  = 60;
	
	private SQLToolkit sqlToolkit;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 */
	public App_BluewavePermissions(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, CONTEXT_TYPE, DEFAULT_TITLE, DEFAULT_DESCRIPTION, DEFAULT_CATEGORY, CONTEXTS_REQUIRED, PREFERENCES_REQUIRED, LOGO_PATH, DEFAULT_LIFETIME,				
				commMode, ipAddress, port);
		
		sqlToolkit = new SQLToolkit("epiwork.hcii.cs.cmu.edu", "adrian", "@dr1@n1234", "gcf_bluewave");
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{							
		// Delivers the UI Code, as Well as the Objects
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/bluewave/managePermissions.php?deviceID=" + subscription.getDeviceID() };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	@Override
	public boolean sendAppData(String userContext)
	{
		JSONContextParser parser   = new JSONContextParser(JSONContextParser.JSON_TEXT, userContext);
		String 			  deviceID = this.getDeviceID(parser);
		
		try
		{
			String    query  = "SELECT COUNT(*) FROM context_permissions WHERE deviceID='" + deviceID + "' AND context_permissions.permission = -1";
			ResultSet result = sqlToolkit.runQuery(query);
			
			if (result != null && result.next())
			{
				int count = result.getInt(1);
				return count > 0;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return false;
	}
}
