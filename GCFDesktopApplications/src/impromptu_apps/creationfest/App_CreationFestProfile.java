package impromptu_apps.creationfest;


import java.util.Calendar;

import impromptu_apps.DesktopApplicationProvider;

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
				"Manage Profile",
				"Configure your contact and profile settings through this app.",
				"CREATIONFEST 2015",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://icons.iconarchive.com/icons/double-j-design/origami-colored-pencil/256/blue-user-icon.png", // LOGO
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
		
		Calendar cal = Calendar.getInstance();
		cal.set(2015, 5, 20, 00, 00);
		boolean pastDate = System.currentTimeMillis() > cal.getTimeInMillis();
		
		double distanceToFestival = this.getDistance(parser, 40.297858, -77.874164);
		
		return (this.hasEmailAddress(parser, new String[] { "adrian.defreitas@gmail.com", "gcf.user.1@gmail.com" }) && pastDate)  
				|| (pastDate && distanceToFestival < 5.0);
	}
	
	/**
	 * OPTIONAL:  Allows you to Customize the Description of the App on a Per User Basis
	 */
	public String getDescription(String userContextJSON)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, userContextJSON);
		
		if (parser.getJSONObject("preferences").has("roles"))
		{
			String roles = parser.getJSONObject("preferences").get("roles").toString();
			return "Current Roles: " + roles + "\n\nClick to Modify these Settings.";
		}
		
		return description;
	}
	
}
