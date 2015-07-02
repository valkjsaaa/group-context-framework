package impromptu_apps.snaptoit;


import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;

import org.apache.commons.vfs2.provider.UriParser;

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
	private String  printerName;
	private String  pictureURL;
	
	public Sti_Printer(GroupContextManager groupContextManager, String printerName, String pictureURL, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_PRINTER",
				"Printer Controls",
				"Controls the Printer " + printerName + ".  Powered by Snap-To-It!",
				"Snap-To-It",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"",				   // LOGO
				60,
				commMode,
				ipAddress,
				port);
		
		this.busy 		 = false;
		this.printerName = printerName;
		this.pictureURL  = pictureURL;
		
		this.addPhoto(this.getLocalStorageFolder() + "zircon_0.jpeg", false, false, -96.7897637847936,-56.72318767948723,3.9806380902648395);
		this.addPhoto(this.getLocalStorageFolder() + "zircon_1.jpeg", false, false, -37.776738110586685,-62.589223931166394,7.596522734509842);
		this.addPhoto(this.getLocalStorageFolder() + "zircon_2.jpeg", false, false, -6.997916844800285,-60.86758595840547,1.7737423910094836);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{	
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/printer/print.php?printer=" + this.printerName };
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
	 * OPTIONAL:  Allows you to Customize the Name of the App on a Per User Basis
	 */
	public String getName(String userContextJSON)
	{
		return name + " (" + printerName.toUpperCase() + ")";
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
