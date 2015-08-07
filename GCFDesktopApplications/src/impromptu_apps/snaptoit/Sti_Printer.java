package impromptu_apps.snaptoit;


import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.HttpToolkit;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.impromptu.ApplicationElement;
import com.adefreitas.gcf.impromptu.ApplicationFunction;
import com.adefreitas.gcf.impromptu.ApplicationObject;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Sti_Printer extends SnapToItApplicationProvider
{	
	// Busy Flag
	private boolean busy;
	private String  printerName;
	private String  networkName;
	
	public Sti_Printer(GroupContextManager groupContextManager, String printerName, String networkName, String[] photos, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_PRINTER_" + printerName,
				"Printer Controls (" + printerName + ")",
				"Controls the Printer " + printerName + ".  Powered by Snap-To-It!",
				"Snap-To-It",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://icons.iconarchive.com/icons/iconshock/real-vista-computer-gadgets/256/multifunction-printer-icon.png", // LOGO
				60,
				commMode,
				ipAddress,
				port);
		
		this.busy 		 = false;
		this.printerName = printerName;
		this.networkName = networkName;
		
		// Adds Photos
		this.addAppliancePhotoFromURL(photos);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{	
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/printer/print.php?printer=" + this.printerName.replace(" ", "%20") };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
		
		System.out.println(this.getContextType() + " received command: " + instruction.getCommand() + "\n  " + instruction.toString());
		
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
		else if (instruction.getCommand().equals("PRINT_PPTX") || instruction.getCommand().equals("PRINT_WORD"))
		{
			try
			{
				JsonParser parser   = new JsonParser();
				JsonObject obj      = (JsonObject)parser.parse(instruction.getPayload(0));
				String     filePath = obj.get("name").getAsString();
				
				print(filePath, instruction.getDeviceID());
			}
			catch (Exception ex)
			{
				System.out.println("Could not parse JSON.  Ignoring Command.");
			}
		}
	}
	
	public String getFunctions()
	{
		// Creates the Function
		ApplicationFunction wordFunction 	   = new ApplicationFunction(this.getAppID(), "Print Document (" + printerName + ")", "Prints a *.docx file using default settings.", "PRINT_WORD");
		ApplicationFunction powerPointFunction = new ApplicationFunction(this.getAppID(), "Print Handouts (" + printerName + ")", "Converts a *.pptx file into handouts.", "PRINT_PPTX");
			
		// Adds a Required Object to the Function
		wordFunction.addRequiredObject(new ApplicationObject("", "FILE_DOCX", "FILE"));
		powerPointFunction.addRequiredObject(new ApplicationObject("", "FILE_PPTX", "FILE"));
		
		// Generates the JSON that Contains all Functions
		String functionsJSON = ApplicationElement.toJSONArray(new ApplicationElement[] { wordFunction, powerPointFunction });
		System.out.println(functionsJSON);
		
		return functionsJSON;
	}
	
	public String getCategory(String userContextJSON)
	{
		// JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, userContextJSON);
		return category;
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
		
		// Determines What Operating System is Being Used
		String OS = System.getProperty("os.name").toLowerCase();
		System.out.println("Operating System: " + OS);
		
		if (file.exists())
		{
			System.out.println("File Downloaded: " + destination);
			
			if (OS.indexOf("win") >= 0) 
			{
				System.out.println("Using Windows Print Method");
				printWindows(file);
			}
			else if (OS.indexOf("mac") >= 0)
			{
				System.out.println("Using Mac Print Method");
				printMac(file);
			}
			else
			{
				System.out.println("Unsupported OS Detected: " + OS);
			}
		}
		else
		{
			System.out.println("Could Not Find File: " + destination);
		}
		
		busy = false;
		
		sendContext();
	}

	private void printMac(File file)
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
	
	private void printWindows(File file)
	{
		// Step 1:  Make this Device the Default
		this.executeRuntimeCommand("RUNDLL32 PRINTUI.DLL,PrintUIEntry /y /n \"" + networkName + "\"");
		
		// Step 2:  Print		
		if (file.getAbsolutePath().endsWith("docx"))
		{
			String command = String.format("\"%s\" \"%s\" /q /n /mFilePrintDefault /mFileClose /mFileExit", 
					"C:\\Program Files (x86)\\Microsoft Office\\Office14\\WINWORD.EXE", file.getAbsolutePath());
			this.executeRuntimeCommand(command);
		}
		else if (file.getAbsolutePath().endsWith("pptx"))
		{
			String command = String.format("\"%s\" /PT \"%s\" \"\" \"\" \"%s\"", 
					"C:\\Program Files (x86)\\Microsoft Office\\Office14\\POWERPNT.EXE", this.networkName, file.getAbsolutePath());
			this.executeRuntimeCommand(command);
		}
		else if (file.getAbsolutePath().endsWith("pdf"))
		{
			String command = String.format("\"%s\" /t \"%s\"", 
					"C:\\Program Files (x86)\\Adobe\\Acrobat Reader DC\\Reader\\AcroRd32.exe", file.getAbsolutePath());
			this.executeRuntimeCommand(command);
		}
		else
		{
			System.out.println("Unsupported File Type: " + file.getAbsoluteFile());
		}
	}
	
}
