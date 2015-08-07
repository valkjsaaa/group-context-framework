package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_PlayGmail extends DesktopApplicationProvider
{	
	public App_PlayGmail(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"GMAIL",
				"Gmail",
				"Gmail is built on the idea that email can be more intuitive, efficient, and useful. And maybe even fun. Get your email instantly via push notifications, read and respond to your conversations online & offline, and search and find any email.",
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
		return new String[] { "PACKAGE=com.google.android.gm" };
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
