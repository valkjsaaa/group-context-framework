package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import java.util.Date;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_CHI2014 extends DesktopApplicationProvider
{	
	public App_CHI2014(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"CHI",
				"CHI 2014 Conference App",
				"This app lets you view the schedule of events for the CHI 2014 conference.",
				"APP",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://chi2014.acm.org/img/apple-touch-icon.png", // LOGO
				60,
				commMode,
				ipAddress,
				port);
	}
	
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{		
		return new String[] { "PACKAGE=edu.cmu.hcii.confapp.chi2014.android" };
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
