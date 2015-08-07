package impromptu_apps.snaptoit;


import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class Sti_Paint extends SnapToItApplicationProvider
{
	// Provider Specific Variables Go Here
	private final String WEBSITE_URL = "http://gcf.cmu-tbank.com/apps/paint/new_paint.html";
	
	public Sti_Paint(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_PAINT",
				"Paint Assistant Tools",
				"Paint Tools",
				"DEBUG",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"",				   // LOGO
				30,
				commMode,
				ipAddress,
				port);
		
		this.enableScreenshots(5000, 3, 0, 0, 0);
		
		setDebugMode(true);
	}
	
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=" + WEBSITE_URL};
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
		
		System.out.println("Received Instruction: " + instruction.toString());
		
		if (instruction.getCommand().equals("COLOR"))
		{
			System.out.println("Changing Color to " + instruction.getPayload(0));
			
			if (instruction.getPayload(0).equals("Red"))
			{
				leftClickMouse(31, 616);
			}
			else if (instruction.getPayload(0).equals("Green"))
			{
				leftClickMouse(31, 676);
			}
			else if (instruction.getPayload(0).equals("Blue"))
			{
				leftClickMouse(31, 737);
			}
			else if (instruction.getPayload(0).equals("Yellow"))
			{
				leftClickMouse(31, 647);
			}
			else if (instruction.getPayload(0).equals("Black"))
			{
				leftClickMouse(31, 587);
			}
			else if (instruction.getPayload(0).equals("Pink"))
			{
				leftClickMouse(31, 800);
			}
		}
	}
	
	// HELPER METHODS --------------------------------------------------------------------
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
