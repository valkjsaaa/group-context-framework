package impromptu_apps.snaptoit;


import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Date;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.Settings;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.HttpToolkit;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.impromptu.ApplicationElement;
import com.adefreitas.gcf.impromptu.ApplicationObject;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class Sti_DigitalProjector extends SnapToItApplicationProvider
{		
	// Provider Specific Variables Go Here
	public		  String PRIMARY_DEVICE		   = "";
	public 		  String PRESENTATION_FILE     = "";	// The File (ON THE COMPUTER)
	public		  String PRESENTATION_URL      = "";    // The cloud location where this file was received
	public static String PRESENTATION_LOCATION = "";	// The Path to the File (ON THE COMPUTER)
	
	public boolean listMode = true;
	
	public Sti_DigitalProjector(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_DIGITAL_PROJECTOR",
				"Digital Projector (EIKI 1)",
				"Lets you upload PowerPoint presentations and control them on this digital projector.",
				"Snap-To-It",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://png-4.findicons.com/files/icons/2711/free_icons_for_windows8_metro/128/video_projector.png",				   // LOGO
				60,
				commMode,
				ipAddress,
				port);
		
		this.addAppliancePhotoFromURL(new String[] {
				"http://gcf.cmu-tbank.com/snaptoit/appliances/digitalprojector/digitalprojector_0.jpeg",
				"http://gcf.cmu-tbank.com/snaptoit/appliances/digitalprojector/digitalprojector_1.jpeg",
				"http://gcf.cmu-tbank.com/snaptoit/appliances/digitalprojector/digitalprojector_2.jpeg",
				});
		
		// Takes a Photo At the Moment a New Photo Comes In
		//this.enableRealtimeScreenshots();
	}
	
	/**
	 * Determines Whether or Not to Send Application Data
	 */
	@Override
	public boolean sendAppData(String bluewaveContext)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, bluewaveContext);
		
		if (listMode)
		{
			return this.getDeviceID(parser).equals("Device 1");
		}
		else
		{
			return super.sendAppData(bluewaveContext);
		}
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String ui = "<html><title>Digital Projector</title>";
		
		if (PRESENTATION_FILE.length() == 0)
		{
			ui += "<div><input value=\"Upload Presentation\" type=\"button\" style=\"height:50px; width:300px; font-size:25px\" onclick=\"device.uploadFile('PP_UPLOADED', 'Pick Your Presentation', ['.pptx']);\"/></div>";
			ui += "</html>";
			
			return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/powerpoint/upload_presentation.html"};
		}
		else
		{
			if (PRIMARY_DEVICE.length() == 0 || subscription.getDeviceID().equals(PRIMARY_DEVICE))
			{
				ui += "<div><h4>Slide Controls</h4><input value=\"left\" type=\"button\" style=\"height:100px; width:100px; font-size:25px\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=left']);\"/>" +
					  "<input value=\"right\" type=\"button\" style=\"height:100px; width:100px; font-size:25px\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=right']);\"/></div>" +
					  "<div><h4>Switch Control Modes</h4>" +
					  "<div><h4>When You Are Finished . . .</h4>" +
					  "<input value=\"Quit Presentation\" type=\"button\" style=\"height:50px; font-size:25px\" onclick=\"device.sendComputeInstruction('QUIT', []);\"/></div>" +
					  "</html>";
				
				// Creates an Object for this Application
				ApplicationObject obj = new ApplicationObject(this.getAppID(), "FILE_PPTX", PRESENTATION_URL);
				
				// Converts Objects into a JSON String
				String objects = ApplicationElement.toJSONArray(new ApplicationElement[] { obj }) ;
						
				// Delivers the UI Code, as Well as the Objects
				//System.out.println("OBJECTS = " + objects);
				return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/powerpoint/control_presentation.html", "OBJECTS=" + objects};
			}
			else
			{
				ui += "<div>" + 
						"<h5>Last Updated: " + new Date().toString() + "</h5><h5>Latest Screenshot</h5>"+
						"<img border=\"0\" src=\"http://" + Settings.DEV_WEB_IP + "/gcf/universalremote/Server/screen.jpeg\" alt=\"Screenshot\" width=\"304\" height=\"228\">" +
						"<input value=\"Download Presentation\" type=\"button\" style=\"height:50px; font-size:25px\" onclick=\"device.downloadFile('" + this.getLocalStorageFolder() + PRESENTATION_FILE + "');\"/>" + 
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
			this.PRESENTATION_URL = folder + filename;
			
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
				//cloudToolkit.uploadFile(UPLOAD_FOLDER, new File(PRESENTATION_LOCATION));
								
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
	
	public String getCategory(String userContextJSON)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, userContextJSON);
		boolean stiData = parser.getJSONObject("snap-to-it").has("PHOTO") || parser.getJSONObject("snap-to-it").has("CODE");
		
		if (stiData)
		{
			return "SNAP-TO-IT";
		}
		else
		{
			return "DEVICES";
		}
		//return category;
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
				
//				Robot robot = this.getRobot();
//				
//				robot.keyPress(KeyEvent.VK_ESCAPE);
//				robot.delay(40);
//				robot.keyRelease(KeyEvent.VK_ESCAPE);
//				robot.delay(1000);
//				
//				robot.keyPress(KeyEvent.VK_META);
//				robot.delay(40);
//				robot.keyPress(KeyEvent.VK_Q);
//				robot.delay(40);
//				robot.keyRelease(KeyEvent.VK_META);
//				robot.delay(40);
//				robot.keyRelease(KeyEvent.VK_Q);
//				robot.delay(1000);
			}
		}
		
		//this.getGroupContextManager().cancelRequest("ACC", subscription.getDeviceID());
		
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
	
//	    // TODO:  Experimental . . .
//	    File screenshot = ScreenshotToolkit.takeScreenshot(320, 240, this.getLocalStorageFolder() + "screen");
//	    cloudToolkit.uploadFile(UPLOAD_FOLDER, screenshot);
//	    
//	    System.out.println("Uploaded New Screenshot");
//	    this.sendContext();
	}
}
