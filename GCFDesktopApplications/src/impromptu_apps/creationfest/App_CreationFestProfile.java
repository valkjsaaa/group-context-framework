package impromptu_apps.creationfest;


import java.util.Calendar;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_CreationFestProfile extends DesktopApplicationProvider
{	
	public App_CreationFestProfile(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"CF_USER",
				"Manage Profile",
				"Configure your contact and profile settings through this app.",
				"CREATIONFEST 2015",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://icons.iconarchive.com/icons/double-j-design/origami-colored-pencil/256/blue-user-icon.png", // LOGO
				3600*24,
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
		
		Calendar cal = Calendar.getInstance();
		cal.set(2015, 5, 29, 00, 00);
		boolean pastDate = cal.getTimeInMillis() < System.currentTimeMillis();
		
		double distanceToFestival = this.getDistance(parser, 40.297858, -77.874164);
		
		return !pastDate;
	}
	
}
