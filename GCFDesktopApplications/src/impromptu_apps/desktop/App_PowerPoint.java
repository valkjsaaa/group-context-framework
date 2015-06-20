package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Date;

import com.adefreitas.desktopframework.toolkit.CloudStorageToolkit;
import com.adefreitas.desktopframework.toolkit.HttpToolkit;
import com.adefreitas.desktopframework.toolkit.ScreenshotToolkit;
import com.adefreitas.desktopframework.toolkit.SftpToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.liveos.ApplicationElement;
import com.adefreitas.liveos.ApplicationObject;
import com.adefreitas.messages.ComputeInstruction;

public class App_PowerPoint extends DesktopApplicationProvider
{		
	// Provider Specific Variables Go Here
	public		  String PRIMARY_DEVICE		   = "";
	public 		  String PRESENTATION_FILE     = "";	// The File (ON THE COMPUTER)
	public static String PRESENTATION_LOCATION = "";	// The Path to the File (ON THE COMPUTER)
	public static String UPLOAD_FOLDER         = "/var/www/html/gcf/universalremote/Server/";	// The Folder Containing the Presentation (ON SERVER)
	
	private  CloudStorageToolkit cloudToolkit;
	
	public App_PowerPoint(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_POWERPOINT",
				"Digital Projector App",
				"This app lets you upload a presentation to the digital projector in this conference room, as well as advance the slide deck.",
				"DEBUG",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://png-2.findicons.com/files/icons/770/token_dark/128/projector.png",				   // LOGO
				60,
				commMode,
				ipAddress,
				port);
		
		cloudToolkit = new SftpToolkit();
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String ui = "<html><title>Digital Projector App</title>";
		
		if (PRESENTATION_FILE.length() == 0)
		{
			ui += "<div><input value=\"Upload Presentation\" type=\"button\" style=\"height:50px; width:300px; font-size:25px\" onclick=\"device.uploadFile('PP_UPLOADED', 'Pick Your Presentation', ['.pptx']);\"/></div>";
			ui += "</html>";
		}
		else
		{
			if (PRIMARY_DEVICE.length() == 0 || subscription.getDeviceID().equals(PRIMARY_DEVICE))
			{
				ui += "<div style=\"text-align:center;\"><h3>Presentation Controls</h3><input value=\"PREV\" type=\"button\" style=\"height:100px; width:100px; font-size:25px\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=left']);\"/>" +
					  "<input value=\"NEXT\" type=\"button\" style=\"height:100px; width:100px; font-size:25px\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=right']);\"/></div>" +
					  "<br/>" +
					  "<div style=\"text-align:center;\"><input value=\"Quit Presentation\" type=\"button\" style=\"height:50px; font-size:25px\" onclick=\"device.sendComputeInstruction('QUIT', []);\"/></div>" +
					  "</html>";
				
				// Creates an Object for this Application
				ApplicationObject obj = new ApplicationObject("FILE_PPTX", PRESENTATION_FILE);
				
				// Converts Objects into a JSON String
				String objects = ApplicationElement.toJSONArray(new ApplicationElement[] { obj }) ;
						
				// Delivers the UI Code, as Well as the Objects
				//System.out.println("OBJECTS = " + objects);
				return new String[] { "UI=" + ui, "OBJECTS=" + objects};
			}
			else
			{
				ui += "<div>" + 
						"<h5>Last Updated: " + new Date().toString() + "</h5><h5>Latest Screenshot</h5>"+
						"<img border=\"0\" src=\"http://" + Settings.DEV_WEB_IP + "/gcf/universalremote/Server/screen.jpeg\" alt=\"Screenshot\" width=\"304\" height=\"228\">" +
						"<input value=\"Download Presentation\" type=\"button\" style=\"height:50px; font-size:25px\" onclick=\"device.downloadFile('" + UPLOAD_FOLDER + PRESENTATION_FILE + "');\"/>" + 
					  "</div>" +
					  "</html>";
			}
		}
		
		return new String[] { "UI=" + ui  };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
		
		System.out.println("Received Instruction: " + instruction.toString());
		
		Robot robot = this.getRobot();
		
		if (instruction.getCommand().equals("KEYPRESS"))
		{
			String key = instruction.getPayload("keycode");
			this.pressKey(key);
		}
		else if (instruction.getCommand().equals("PP_UPLOADED"))
		{		
			PRIMARY_DEVICE = instruction.getDeviceID();
			System.out.println("PRIMARY_DEVICE: " + PRIMARY_DEVICE);
			
			String filePath = instruction.getPayload("uploadPath");
			System.out.println("I got notified of an upload at: " + filePath);
			
			String folder      = filePath.substring(0, filePath.lastIndexOf("/") + 1);
			String filename    = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length());
			String destination = this.getLocalStorageFolder() + instruction.getDeviceID().replace(" ", "") + "/" + filename;
			
			System.out.println("Folder: " + folder);
			System.out.println("File:   " + filename);
			System.out.println("Dest:   " + destination);
			
