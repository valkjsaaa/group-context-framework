package liveos_apps;

import java.util.ArrayList;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class Sti_Map extends SnapToItApplicationProvider
{

	public Sti_Map(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_MAP",
				"Map Demonstration",
				"Tests STI v2.0. with non-connected appliances",
				"DEBUG",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://108.32.88.8/gcf/universalremote/magic/gears.png",				   // LOGO
				30,
				commMode,
				ipAddress,
				port);
		
		this.addPhoto(this.getLocalStorageFolder() + "Map1.jpeg");
		this.addPhoto(this.getLocalStorageFolder() + "Map2.jpeg");
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
		return new String[] { "WEBSITE=https://www.google.com/maps/@40.4437326,-79.9461237,22z"};
		//return new String[] { "PACKAGE=com.rovio.angrybirds" };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	
	
}
