package impromptu_apps.favors;

import java.sql.ResultSet;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.desktop.toolkit.SQLToolkit;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_FavorProfile extends DesktopApplicationProvider
{	
	private SQLToolkit toolkit;
	
	public App_FavorProfile(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port, SQLToolkit toolkit)
	{
		super(groupContextManager, 
				"FAVOR_PROFILE",
				"Manage Profile",
				"Configure your contact and profile settings through this app.",
				"FAVORS",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://icons.iconarchive.com/icons/double-j-design/origami-colored-pencil/256/blue-user-icon.png", // LOGO
				3600,
				commMode,
				ipAddress,
				port);
		
		this.toolkit = toolkit;
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/favors/profile_manage.php?deviceID=" + subscription.getDeviceID()};
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	@Override
	public boolean sendAppData(String json)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		
		try
		{
			String    query   = String.format("SELECT device_id FROM favors_profile WHERE device_id='%s' AND status=1;", this.getDeviceID(parser)); 
			ResultSet results = toolkit.runQuery(query);
			return results.next();
		}
		catch (Exception ex)
		{
			System.out.println("PRoblem Occurred Checking DeviceID Against Database: " + ex.getMessage());
		}
		
		return false;
	}
	
}
