package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_FileUploadDemo extends DesktopApplicationProvider
{
	public App_FileUploadDemo(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"FILE_UPLOAD",
				"File Upload Demo",
				"For Adrian Only.",
				"DEBUG",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://www.mysosho.com/html_json/pic/warning_icon.png", // LOGO
				300,
				commMode,
				ipAddress,
				port);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		//return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/creationfest/index.html" };
		return new String[] { "WEBSITE=http://www.androidexample.com/media/webview/details.html"};
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
		
		System.out.println("Received Instruction: " + instruction.toString());
	}

	@Override
	public boolean sendAppData(String json)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		return this.hasEmailAddress(parser, new String[] {"adrian.defreitas@gmail.com", "gcf.user.1@gmail.com", "gcf.user.2@gmail.com", "adoryab@gmail.com"});
	}
}
