package liveos_apps;

import java.util.Date;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_PlayFeedly extends DesktopApplicationProvider
{	
	public App_PlayFeedly(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"FEEDLY",
				"Feedly",
				"It's your friendly news reader!",
				"APP",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://74.111.161.33/gcf/universalremote/magic/travel.png",				   // LOGO
				60,
				commMode,
				ipAddress,
				port);
	}
	
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{		
		return new String[] { "PACKAGE=com.devhd.feedly" };
	}

	@Override
	public boolean sendAppData(String json)
	{
		Date date = new Date();
		
		return (date.getHours() < 10 || date.getHours() > 17);
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}
}
