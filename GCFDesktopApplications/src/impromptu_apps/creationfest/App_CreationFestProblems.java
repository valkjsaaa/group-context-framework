package impromptu_apps.creationfest;


import java.util.Calendar;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_CreationFestProblems extends DesktopApplicationProvider
{	
	public App_CreationFestProblems(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"CF_PROBLEMS",
				"View Problems",
				"View reported problems (based on the events categories specified in your profile)",
				"CREATIONFEST 2015",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://www.iconsdb.com/icons/preview/soylent-red/view-details-xxl.png", // LOGO
				3600*24,
				commMode,
				ipAddress,
				port);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{		
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/creationfest/viewProblems.php?deviceID=" + subscription.getDeviceID()};
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
		
		// Must have SOME roles
		if (parser.getJSONObject("preferences").has("roles"))
		{
			if (parser.getJSONObject("preferences").get("roles").getAsString().length() == 0)
			{
				return false;
			}
		}
		
		Calendar cal = Calendar.getInstance();
		cal.set(2015, 5, 29, 00, 00);
		boolean pastDate = cal.getTimeInMillis() < System.currentTimeMillis();
		
		double distanceToFestival = this.getDistance(parser, 40.297858, -77.874164);
		
		return !pastDate;
	}
	
}
