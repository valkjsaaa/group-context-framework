package impromptu_apps.snaptoit;


import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;

import com.adefreitas.desktopframework.toolkit.HttpToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Sti_Game extends SnapToItApplicationProvider
{		
	// Provider Specific Variables Go Here
	private final String WEBSITE_URL = "http://gcf.cmu-tbank.com/apps/gamecontroller/index.html";
	
	public Sti_Game(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_GAME_CONTROLLER",
				"Game Controller",
				"Game Controller Application",
				"DEBUG",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://inwallspeakers1.com/wp-content/uploads/2014/06/gaming-controller-icon.png",				   // LOGO
				30,
				commMode,
				ipAddress,
				port);
		
		//this.enableScreenshots(5000, 3);
		// Takes a Photo At the Moment a New Photo Comes In
		this.enableRealtimeScreenshots();
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
