package liveos_apps;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

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
			int value = Integer.valueOf(instruction.getParameters()[0]);
			
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
