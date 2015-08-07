package impromptu_apps.snaptoit;


import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class Sti_GenericDevice extends SnapToItApplicationProvider
{	
	String[] urls;
	
	public Sti_GenericDevice(GroupContextManager groupContextManager, String name, String[] urls, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				name,
				name,
				"This is an empty placeholder for the generic object: " + name,
				"Snap-To-It",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"",				   // LOGO
				60,
				commMode,
				ipAddress,
				port);
		
		this.name = name;
		this.urls = urls;
		this.addAppliancePhotoFromURL(urls);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{	
		return new String[] { "WEBSITE=http://www.google.com" };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

}
