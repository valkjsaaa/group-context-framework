package impromptu_apps.snaptoit;


import java.util.ArrayList;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class Sti_Map extends SnapToItApplicationProvider
{

	public Sti_Map(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_MAP",
				"CMU Map",
				"Provides you with an interactive map of the CMU campus",
				"SNAP-TO-IT",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://icons.iconarchive.com/icons/igh0zt/ios7-style-metro-ui/512/MetroUI-Google-Maps-icon.png",				   // LOGO
				60,
				commMode,
				ipAddress,
				port);
		
		this.addPhoto(this.getLocalStorageFolder() + "Map1.jpg");
	}
	
	public ArrayList<String> getInformation()
	{		
		ArrayList<String> result = new ArrayList<String>();
		
		result.add("APP_ID=" + appID);
		result.add("APP_CONTEXT_TYPE=" + CONTEXT_TYPE);
		result.add("DEVICE_ID=" + this.getGroupContextManager().getDeviceID());
		result.add("NAME=" + name);
		result.add("DESCRIPTION=" + getDebugDescription());
		result.add("CATEGORY=" + category);
		result.add("CONTEXTS=");
		result.add("PREFERENCES=");
		result.add("LOGO=" + logoPath);
		result.add("LIFETIME=" + lifetime);
		result.add("FUNCTIONS="	+ getFunctions());
		result.add("COMM_MODE="	+ commMode.toString());
		result.add("APP_ADDRESS=" + ipAddress);
		result.add("APP_PORT=" + Integer.toString(port));
		result.add("APP_CHANNEL=" + channel);
		
		return result;
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=http://www.cmu.edu/about/visit/campus-map-interactive/"};
		//return new String[] { "PACKAGE=com.rovio.angrybirds" };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	
	
}
