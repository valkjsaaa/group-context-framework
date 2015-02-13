package liveos_apps;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_Flickr extends DesktopApplicationProvider
{

	public App_Flickr(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"FLICKR",
				"Flickr",
				"A test app running on the Enyo Platform.",
				"DEBUG",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://74.111.161.33/gcf/universalremote/magic/ink.png",				   // LOGO
				30,
				commMode,
				ipAddress,
				port);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=http://enyojs.com/sampler/latest/lib/layout/panels/samples/PanelsFlickrSample.html" };
		//return new String[] { "WEBSITE=http://enyojs.github.io/moon-flickr/" };
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
