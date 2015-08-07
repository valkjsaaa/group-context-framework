package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import java.util.Date;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.impromptu.ApplicationElement;
import com.adefreitas.gcf.impromptu.ApplicationObject;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_Diagnostics extends DesktopApplicationProvider
{

	public App_Diagnostics(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"DIAG",
				"Diagnostic App",
				"An application intended only for developers.",
				"DEBUG",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"https://collegeready.epiconline.org/platform/assets/images/icons/icon_campusready.png",				   // LOGO
				30,
				commMode,
				ipAddress,
				port);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String ui  = "<html><title>Diagnostic App :)</title>";
		ui 		  += "<h4>Updated: " + new Date() + ".</h4></html>";
		ui 		  += "<h4>Download Test.</h4></html>";
		ui	      += "<div><input value=\"Download Test.docx\" type=\"button\" height=\"100\" onclick=\"device.downloadFile('http://gcf.cmu-tbank.com/test.docx');\"/></div>";
		
		// Creates an Object for this Application
		ApplicationObject obj = new ApplicationObject(this.getAppID(), "FILE_DOCX", "Download Test.docx");
		
		// Converts Objects into a JSON String
		String objects = ApplicationElement.toJSONArray(new ApplicationElement[] { obj }) ;
				
		// Delivers the UI Code, as Well as the Objects
		//System.out.println("OBJECTS = " + objects);
		return new String[] { "UI=" + ui, "OBJECTS=" + objects};
		//return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/diagnostics/index.html", "OBJECTS=" + objects};
		//return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/diagnostics/index.html"};
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
