package impromptu_apps.favors;

import impromptu_apps.DesktopApplicationProvider;

import java.util.Calendar;


import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_FavorRequester extends DesktopApplicationProvider
{
	public App_FavorRequester(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		// Creates App with Default Settings
		super(groupContextManager, 
				"FAVOR_REQUEST",
				"Favor Requester",
				"This app lets you request a favor.",
				"FAVOR BANK",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://cdn5.fedobe.com/wp-content/uploads/2012/09/service-icon-concept.png", // LOGO
				3600,
				commMode,
				ipAddress,
				port);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/favors/submitFavor.php"};
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	public int getLifetime(String userContextJSON)
	{
		return this.lifetime;
	}
	
	@Override
	public boolean sendAppData(String json)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		
		return this.hasEmailAddress(parser, new String[] {"adrian.defreitas@gmail.com", "gcf.user.1@gmail.com"});
	}
}
