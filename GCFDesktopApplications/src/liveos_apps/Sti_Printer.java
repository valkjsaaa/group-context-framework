package liveos_apps;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;

import com.adefreitas.desktopframework.toolkit.HttpToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Sti_Printer extends SnapToItApplicationProvider
{	
	// Busy Flag
	private boolean busy;
	
	public Sti_Printer(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_PRINTER",
				"Printer App",
				"Controls the Printer Pewter.  Powered by Snap-To-It!",
				"DEBUG",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"",				   // LOGO
				30,
				commMode,
				ipAddress,
				port);
		
		this.addPhoto(this.getLocalStorageFolder() + "Pewter1.jpeg");
		this.addPhoto(this.getLocalStorageFolder() + "Pewter2.jpeg");
		this.addPhoto(this.getLocalStorageFolder() + "Pewter3.jpeg");
		
		this.busy = false;
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		// Retreives Preferences
		String contextTxt = CommMessage.getValue(subscription.getParameters(), "context");
		System.out.println("CONTEXT: " + contextTxt);
		
		String ui = "<html><title>Printer Controls</title>" + 
				"<div><img src=\"http://www.blankdvdmedia.com/product/laser-printers/hp/images/hp-laserjet-9050dn-laser-toner-cartridge.jpg\" width=\"200\" height=\"200\" alt=\"Printer\"></div>";
				
		if (contextTxt != null && contextTxt.length() >= 0)
		{
			JsonParser parser 		  = new JsonParser();
			JsonObject obj    		  = (JsonObject)parser.parse(contextTxt);
			JsonObject interactionObj = obj.getAsJsonObject("interaction");
			String     url    		  = (interactionObj == null) ? "" : interactionObj.get("url").getAsString();
			
//			if (url.length() > 0)
//			{
//				ui += "<h4>PRINT THE FOLLOWING RECENT DOCUMENT</h4>";	
//				ui += "<div><input value=\"Print " + url + "\" type=\"button\"  height=\"100\" onclick=\"device.sendComputeInstruction('PRINT', ['FILE=" + url + "']);\"/></div>";
//				ui += "<h4>PRINT ANOTHER DOCUMENT</h4>";
//			}
		}
	
		ui += "<div><input value=\"Print File\" type=\"button\" width-\"400\" height=\"200\" style=\"height:80px; font-size:50px\" onclick=\"device.uploadFile('UPLOAD_PRINT', 'What do you want to print?', ['.docx', '.pdf']);\"/></div>";	
		ui += "</html>";

		return new String[] { "UI=" + ui };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
		
		if (instruction.getCommand().equals("UPLOAD_PRINT") && !busy)
		{
			busy = true;
			
			sendContext();
			
			String filePath = instruction.getPayload("uploadPath");
			System.out.println("I got notified of an upload at: " + filePath);
			
			print(filePath, instruction.getDeviceID());
		}
		else if (instruction.getCommand().equals("PRINT") && !busy)
		{
			busy = true;
			
			sendContext();
			
			String filePath = instruction.getPayload("FILE");
			System.out.println("*** I got told to print: " + filePath + " ***");
			
			print(filePath, instruction.getDeviceID());
		}
		else if (instruction.getCommand().equals("REMOTE_PRINT") && !busy)
		{
			System.out.println("*** I got remotely told to print something. ***");
		}
	}
	
	/**
	 * Helper Function to Print
	 * @param filePath
	 * @param deviceID
	 */
	private void print(String filePath, String deviceID)
	{
		String folder      = filePath.substring(0, filePath.lastIndexOf("/") + 1);
		String filename    = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length());
		String destination = this.getLocalStorageFolder() + deviceID.replace(" ", "") + "/" + filename;
		
		System.out.println("Folder: " + folder);
		System.out.println("File:   " + filename);
		System.out.println("Dest:   " + destination);
		
		// Tries to Download the File
		// TODO:  Hook to Cloud Service!
		HttpToolkit.downloadFile(folder + filename, destination);
		File file = new File(destination);
		
		if (file.exists())
		{
			// Opens the Application
			this.executeRuntimeCommand("open " + file.getAbsolutePath());
			
			Robot robot = this.getRobot();
			
			// Runs the Ctrl-P Command
			robot.delay(2000);
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
		else
		{
			System.out.println("Could Not Find File: " + destination);
		}
		
		busy = false;
		
		sendContext();
	}

}
