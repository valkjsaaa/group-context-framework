package snap_to_it;

import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class RCP_Light extends RemoteControlProvider
{
	// Provider Specific Variables Go Here
	private final String WEBSITE_URL = "http://71.182.231.215/gcf/universalremote/Websites/lights.html";
	
	private boolean on = true;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public RCP_Light(GroupContextManager groupContextManager) 
	{
		super(groupContextManager);
	}

	protected void initializeUserInterfaces()
	{
		super.initializeUserInterfaces();
	}
	
	public void sendMostRecentReading()
	{
		for (ContextSubscriptionInfo subscription : this.getSubscriptions())
		{
			String ui = "<html><title>Ceiling Lights</title>" +
					//"<div><img src=\"http://dc942d419843af05523b-ff74ae13537a01be6cfec5927837dcfe.r14.cf1.rackcdn.com/wp-content/uploads/16854.jpg\" width=\"150\" height=\"150\" alt=\"Printer\"></div>" +
					"<div><img src=\"http://www.stylepark.com/db-images/cms/architecture-article/img/l2_v336080_958_480_319-7.jpg\" width=\"200\" height=\"200\" alt=\"Printer\"></div>" +
					((!on) ? 
						"<p style=\"color:white; background-color:red; font-size:25px\">Status: OFF</p>" :
						"<p style=\"color:white; background-color:green; font-size:25px\">Status: ON</p>"
					) +
					"<div><input value=\"Toggle Lights\" type=\"button\" style=\"height:50px; font-size:25px\" onclick=\"device.sendComputeInstruction('TOGGLE_LIGHTS', []);\"/></div>" +
				"</html>";
			
			this.getGroupContextManager().sendContext(
					this.getContextType(), 
					"", 
					new String[] { subscription.getDeviceID() }, 
					new String[] { "UI=" + ui });
		}
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
		
		if (instruction.getCommand().equals("TOGGLE_LIGHTS"))
		{
			on = !on;
		}
		
		this.sendMostRecentReading();
	}
}
