package snap_to_it;

import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class RCP_Diagnostics extends RemoteControlProvider
{
	// Provider Specific Variables Go Here
	private final String WEBSITE_URL = "http://71.182.231.215/gcf/lights.html";
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public RCP_Diagnostics(GroupContextManager groupContextManager) 
	{
		super(groupContextManager);
		
		// Use this if you want to manually ADD stuff
		this.addPhoto(APP_DATA_FOLDER + "canon1.jpg");
		this.addPhoto(APP_DATA_FOLDER + "speaker1.jpg");
		this.addPhoto(APP_DATA_FOLDER + "game1.jpg");
//		this.addPhoto(APP_DATA_FOLDER + "printer3.jpg");
//		this.addPhoto(APP_DATA_FOLDER + "jambox1.jpg");
//		this.addPhoto(APP_DATA_FOLDER + "jambox2.jpg");
//		this.addPhoto(APP_DATA_FOLDER + "jambox3.jpg");
//		this.addPhoto(APP_DATA_FOLDER + "jambox4.jpg");
//		this.addPhoto(APP_DATA_FOLDER + "projector1.jpg");
//		this.addPhoto(APP_DATA_FOLDER + "projector2.jpg");
//		this.addPhoto(APP_DATA_FOLDER + "projector3.jpg");

		// Uncomment this is you want to detect a screenshot
		//this.enableScreenshots(5000, 3);
	}

	protected void initializeUserInterfaces()
	{
		super.initializeUserInterfaces();
	}
	
	public void sendMostRecentReading()
	{
		// Broadcasts the UI to Everyone
		this.getGroupContextManager().sendContext(this.getContextType(), "", new String[0], new String[] { "UI=" + this.getDiagnosticHTML() });
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString() + " . . . but doing nothing with it.");
	}

	// PRIVATE METHODS -------------------------------------------------------------------------------------------
	public String getDiagnostics()
	{
		return "";
	}
}
