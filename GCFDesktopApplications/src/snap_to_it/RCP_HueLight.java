package snap_to_it;

import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;
import com.adefreitas.messages.ContextRequest;

public class RCP_HueLight extends RemoteControlProvider
{
	// Provider Specific Variables Go Here
	private final String WEBSITE_URL = "http://71.182.231.215/gcf/universalremote/Websites/lights.html";
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public RCP_HueLight(GroupContextManager groupContextManager) 
	{
		super(groupContextManager);
		
		// Sends a Request for Light Data
		this.getGroupContextManager().sendRequest("HUE", ContextRequest.SINGLE_SOURCE, 10000, new String[0]);
		
		// TODO:  Loads the Pictures to Use
	}

	protected void initializeUserInterfaces()
	{
		super.initializeUserInterfaces();
	}
	
	public void sendMostRecentReading()
	{
		for (ContextSubscriptionInfo subscription : this.getSubscriptions())
		{
			this.getGroupContextManager().sendContext(
					this.getContextType(), 
					"", 
					new String[] { subscription.getDeviceID() }, 
					new String[] { "WEBSITE=" + WEBSITE_URL   });
		}
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
		
		if (instruction.getCommand().equals("LIGHT_RGB"))
		{
			//this.getGroupContextManager().sendComputeInstruction("HUE", instruction.getCommand(), instruction.getParameters());
		}
	}
}
