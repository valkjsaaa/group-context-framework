package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_Disclaimer extends DesktopApplicationProvider
{
	public static final String   CONTEXT_TYPE  	      = "DISCLAIMER";
	public static final String   DEFAULT_TITLE 	      = "Disclaimer";
	public static final String   DEFAULT_DESCRIPTION  = "View Impromptu's terms of use.  You must agree to these conditions before your device will receive apps.";
	public static final String   DEFAULT_CATEGORY     = "IMPROMPTU";
	public static final String[] CONTEXTS_REQUIRED    = new String[] { };
	public static final String[] PREFERENCES_REQUIRED = new String[] { };
	public static final String   LOGO_PATH			  = "http://www.andrew.cmu.edu/course/98-233/images/cmu_seal.png";
	public static final int      DEFAULT_LIFETIME	  = 120;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 */
	public App_Disclaimer(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, CONTEXT_TYPE, DEFAULT_TITLE, DEFAULT_DESCRIPTION, DEFAULT_CATEGORY, CONTEXTS_REQUIRED, PREFERENCES_REQUIRED, LOGO_PATH, DEFAULT_LIFETIME,				
				commMode, ipAddress, port);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String ui  = "<html><title>Disclaimer</title>";
		ui 		  += "<h3>Please Read the Following:</h3>";
		ui 		  += "<p>Impromptu is a research tool, <b>NOT</b> a commercial product.  By using this app, you give Carnegie Mellon University permission to " +
				  " access your phone's sensors and/or data at any time.  In addition, the services provided by this app will change at any time and without warning, so please do not rely on this app for important tasks.</p>";
		ui        += "<p>THANK YOU for using Impromptu!  Click on the 'Agree' button to start receiving apps!</p>";
		ui	      += "<p><input value=\"AGREE\" type=\"button\" height=\"150\" width=\"300\"" +
				      "onclick=\"device.toast('Welcome to Impromptu.'); device.setPreference('disclaimer','true'); device.removeApplicationFromCatalog(); device.finish()\"/></p>";
		ui	      += "<p><input value=\"DISAGREE\" type=\"button\" height=\"150\" width=\"300\" onclick=\"device.finish();\"/></p>";
								
		// Delivers the UI Code, as Well as the Objects
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/disclaimer/disclaimer.php" };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
		
		if (instruction.getCommand().equals("AGREE"))
		{
			this.log("DISCLAIMER_SIGNED", instruction.getDeviceID());
		}
	}

	@Override
	public boolean sendAppData(String json)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		return !this.signedDisclaimer(parser);
	}

}
