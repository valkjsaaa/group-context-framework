package impromptu_apps.snaptoit;


import java.util.ArrayList;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class Sti_DoorPlate extends SnapToItApplicationProvider
{

	public Sti_DoorPlate(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_DOORPLATE",
				"Office 2602M App",
				"The occupant of this office is currently busy.  This app will tell you the next best time to meet!",
				"DEBUG",
				new String[] { "Calendar Schedule (2 Days)" },  // Contexts
				new String[] { },  // Preferences
				"",				   // LOGO
				30,
				commMode,
				ipAddress,
				port);
		
		this.addPhoto(this.getLocalStorageFolder() + "Doorplate1.jpeg");
		//this.addPhoto(this.getLocalStorageFolder() + "Map2.jpeg");
		
		this.setDebugMode(true);
	}
	
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/doorplate/index.html"};
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

}
