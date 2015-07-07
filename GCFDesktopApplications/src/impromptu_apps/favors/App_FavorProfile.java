package impromptu_apps.favors;


import java.util.Calendar;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_FavorProfile extends DesktopApplicationProvider
{	
	public App_FavorProfile(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"FAVOR_PROFILE",
				"Manage Profile",
				"Configure your contact and profile settings through this app.",
				"FAVOR BANK",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://icons.iconarchive.com/icons/double-j-design/origami-colored-pencil/256/blue-user-icon.png", // LOGO
				3600,
				commMode,
				ipAddress,
				port);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/favors/profile_manage.php"};
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
		
		return this.hasEmailAddress(parser, new String[] {"adrian.defreitas@gmail.com", "gcf.user.1@gmail.com"});
	}
	
}
