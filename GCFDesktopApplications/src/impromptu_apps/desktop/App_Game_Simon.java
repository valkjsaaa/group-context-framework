package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_Game_Simon extends DesktopApplicationProvider
{
	int    bestScore     = 0;
	String bestScoreName = "";
	
	public App_Game_Simon(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"SIMON",
				"Simon",
				"Remember the terrible memory game?  Well you can play it here!",
				"GAME",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://74.111.161.33/gcf/universalremote/magic/dots.png",				   // LOGO
				60,
				commMode,
				ipAddress,
				port);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/dev/index.html" };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
		
		if (instruction.getCommand().equals("SCORE"))
		{
			int value = Integer.valueOf(instruction.getPayload(0));
			
			System.out.println("SCORE = " + value);
			
			bestScore = Math.max(bestScore, value);
			this.setDescription("High Score: " + bestScore + ".  Think you can beat it?");
		}
	}

	@Override
	public boolean sendAppData(String json)
	{
		return true;
	}

}
