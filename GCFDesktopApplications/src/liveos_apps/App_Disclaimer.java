package liveos_apps;

import java.util.Date;

import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.liveos.ApplicationElement;
import com.adefreitas.liveos.ApplicationObject;
import com.adefreitas.messages.ComputeInstruction;
import com.google.gson.JsonObject;

public class App_Disclaimer extends DesktopApplicationProvider
{

	public App_Disclaimer(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"DISCLAIMER",
				"Impromptu Disclaimer",
				"You must agree to the system before you can use its apps.",
				"ADMINISTRATIVE",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"",				   // LOGO
				30,
				commMode,
				ipAddress,
				port);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String ui  = "<html><title>Disclaimer</title>";
		ui 		  += "<h4>Please Read and Agree to the Following:</h4>";
		ui 		  += "<p>This app is a research tool, NOT a commercial product.  By using this app, you give Carnegie Mellon University permission to " +
				  " access the sensors on your phone in order at any time.  In addition, the services provided by this app may change at any time and without warning.</p>";
		ui	      += "<p><input value=\"I AGREE\" type=\"button\" height=\"100\" " +
				      "onclick=\"device.toast('Welcome to Impromptu.'); device.setPreference('disclaimer','true'); device.removeApplicationFromCatalog(); device.finish()\"/></p>";
		ui	      += "<p><input value=\"I DISAGREE\" type=\"button\" height=\"100\" onclick=\"device.finish();\"/></p>";
								
		// Delivers the UI Code, as Well as the Objects
		return new String[] { "UI=" + ui };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	@Override
	public boolean sendAppData(String json)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		return !this.signedDisclaimer(parser);
	}

}
