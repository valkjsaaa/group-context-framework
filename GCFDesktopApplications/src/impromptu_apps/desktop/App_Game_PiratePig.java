package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_Game_PiratePig extends DesktopApplicationProvider
{

	public App_Game_PiratePig(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"PIRATE",
				"Pirate Pig",
				"A test game running on the Enyo framework.",
				"GAME",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://74.111.161.33/gcf/universalremote/magic/piratepig.png",				   // LOGO
				60,
				commMode,
				ipAddress,
				port);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=http://www.enyojs.com/samples/piratepig/" };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	@Override
	public boolean sendAppData(String json)
	{
		return true;
	}

}
