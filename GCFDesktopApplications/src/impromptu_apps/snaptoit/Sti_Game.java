package impromptu_apps.snaptoit;


import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.HttpToolkit;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.messages.CommMessage;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Sti_Game extends SnapToItApplicationProvider
{		
	// Provider Specific Variables Go Here
	private final String WEBSITE_URL = "http://gcf.cmu-tbank.com/apps/gamecontroller/index.html";
		
	public boolean listMode = true;
	
	public Sti_Game(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_GAME_CONTROLLER",
				"Macbook Air",
				"Lets you control the application on this computer.",
				"SNAP-TO-IT",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://www.gedtestingservice.com/uploads/images/medium/0a54c4f41f9bb1a1b74fe0cdaedbc0a7.jpeg",
				//"http://inwallspeakers1.com/wp-content/uploads/2014/06/gaming-controller-icon.png",				   // LOGO
				30,
				commMode,
				ipAddress,
				port);
		
		//this.setDebugMode(f);
		
		//this.enableScreenshots(5000, 3, 0.0, 0.0, 0);
		// Takes a Photo At the Moment a New Photo Comes In
		this.enableRealtimeScreenshots(0.0, 0.0, 0);
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
		// Retreives Preferences
		String contextTxt = CommMessage.getValue(subscription.getParameters(), "context");
		System.out.println("CONTEXT: " + contextTxt);

		return new String[] { "WEBSITE=" + WEBSITE_URL };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
		
		System.out.println("Received Instruction: " + instruction.toString());
		
		if (instruction.getCommand().equals("KEYPRESS"))
		{
			String key = instruction.getPayload("keycode");
			int keycode = -1;
			
			if (key.equals("up"))
			{
				keycode = KeyEvent.VK_UP;
			}
			else if (key.equals("down"))
			{
				keycode = KeyEvent.VK_DOWN;
			}
			else if (key.equals("left"))
			{
				keycode = KeyEvent.VK_LEFT;
			}
			else if (key.equals("right"))
			{
				keycode = KeyEvent.VK_RIGHT;
			}
			else if (key.equals("a"))
			{
				keycode = KeyEvent.VK_D;
			}
			else if (key.equals("b"))
			{
				keycode = KeyEvent.VK_S;
			}
			else if (key.equals("start"))
			{
				keycode = KeyEvent.VK_ENTER;
			}
			else if (key.equals("select"))
			{
				keycode = KeyEvent.VK_SPACE;
			}
			
			System.out.println("Pressing " + keycode);
			
			Robot robot = this.getRobot();
			
		    //robot.delay(10);
		    robot.keyPress(keycode);
		    robot.delay(20);
		    robot.keyRelease(keycode);
		}
	}
	
}
