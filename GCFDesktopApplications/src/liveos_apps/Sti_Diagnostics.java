package liveos_apps;

import java.util.ArrayList;
import java.util.Date;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.liveos.ApplicationElement;
import com.adefreitas.liveos.ApplicationObject;
import com.adefreitas.messages.ComputeInstruction;

public class Sti_Diagnostics extends SnapToItApplicationProvider
{

	public Sti_Diagnostics(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_DIAG",
				"Snap To It Diagnostic App",
				"Tests STI v2.0.",
				"DEBUG",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://108.32.88.8/gcf/universalremote/magic/gears.png",				   // LOGO
				30,
				commMode,
				ipAddress,
				port);
		
		// Tells STI 
		this.storeUserPhotos = false;
		
		// Loads Photos
		this.addPhoto("appData/liveOS/STI_PRINTER/Pewter1.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/Pewter2.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/Pewter3.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/Zircon1.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/Zircon2.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/Zircon3.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/Color1.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/Color2.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/Color3.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/Copy1.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/Copy2.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/Copy3.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/PewterCopy1.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/PewterCopy2.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/PewterCopy3.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH411.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH412.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH413.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH4509BW1.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH4509BW2.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH4509BW3.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSHRoboticsCopy1.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSHRoboticsCopy2.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSHRoboticsCopy3.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/DevLab1.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/DevLab2.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/DevLab3.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH111.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH112.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH113.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH1CopyGray1.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH1CopyGray2.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH1CopyGray3.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH1CopyBlue1.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH1CopyBlue2.jpeg");
		this.addPhoto("appData/liveOS/STI_PRINTER/NSH1CopyBlue3.jpeg");
		
//		this.test("appData/liveOS/STI_PRINTER/test1.jpeg");
//		this.test("appData/liveOS/STI_PRINTER/test2.jpeg");
//		this.test("appData/liveOS/STI_PRINTER/test3.jpeg");
//		this.test("appData/liveOS/STI_PRINTER/test4.jpeg");
//		this.test("appData/liveOS/STI_PRINTER/test5.jpeg");
//		this.test("appData/liveOS/STI_PRINTER/test6.jpeg");
//		this.test("appData/liveOS/STI_PRINTER/test7.jpeg");
//		this.test("appData/liveOS/STI_PRINTER/test8.jpeg");
//		this.test("appData/liveOS/STI_PRINTER/test9.jpeg");
//		this.test("appData/liveOS/STI_PRINTER/test10.jpeg");
//		this.test("appData/liveOS/STI_PRINTER/test11.jpeg");
//		this.test("appData/liveOS/STI_PRINTER/test12.jpeg");
//		this.test("appData/liveOS/STI_PRINTER/test13.jpeg");
//		this.test("appData/liveOS/STI_PRINTER/test14.jpeg");
	}
	
	public ArrayList<String> getInformation()
	{		
		ArrayList<String> result = new ArrayList<String>();
		
		result.add("APP_ID=" + appID);
		result.add("APP_CONTEXT_TYPE=" + CONTEXT_TYPE);
		result.add("DEVICE_ID=" + this.getGroupContextManager().getDeviceID());
		result.add("NAME=" + name);
		result.add("DESCRIPTION=" + getDebugDescription());
		result.add("CATEGORY=" + category);
		result.add("CONTEXTS=");
		result.add("PREFERENCES=");
		result.add("LOGO=" + logoPath);
		result.add("LIFETIME=" + lifetime);
		result.add("FUNCTIONS="	+ getFunctions());
		result.add("COMM_MODE="	+ commMode.toString());
		result.add("APP_ADDRESS=" + ipAddress);
		result.add("APP_PORT=" + Integer.toString(port));
		result.add("APP_CHANNEL=" + channel);
		
		return result;
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String ui  = "<html><title>Snap-To-It Diagnostics</title>";
		ui 	       += "<div>";
		ui		   += this.getDebugDescription().replace("\n", "<br />");
		ui 	       += "</div>";
				
		// Delivers the UI Code, as Well as the Objects
		//System.out.println("OBJECTS = " + objects);
		return new String[] { "UI=" + ui};
		//return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/diagnostics/index.html", "OBJECTS=" + objects};
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	
	
}
