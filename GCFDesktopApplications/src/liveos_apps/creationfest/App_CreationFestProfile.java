package liveos_apps.creationfest;


import liveos_apps.DesktopApplicationProvider;

import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_CreationFestProfile extends DesktopApplicationProvider
{	
	public App_CreationFestProfile(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"CF_USER",
				"User Settings",
				"Configure your contact and profile settings through this app.",
				"CREATIONFEST 2015",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"https://cdn4.iconfinder.com/data/icons/pretty_office_3/48/sign-up.png", // LOGO
				300,
				commMode,
				ipAddress,
				port);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/creationfest/setPreferences.php"};
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
		
		return hasEmailAddress(parser, "adrian.defreitas@gmail.com") && this.signedDisclaimer(parser);
	}
}
