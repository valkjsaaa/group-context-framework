package impromptu_apps.desktop;

import impromptu_apps.DesktopApplicationProvider;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;

import com.adefreitas.desktopframework.toolkit.HttpToolkit;
import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.liveos.ApplicationElement;
import com.adefreitas.liveos.ApplicationFunction;
import com.adefreitas.liveos.ApplicationObject;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class App_Printer extends DesktopApplicationProvider
{
	// Busy Flag
	private boolean busy;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 */
	public App_Printer(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"PRINT_ZIRCON",
				"Printer App",
				"This application lets you print documents to the HP 9050N printer known as ZIRCON.",
				"AUTOMATION",
				new String[] { },
				new String[] { },
				"http://icons.iconarchive.com/icons/awicons/vista-artistic/96/2-Hot-Printer-icon.png",
				60,
				commMode,
				ipAddress,
				port);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		// Retreives Preferences
		String contextTxt = CommMessage.getValue(subscription.getParameters(), "context");
		System.out.println("CONTEXT: " + contextTxt);
		
		String ui = "<html><title>Printer Controls</title>" + 
				"<div><img src=\"http://www.blankdvdmedia.com/product/laser-printers/hp/images/hp-laserjet-9050dn-laser-toner-cartridge.jpg\" width=\"150\" height=\"150\" alt=\"Printer\"></div>" +
//				"<div>You are connected to: Pewter</div>" +
				((busy) ? 
						"<p style=\"color:white; background-color:red\">Status: PRINTING</p>" :
							"<p style=\"color:white; background-color:green\">Status: READY</p>");
				
		if (contextTxt != null && contextTxt.length() >= 0)
		{
			JsonParser parser 		  = new JsonParser();
			JsonObject obj    		  = (JsonObject)parser.parse(contextTxt);
			JsonObject interactionObj = obj.getAsJsonObject("interaction");
			String     url    		  = (interactionObj == null) ? "" : interactionObj.get("url").getAsString();
			
			if (url.length() > 0)
			{
				ui += "<h4>PRINT THE FOLLOWING RECENT DOCUMENT</h4>";	
				ui += "<div><input value=\"Print " + url + "\" type=\"button\"  height=\"100\" onclick=\"device.sendComputeInstruction('PRINT', ['FILE=" + url + "']);\"/></div>";
				ui += "<h4>PRINT ANOTHER DOCUMENT</h4>";
			}
		}
	
		ui += "<div><input value=\"Print File\" type=\"button\"  height=\"100\" onclick=\"device.uploadFile('UPLOAD_PRINT', 'What do you want to print?', ['.docx', '.pdf']);\"/></div>";	
		ui += "</html>";
		
		return new String[] { "UI=" + ui };
		//return new String[] { "WEBSITE=http://www.google.com" };
	}

	public String getFunctions()
	{
		// Creates the Function
		ApplicationFunction wordFunction 	   = new ApplicationFunction("Print Document", "Prints a *.docx file using default settings.", "PRINT_WORD");
		ApplicationFunction powerPointFunction = new ApplicationFunction("Print Handouts", "Converts a *.pptx file into handouts.", "PRINT_PPTX");
			
		// Adds a Required Object to the Function
		wordFunction.addRequiredObject(new ApplicationObject("FILE_DOCX", "FILE"));
		powerPointFunction.addRequiredObject(new ApplicationObject("FILE_PPTX", "FILE"));
		
		// Generates the JSON that Contains all Functions
		String functionsJSON = ApplicationElement.toJSONArray(new ApplicationElement[] { wordFunction, powerPointFunction });
		System.out.println(functionsJSON);
		
		return functionsJSON;
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

	@Override
	public boolean sendAppData(String contextJSON)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, contextJSON);
		return this.hasEmailAddress(parser, "adrian.defreitas@gmail.com");
	}

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
