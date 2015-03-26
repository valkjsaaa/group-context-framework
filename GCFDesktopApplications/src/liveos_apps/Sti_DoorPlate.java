package liveos_apps;

import java.util.ArrayList;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class Sti_DoorPlate extends SnapToItApplicationProvider
{

	public Sti_DoorPlate(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_DOORPLATE",
				"Bob's Office",
				"Office 2602M.  Click here to get more information!",
				"DEBUG",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"",				   // LOGO
				30,
				commMode,
				ipAddress,
				port);
		
		this.addPhoto(this.getLocalStorageFolder() + "Doorplate1.jpeg");
		//this.addPhoto(this.getLocalStorageFolder() + "Map2.jpeg");
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
