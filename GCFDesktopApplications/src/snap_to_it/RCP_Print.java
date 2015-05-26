package snap_to_it;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;

import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;

public class RCP_Print extends RemoteControlProvider
{
	// Provider Specific Variables Go Here
	private static final String UPLOAD_FOLDER = "/var/www/html/gcf/universalremote/Server/";	// The folder where the clients should upload to (ON SERVER)
	
	// Busy Flag
	private boolean busy;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public RCP_Print(GroupContextManager groupContextManager) 
	{
		super(groupContextManager);

//		this.addPhoto(APP_DATA_FOLDER + "printer1.jpg");
//		this.addPhoto(APP_DATA_FOLDER + "printer2.jpg");
//		this.addPhoto(APP_DATA_FOLDER + "printer3.jpg");
		
		this.addPhoto(APP_DATA_FOLDER + "pewter1.jpg");
		this.addPhoto(APP_DATA_FOLDER + "pewter2.jpg");
		
		busy = false;
	}

	/**
	 * This holds static interfaces
	 */
	protected void initializeUserInterfaces()
	{
		super.initializeUserInterfaces();
	}

	/**
	 * This creates dynamic interfaces
	 */
	private String getInterface()
	{
		String ui = "<html><title>Printer Controls</title>" +
						//"<div><img src=\"http://dc942d419843af05523b-ff74ae13537a01be6cfec5927837dcfe.r14.cf1.rackcdn.com/wp-content/uploads/16854.jpg\" width=\"150\" height=\"150\" alt=\"Printer\"></div>" +
						"<div><img src=\"http://www.blankdvdmedia.com/product/laser-printers/hp/images/hp-laserjet-9050dn-laser-toner-cartridge.jpg\" width=\"200\" height=\"200\" alt=\"Printer\"></div>" +
						"<div><h4>Printer Name: Pewter</h4></div>" +
						((busy) ? 
							"<p style=\"color:white; background-color:red; font-size:25px\">Status: PRINTING</p>" :
							"<p style=\"color:white; background-color:green; font-size:25px\">Status: READY</p>"
						) +
						"<div><input value=\"Print File\" type=\"button\" style=\"height:50px; font-size:25px\" onclick=\"device.uploadFile('UPLOAD_PRINT', '" + UPLOAD_FOLDER + "', ['.docx', '.pdf']);\"/></div>" +
					"</html>";
		
//		ui = "<html><title>Access Denied</title>" +
//				"<div><img src=\"http://images.inmagine.com/400nwm/valueclips/unc265/u14898482.jpg\" width=\"150\" height=\"150\" alt=\"Printer\"></div>" + 
//				"<div>You do not have permission to use this printer.  <br/><br/> Please contact your network administrator for more details.</div>" + 		
//				"</html>";
		
		return ui;
	}
	
	@Override
	public void sendContext() 
	{
		// TODO:  Replace with a More Efficient Version
		//        Send ONE UI Per UI Type
		for (ContextSubscriptionInfo subscription : this.getSubscriptions())
		{
			
			
			this.getGroupContextManager().sendContext(
					this.getContextType(), 
					new String[] { subscription.getDeviceID() }, 
					new String[] { "UI=" + getInterface() });
		}
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
		
		if (instruction.getCommand().equals("UPLOAD_PRINT") && !busy)
		{
			busy = true;
			
			sendContext();
			
			String filePath = instruction.getPayload("uploadPath");
			System.out.println("I got notified of an upload at: " + filePath);
			
			String folder      = filePath.substring(0, filePath.lastIndexOf("/") + 1);
			String filename    = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length());
			String destination = APP_DATA_FOLDER + instruction.getDeviceID().replace(" ", "") + "/" + filename;
			
			System.out.println("Folder: " + folder);
			System.out.println("File:   " + filename);
			System.out.println("Dest:   " + destination);
			
			// Tries to Download the File
			this.getCloudStorageToolkit().downloadFile(filePath, destination);
			File file = new File(destination);
			
			if (file.exists())
			{
				// Opens the Application
				this.executeRuntimeCommand("open " + file.getAbsolutePath());
				
				Robot robot = this.getRobot();
				
				// Runs the Ctrl-P Command
				robot.delay(3000);
				robot.keyPress(KeyEvent.VK_META);
				robot.delay(20);
				robot.keyPress(KeyEvent.VK_P);
				robot.delay(20);
				robot.keyRelease(KeyEvent.VK_META);
				robot.delay(20);
				robot.keyRelease(KeyEvent.VK_P);
				robot.delay(500);
				
				// Accepts Default Print Parameters
				robot.keyPress(KeyEvent.VK_ENTER);
				robot.delay(20);
				robot.keyRelease(KeyEvent.VK_ENTER);
				robot.delay(3000);
				
				// Kills the Program
				robot.keyPress(KeyEvent.VK_META);
				robot.delay(20);
				robot.keyPress(KeyEvent.VK_Q);
				robot.delay(20);
				robot.keyRelease(KeyEvent.VK_META);
				robot.delay(20);
				robot.keyRelease(KeyEvent.VK_Q);
				robot.delay(500);
			}
			
			busy = false;
			
			sendContext();
		}
	}
}
