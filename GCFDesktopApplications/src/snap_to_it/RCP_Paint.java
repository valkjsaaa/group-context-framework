package snap_to_it;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Date;

import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;

public class RCP_Paint extends RemoteControlProvider
{
	private final String WEBSITE_URL = "http://71.182.231.215/gcf/universalremote/Websites/paint.html";
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public RCP_Paint(GroupContextManager groupContextManager) 
	{
		super(groupContextManager);

		this.enableScreenshots(30000, 3);
	}

	/**
	 * This holds static interfaces
	 */
	protected void initializeUserInterfaces()
	{
		super.initializeUserInterfaces();
	}

	/**
	 * This creates dynamic interfaces
	 */
	private String getInterface(int i)
	{
		String ui1 = "<html><title>Paint Palette</title><div>" + 
						"<img border=\"0\" src=\"http://71.182.231.215/gcf/universalremote/Websites/pallete.png\" alt=\"Screenshot\" width=\"100\" >" +
			  		"</div>" +
			  		"</html>";

		String ui2 = "<html><title>Paint Tools</title><div>" + 
				"<img border=\"0\" src=\"http://71.182.231.215/gcf/universalremote/Websites/brushes.png\" alt=\"Screenshot\" width=\"100\">" +
	  		"</div>" +
	  		"</html>";
		
		if (i == 0)
		{
			return ui1;
		}
		else
		{
			return ui2;
		}
	}
	
	@Override
	public void sendMostRecentReading() 
	{
		int i=0;
		
		for (ContextSubscriptionInfo subscription : this.getSubscriptions())
		{
			this.getGroupContextManager().sendContext(
					this.getContextType(), 
					"", 
					new String[] { subscription.getDeviceID() }, 
					new String[] { "UI=" + getInterface(i) });
					//new String[] { "WEBSITE=" + WEBSITE_URL});
			
			i++;
		}
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
		
		if (instruction.getCommand().equals("COLOR"))
		{
			System.out.println("Changing Color to " + instruction.getParameters()[0]);
			
			if (instruction.getParameters()[0].equals("Red"))
			{
				leftClickMouse(31, 616);
			}
			else if (instruction.getParameters()[0].equals("Green"))
			{
				leftClickMouse(31, 676);
			}
			else if (instruction.getParameters()[0].equals("Blue"))
			{
				leftClickMouse(31, 737);
			}
			else if (instruction.getParameters()[0].equals("Yellow"))
			{
				leftClickMouse(31, 647);
			}
			else if (instruction.getParameters()[0].equals("Black"))
			{
				leftClickMouse(31, 587);
			}
			else if (instruction.getParameters()[0].equals("Pink"))
			{
				leftClickMouse(31, 800);
			}
			
			//sendMostRecentReading();
		}
	}
	
	private void pressKey(int key)
	{
		Robot robot = this.getRobot();
		
		robot.keyPress(key);
		robot.delay(40);
		robot.keyRelease(key);
		robot.delay(100);
	}
	
	private void pressKeys(int key1, int key2)
	{
		Robot robot = this.getRobot();
		
		robot.keyPress(key1);
		robot.delay(40);
		robot.keyPress(key2);
		robot.delay(40);
		robot.keyRelease(key1);
		robot.delay(40);
		robot.keyRelease(key2);
		robot.delay(100);
	}

	private void leftClickMouse(int x, int y)
	{
		Robot robot = this.getRobot();
		
		// Saves the Mouse's Location
		Point currentMouseLocation = MouseInfo.getPointerInfo().getLocation();
		
		// Moves the Mouse and Clicks
		robot.mouseMove(x, y);
		robot.mousePress(InputEvent.BUTTON1_MASK);
		robot.delay(50);
		robot.mouseRelease(InputEvent.BUTTON1_MASK);
		
		// Moves the Mouse Back
		robot.mouseMove(currentMouseLocation.x, currentMouseLocation.y);
	}
}
