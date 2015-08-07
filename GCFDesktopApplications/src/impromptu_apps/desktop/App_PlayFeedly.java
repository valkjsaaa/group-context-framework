package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import java.util.Date;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.messages.ComputeInstruction;

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
				"http://files.softicons.com/download/android-icons/flat-icons-add-on-1-by-martz90/png/256x256/feedly.png", // LOGO
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
		
		return true;
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}
}