			// Tries to Download the File
			HttpToolkit.downloadFile(folder + filename, destination);
			
			File file = new File(destination);
			
			if (file.exists())
			{
				PRESENTATION_FILE     = filename;
				PRESENTATION_LOCATION = destination;
				
				// Uploads the Presentation to Dropbox
				cloudToolkit.uploadFile(UPLOAD_FOLDER, new File(PRESENTATION_LOCATION));
								
				this.sendContext();
				
				runPresentation(false);
			}
		}
		else if (instruction.getCommand().equals("QUIT"))
		{
			PRESENTATION_FILE     = "";
			PRESENTATION_LOCATION = "";
					
			robot.keyPress(KeyEvent.VK_ESCAPE);
			robot.delay(40);
			robot.keyRelease(KeyEvent.VK_ESCAPE);
			robot.delay(500);
			
			robot.keyPress(KeyEvent.VK_META);
			robot.delay(40);
			robot.keyPress(KeyEvent.VK_Q);
			robot.delay(40);
			robot.keyRelease(KeyEvent.VK_META);
			robot.delay(40);
			robot.keyRelease(KeyEvent.VK_Q);
			robot.delay(500);
			
			this.sendContext();
		}
	}

	// OVERRIDE METHODS ----------------------------------------------------------------------------------
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		super.onSubscriptionCancelation(subscription);
		
		if (PRIMARY_DEVICE.equals(subscription.getDeviceID()))
		{
			PRIMARY_DEVICE = "";
			
			if (PRESENTATION_FILE.length() > 0)
			{
				PRESENTATION_FILE = "";
				
				Robot robot = this.getRobot();
				
				robot.keyPress(KeyEvent.VK_ESCAPE);
				robot.delay(40);
				robot.keyRelease(KeyEvent.VK_ESCAPE);
				robot.delay(1000);
				
				robot.keyPress(KeyEvent.VK_META);
				robot.delay(40);
				robot.keyPress(KeyEvent.VK_Q);
				robot.delay(40);
				robot.keyRelease(KeyEvent.VK_META);
				robot.delay(40);
				robot.keyRelease(KeyEvent.VK_Q);
				robot.delay(1000);
			}
		}
		
		this.getGroupContextManager().cancelRequest("ACC", subscription.getDeviceID());
		
		sendContext();
	}
	
	// HELPER METHODS ------------------------------------------------------------------------------------
	private void runPresentation(boolean quitExisting)
	{		
		System.out.println("Running presentation: " + PRESENTATION_FILE);
		
		Robot robot = this.getRobot();
		
		if (quitExisting)
		{
			robot.keyPress(KeyEvent.VK_ESCAPE);
			robot.delay(40);
			robot.keyRelease(KeyEvent.VK_ESCAPE);
			robot.delay(1000);
			
			robot.keyPress(KeyEvent.VK_META);
			robot.delay(40);
			robot.keyPress(KeyEvent.VK_Q);
			robot.delay(40);
			robot.keyRelease(KeyEvent.VK_META);
			robot.delay(40);
			robot.keyRelease(KeyEvent.VK_Q);
			robot.delay(1000);
		}
		
		try
		{
			// Runs Powerpoint
			this.executeRuntimeCommand("open " + PRESENTATION_LOCATION);
			
			// Gives the Program Time to Do What it Needs to Do
			Thread.sleep(1500);
			
			// Tries to Enter Presentation Mode
			robot.keyPress(KeyEvent.VK_SHIFT);
			robot.delay(40);
			robot.keyPress(KeyEvent.VK_META);
			robot.delay(40);
			robot.keyPress(KeyEvent.VK_ENTER);
			robot.delay(40);
			robot.keyRelease(KeyEvent.VK_SHIFT);
			robot.delay(40);
			robot.keyRelease(KeyEvent.VK_META);
			robot.delay(40);
			robot.keyRelease(KeyEvent.VK_ENTER);
		}
		catch (Exception ex)
		{
			System.out.println("A problem occurred while setting up the PowerPoint Presentation: " + ex.getMessage());
		}
	}

	private void pressKey(String key)
	{
		int keycode = -1;
		
		if (key.equals("left"))
		{
			keycode = KeyEvent.VK_LEFT;
		}
		else if (key.equals("right"))
		{
			keycode = KeyEvent.VK_RIGHT;
		}
		
		System.out.println("Pressing " + keycode);
					
		Robot robot = this.getRobot();
	    robot.delay(20);
	    robot.keyPress(keycode);
	    robot.delay(20);
	    robot.keyRelease(keycode);
	
	    // TODO:  Experimental . . .
	    File screenshot = ScreenshotToolkit.takeScreenshot(320, 240, this.getLocalStorageFolder() + "screen");
	    cloudToolkit.uploadFile(UPLOAD_FOLDER, screenshot);
	    
	    System.out.println("Uploaded New Screenshot");
	    this.sendContext();
	}

	
	@Override
	public boolean sendAppData(String bluewaveContextJSON) 
	{
		return true;
	}
}
