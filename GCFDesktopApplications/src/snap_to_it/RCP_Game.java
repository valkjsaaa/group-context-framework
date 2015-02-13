package snap_to_it;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;

import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;

public class RCP_Game extends RemoteControlProvider
{
	// Provider Specific Variables Go Here
	private final String WEBSITE_URL_1 = "http://71.182.231.215/gcf/universalremote/Websites/controller.html";
	private final String WEBSITE_URL_2 = "http://71.182.231.215/gcf/universalremote/Websites/controller2.html";
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public RCP_Game(GroupContextManager groupContextManager) 
	{
		super(groupContextManager);
		this.enableScreenshots(5000, 3);
		//this.addPhoto(APP_DATA_FOLDER + "game1.jpg");
	}

	protected void initializeUserInterfaces()
	{
		super.initializeUserInterfaces();
	}
	
	public void sendMostRecentReading()
	{
		for (ContextSubscriptionInfo subscription : this.getSubscriptions())
		{
			// Prints out Preferences
			ArrayList<String> preferences = CommMessage.getValues(subscription.getParameters(), "preferences");
			String 			  controller  = (preferences != null) ? CommMessage.getValue(preferences.toArray(new String[0]), "controller") : null;
			String			  website	  = (controller != null && controller.equals("2")) ? WEBSITE_URL_2 : WEBSITE_URL_1;			
			System.out.println("Controller Preference " + controller + ": " + website);
			
			this.getGroupContextManager().sendContext(
					this.getContextType(), 
					"", 
					new String[] { subscription.getDeviceID() }, 
					new String[] { "WEBSITE=" + website });
		}
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
		
		if (instruction.getCommand().equals("KEYPRESS"))
		{
			String key = CommMessage.getValue(instruction.getParameters(), "keycode");
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
