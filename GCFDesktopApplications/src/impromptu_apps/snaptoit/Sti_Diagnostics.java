package impromptu_apps.snaptoit;


import java.util.ArrayList;
import java.util.Date;

import cern.colt.Arrays;

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

		// Runs an Experiment
		//runExperiment(15, new int[] { 1 }, new int[] { 6, 7, 8, 9, 10, 11 });
		//runExperiment(15, new int[] { 1, 2, 3 }, new int[] { 6, 7, 8, 9, 10, 11 });
		//runExperiment(15, new int[] { 1, 2, 3, 4, 5 }, new int[] { 6, 7, 8, 9, 10, 11 });

		String photo1 = "appData/liveOS/STI_DIAG/12-1.jpeg";
		String photo2 = "appData/liveOS/STI_DIAG/12-12.jpeg";
		
		this.addPhoto(photo1);
		this.addPhoto(photo2);
		this.viewComparison(photo1, photo2);
	}
	
	/**
	 * Runs ONE Experiment
	 * @param numDevices
	 * @param picsToLoad
	 * @param picsToTest
	 */
	private void runExperiment(int numDevices, int[] picsToLoad, int[] picsToTest)
	{	
		// Loads INITIAL Photos
		for (int d=1; d<numDevices+1; d++)
		{
			for (int i : picsToLoad)
			{
				this.addPhoto("appData/liveOS/STI_DIAG/" + d + "-" + i + ".jpeg");	
			}	
		}
				
		// TRIAL 
		double top1 = 0;
		double top3 = 0;
		double top5 = 0;
		double top7 = 0;
		double top9 = 0;
		
		for (int d=1; d<numDevices+1; d++)
		{
			for (int i : picsToTest)
			{
				String result = this.test("appData/liveOS/STI_DIAG/" + d + "-" + i + ".jpeg");	
				
				if (result.charAt(0) == 'Y')
				{
					top1++;
				}
				if (result.charAt(1) == 'Y')
				{
					top3++;
				}
				if (result.charAt(2) == 'Y')
				{
					top5++;
				}	
				if (result.charAt(3) == 'Y')
				{
					top7++;
				}	
				if (result.charAt(4) == 'Y')
				{
					top9++;
				}		
			}
		}

		double top1_avg = ((double)top1/(double)picsToTest.length)/(double)numDevices;
		double top3_avg = ((double)top3/(double)picsToTest.length)/(double)numDevices;
		double top5_avg = ((double)top5/(double)picsToTest.length)/(double)numDevices;
		double top7_avg = ((double)top7/(double)picsToTest.length)/(double)numDevices;
		double top9_avg = ((double)top9/(double)picsToTest.length)/(double)numDevices;
		
		System.out.println("TRIAL COMPLETE: ");// + top1 + "; " + top3 + "; " + top5 + "; " + top7 + "; " + top9);
		System.out.println("PICS LOADED: " + Arrays.toString(picsToLoad));
		System.out.println("PICS TESTED: " + Arrays.toString(picsToTest));
		System.out.printf("TOP 1:" + top1 + " (%1.4f)\n", top1_avg);
		System.out.printf("TOP 3:" + top3 + " (%1.4f)\n", top3_avg);
		System.out.printf("TOP 5:" + top5 + " (%1.4f)\n", top5_avg);
		System.out.printf("TOP 7:" + top7 + " (%1.4f)\n", top7_avg);
		System.out.printf("TOP 9:" + top9 + " (%1.4f)\n", top9_avg);
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
