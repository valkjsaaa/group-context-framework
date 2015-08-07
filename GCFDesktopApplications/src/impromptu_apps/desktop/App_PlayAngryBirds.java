package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_PlayAngryBirds extends DesktopApplicationProvider
{	
	public App_PlayAngryBirds(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"ANGRY",
				"Angry Birds",
				"The survival of the Angry Birds is at stake. Dish out revenge on the greedy pigs who stole their eggs. Use the unique powers of each bird to destroy the pigs’ defenses.",
				"DEBUG",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://74.111.161.33/gcf/universalremote/magic/travel.png",				   // LOGO
				30,
				commMode,
				ipAddress,
				port);
	}
	
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{		
		return new String[] { "PACKAGE=com.rovio.angrybirds" };
	}

	@Override
	public boolean sendAppData(String json)
	{
		return true;
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}
}
