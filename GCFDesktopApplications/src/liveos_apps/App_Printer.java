package liveos_apps;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;

import com.adefreitas.desktoptoolkits.HttpToolkit;
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
				"PRINT_PEWTER",
				"Printer (Pewter)",
				"This application lets you print documents to the HP 9050N printer in NSH 2nd Floor known as PEWTER.",
				"AUTOMATION",
				new String[] { },
				new String[] { },
				"http://74.111.161.33/gcf/universalremote/magic/ink.png",
				30,
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
				"<div><img src=\"http://www.blankdvdmedia.com/product/laser-printers/hp/images/hp-laserjet-9050dn-laser-toner-cartridge.jpg\" width=\"200\" height=\"200\" alt=\"Printer\"></div>" +
				"<div>You are connected to: Pewter</div>" +
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
		ApplicationFunction function = new ApplicationFunction("Print Document", "Prints a *.docx file using default settings.", "REMOTE_PRINT");
			
		// Adds a Required Object to the Function
		function.addRequiredObject(new ApplicationObject("FILE_DOCX", "FILE"));
		
		// Generates the JSON that Contains all Functions
		String functionsJSON = ApplicationElement.toJSONArray(new ApplicationElement[] { function });
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
			
			sendMostRecentReading();
			
			String filePath = CommMessage.getValue(instruction.getParameters(), "uploadPath");
			System.out.println("I got notified of an upload at: " + filePath);
			
			print(filePath, instruction.getDeviceID());
		}
		else if (instruction.getCommand().equals("PRINT") && !busy)
		{
			busy = true;
			
			sendMostRecentReading();
			
			String filePath = CommMessage.getValue(instruction.getParameters(), "FILE");
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
		//JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, contextJSON);
		//return getDevices(parser).contains("ZTE-Office");
		return true;
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
		
		sendMostRecentReading();
	}
}
