package snap_to_it;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import toolkits.ScreenshotToolkit;

import com.adefreitas.desktopframework.DesktopGroupContextManager;
import com.adefreitas.desktopframework.MessageProcessor;
import com.adefreitas.desktoptoolkits.DrawingPanel;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;
import com.adefreitas.messages.ContextData;

public class RCP_Experimental extends RemoteControlProvider implements MessageProcessor
{
	// Provider Specific Variables Go Here
	public		  String PRIMARY_DEVICE		   = "";
	public 		  String PRESENTATION_FILE     = "";                 							// The File (ON THE COMPUTER)
	public static String PRESENTATION_LOCATION = "";											// The Path to the File (ON THE COMPUTER)
	public static String UPLOAD_FOLDER         = "/var/www/html/gcf/universalremote/Server/";	// The Folder Containing the Presentation (ON SERVER)
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public RCP_Experimental(DesktopGroupContextManager groupContextManager) 
	{
		super(groupContextManager);
		
		// Enables Screenshot Comparison
		this.addPhoto(APP_DATA_FOLDER + "projector1.jpg");
		this.addPhoto(APP_DATA_FOLDER + "projector2.jpg");
		this.addPhoto(APP_DATA_FOLDER + "projector3.jpg");
		//this.enableScreenshots(10000, 3);
		
		groupContextManager.registerOnMessageProcessor(this);
		
		TestThread t = new TestThread();
		t.start();
	}

	private double intensity;
	
	
	private class TestThread extends Thread
	{
		public void run()
		{	
			DrawingPanel panel = new DrawingPanel(640, 480);
			Graphics2D   pen   = panel.getGraphics();
			
			while (true)
			{
				panel.setWindowTitle("Intensity: " + intensity + "; Updated: " + new Date());
				
				pen.setColor(Color.WHITE);
				pen.fillRect(0, 0, panel.getWindow().getWidth(), panel.getWindow().getHeight());
				
				double percentage = (intensity / 255.0);
				
				pen.setColor(new Color(255, 192, 0, (int)intensity));
				
				pen.fillOval(panel.getWindow().getWidth()/2-50, 
						panel.getWindow().getHeight()/2-50, 
						100, 
						100);
				
				panel.copyGraphicsToScreen();
				
				panel.sleep(100);
			}
		}	
	}
	
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
	
	protected void initializeUserInterfaces()
	{
		super.initializeUserInterfaces();
	}
	
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		// Prints out Preferences
		ArrayList<String> preferences = CommMessage.getValues(newSubscription.getParameters(), "preferences");
		
		if (preferences != null)
		{
			for (String preference : preferences)
			{
				System.out.println(" *** PREFERENCE: " + preference + " ***");
			}
		}
		
		// Subscribes to their light provider
		this.getGroupContextManager().sendRequest("LGT", new String[] { newSubscription.getDeviceID() }, 250, new String[0]);
	}
	
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
		
		this.getGroupContextManager().cancelRequest("LGT", subscription.getDeviceID());
		
		sendMostRecentReading();
	}
	
	public void sendMostRecentReading()
	{
		for (ContextSubscriptionInfo subscription : this.getSubscriptions())
		{
			String ui = "<html><title>Experimental :)</title>";
			
			// TODO:  Hack for Comm Talk
			ui += "<div><h4>See what happens.</h4></html>";
			
			System.out.println("Sending User Interface to: " + subscription.getDeviceID());
			
			this.getGroupContextManager().sendContext(
					this.getContextType(), 
					"", 
					new String[] { subscription.getDeviceID() }, 
					new String[] { "UI=" + ui  });
		}
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
		
		Robot robot = this.getRobot();
		
		if (instruction.getCommand().equals("KEYPRESS"))
		{
			String key = CommMessage.getValue(instruction.getParameters(), "keycode");
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
						
		    robot.delay(40);
		    robot.keyPress(keycode);
		    robot.delay(40);
		    robot.keyRelease(keycode);
		
		    // TODO:  Experimental . . .
		    File screenshot = ScreenshotToolkit.takeScreenshot(640, 480, APP_DATA_FOLDER + "screen");
		    this.getCloudStorageToolkit().uploadFile(UPLOAD_FOLDER, screenshot);
		    
		    System.out.println("Uploaded New Screenshot");
		    this.sendMostRecentReading();
		}
		else if (instruction.getCommand().equals("PP_UPLOADED"))
		{		
			PRIMARY_DEVICE = instruction.getDeviceID();
			System.out.println("PRIMARY_DEVICE: " + PRIMARY_DEVICE);
			
			String filePath = CommMessage.getValue(instruction.getParameters(), "uploadPath");
			System.out.println("I got notified of an upload at: " + filePath);
			
			// TODO:  Remove me:  I test preferences Code
			this.setUserPreference(PRIMARY_DEVICE, "presentation", filePath);
			
			String folder      = filePath.substring(0, filePath.lastIndexOf("/") + 1);
			String filename    = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length());
			String destination = APP_DATA_FOLDER + instruction.getDeviceID().replace(" ", "") + "/" + filename;
			
			System.out.println("Folder: " + folder);
			System.out.println("File:   " + filename);
			System.out.println("Dest:   " + destination);
			
			// Tries to Download the File
			this.getCloudStorageToolkit().downloadFile(folder + filename, destination);
			
			File file = new File(destination);
			
			if (file.exists())
			{
				PRESENTATION_FILE     = filename;
				PRESENTATION_LOCATION = destination;
				
				// Uploads the Presentation to Dropbox
				this.getCloudStorageToolkit().uploadFile(UPLOAD_FOLDER, new File(PRESENTATION_LOCATION));
								
				this.sendMostRecentReading();
				
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
			robot.delay(1000);
			
			robot.keyPress(KeyEvent.VK_META);
			robot.delay(40);
			robot.keyPress(KeyEvent.VK_Q);
			robot.delay(40);
			robot.keyRelease(KeyEvent.VK_META);
			robot.delay(40);
			robot.keyRelease(KeyEvent.VK_Q);
			robot.delay(1000);
			
			this.sendMostRecentReading();
		}
	}

	
	// Game Controls
	boolean pressed = false;
	
	@Override
	public void onMessage(CommMessage message)
	{
		Robot robot = this.getRobot();
		
		if (message instanceof ContextData)
		{
			intensity = ((ContextData) message).getValuesAsDoubles()[0];
			
			//System.out.printf("LIGHT: %1.2f %s\n", intensity, pressed);
			
//			if (value < 50.0)
//			{
//				if (!pressed)
//				{
//					robot.mousePress(InputEvent.BUTTON1_MASK);
//					pressed = true;
//				}
//			}
//			else 
//			{
//				if (pressed)
//				{
//					robot.mouseRelease(InputEvent.BUTTON1_MASK);
//					pressed = false;
//				}
//			}
		}
	}

	

}
