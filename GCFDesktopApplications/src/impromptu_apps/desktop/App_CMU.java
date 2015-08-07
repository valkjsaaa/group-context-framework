package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import java.util.ArrayList;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.messages.CommMessage;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_CMU extends DesktopApplicationProvider
{
	public static final double MIN_DISTANCE_IN_KM = 1.0;
		
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 */
	public App_CMU(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"CMU_MAP",
				"Carnegie Mellon University Map",
				"A detailed map of the CMU campus.",
				"TRANSPORTATION",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://icons.iconarchive.com/icons/igh0zt/ios7-style-metro-ui/512/MetroUI-Google-Maps-icon.png",				   // LOGO
				300,
				commMode,
				ipAddress,
				port);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=http://www.cmu.edu/about/visit/campus-map-interactive/" };
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
		
		double distance = this.getDistance(parser, 40.4433, -79.9436);
		
		System.out.printf("Distance = %1.2f km ", distance);
		
		return distance < MIN_DISTANCE_IN_KM;
	}
}
