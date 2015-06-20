package impromptu_apps.creationfest;

import impromptu_apps.DesktopApplicationProvider;

import java.util.Calendar;


import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_CreationFestReporter extends DesktopApplicationProvider
{
	private ProblemContextProvider problemProvider;
	
	public App_CreationFestReporter(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		// Creates App with Default Settings
		super(groupContextManager, 
				"CF_REPORT",
				"Problem Reporter",
				"This app lets you report problems to the CreationFest Leadership.",
				"CREATIONFEST 2015",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://www.safeconcerts.com/images/bank/creation-fest-logo.jpg", // LOGO
				300,
				commMode,
				ipAddress,
				port);
		
		// Creates a Context Provider to Report Problems
		problemProvider = new ProblemContextProvider(groupContextManager);
		
		// Loads a Context Provider for Problems!
		groupContextManager.registerContextProvider(problemProvider);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/creationfest/submitProblem.php"};
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	public int getLifetime(String userContextJSON)
	{
		Calendar cal = Calendar.getInstance();
		cal.set(2015, 5, 29, 0, 0);
		
		return (int)((cal.getTimeInMillis() - System.currentTimeMillis()) / 1000);
	}
	
	@Override
	public boolean sendAppData(String json)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		
		double distanceToFestival = this.getDistance(parser, 40.297858, -77.874164);
		
		return this.hasEmailAddress(parser, new String[] { "adrian.defreitas@gmail.com", "gcf.user.1@gmail.com" }) || distanceToFestival < 5.0;
	}
}
