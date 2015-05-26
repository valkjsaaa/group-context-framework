package liveos_apps;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import toolkits.ScreenshotToolkit;

import com.adefreitas.desktopframework.toolkit.CloudStorageToolkit;
import com.adefreitas.desktopframework.toolkit.HttpToolkit;
import com.adefreitas.desktopframework.toolkit.SftpToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Sti_PowerPointDemo extends SnapToItApplicationProvider
{		
	// Provider Specific Variables Go Here
	public		  String PRIMARY_DEVICE		   = "";
	public 		  String PRESENTATION_FILE     = "";	// The File (ON THE COMPUTER)
	public static String PRESENTATION_LOCATION = "";	// The Path to the File (ON THE COMPUTER)
	public static String UPLOAD_FOLDER         = "/var/www/html/gcf/universalremote/Server/";	// The Folder Containing the Presentation (ON SERVER)
	
	private  CloudStorageToolkit cloudToolkit;
	
	public Sti_PowerPointDemo(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_POWERPOINT_DEMO",
				"PointPoint App",
				"PowerPoint Application",
				"DEBUG",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"",				   // LOGO
				30,
				commMode,
				ipAddress,
				port);
		
		cloudToolkit = new SftpToolkit();
		
		this.setDebugMode(true);
		
		this.enableScreenshots(300000, 1);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String ui = "<html><title>Digital Projector</title>";
		
		if (PRIMARY_DEVICE.length() == 0 || subscription.getDeviceID().equals(PRIMARY_DEVICE))
		{
			ui += "<div><h4>Slide Controls</h4><input value=\"left\" type=\"button\" style=\"height:100px; width:100px; font-size:25px\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=left']);\"/>" +
				  "<input value=\"right\" type=\"button\" style=\"height:100px; width:100px; font-size:25px\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=right']);\"/></div>" +
				  "<div><h4>Switch Control Modes</h4>" +
				  "<div><h4>When You Are Finished . . .</h4>" +
				  "<input value=\"Quit Presentation\" type=\"button\" style=\"height:50px; font-size:25px\" onclick=\"device.sendComputeInstruction('QUIT', []);\"/></div>" +
				  "</html>";
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
		
		return new String[] { "UI=" + ui  };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
		
		System.out.println("Received Instruction: " + instruction.toString());
		
		if (instruction.getCommand().equals("KEYPRESS"))
		{
			String key = instruction.getPayload("keycode");
			this.pressKey(key);
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
}
