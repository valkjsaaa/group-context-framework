package impromptu_apps.snaptoit;

import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class Sti_Template extends SnapToItApplicationProvider
{

	public static final String APP_CONTEXT_TYPE = "STI_TEMPLATE";
	public static final String APP_NAME			= "Example Snap-To-It App";
	public static final String APP_DESCRIPTION  = "A sample example application built on the snap-to-it platform.";
	public static final String APP_LOGO		    = "http://icons.iconarchive.com/icons/double-j-design/origami-colored-pencil/256/blue-camera-icon.png";
	
	// Optional
	public static final double APP_SCREENSHOT_AZIMUTH = 230.0;
	public static final double APP_SCREENSHOT_PITCH   = 280.0;
	public static final double APP_SCREENSHOT_ROLL    = 0.0;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param commMode The means by which this device communicates with other devices (TCP, MQTT)
	 * @param ipAddress The IP address
	 * @param port The network port
	 */
	public Sti_Template(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				APP_CONTEXT_TYPE,
				APP_NAME,
				APP_DESCRIPTION,
				"SNAP-TO-IT",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"",				   // LOGO
				120,
				commMode,
				ipAddress,
				port);
		
		// Example #1:  Enable Realtime Screenshots (With Azimuth, Pitch, and Roll)
		//this.enableScreenshots(300000, 1, APP_SCREENSHOT_AZIMUTH, APP_SCREENSHOT_PITCH, APP_SCREENSHOT_ROLL);
		
		// Example #2:  Enable "Just In Time" Screenshots (With Azimuth, Pitch, and Roll)
		this.enableRealtimeScreenshots(APP_SCREENSHOT_AZIMUTH, APP_SCREENSHOT_PITCH, APP_SCREENSHOT_ROLL);
		
		// Example #3:  Enable Photo (Requires Metadata; Use the STI App to Gather it!)
//		this.addAppliancePhotoFromURL(new String[] {
//				"http://gcf.cmu-tbank.com/snaptoit/appliances/digitalprojector/digitalprojector_0.jpeg",
//				"http://gcf.cmu-tbank.com/snaptoit/appliances/digitalprojector/digitalprojector_1.jpeg",
//				"http://gcf.cmu-tbank.com/snaptoit/appliances/digitalprojector/digitalprojector_2.jpeg",
//				});
	}
	
	/**
	 * This returns an Interface for a Specific User
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/doorplate/index.html"};
	}

	/**
	 * This processes an instruction sent by an application
	 */
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	/**
	 * OPTIONAL:  Allows you to Customize the Name of the App on a Per User Basis
	 */
	public String getName(String userContextJSON)
	{
		return name;
	}
	
	/**
	 * OPTIONAL:  Allows you to Customize the Category of the App on a Per User Basis
	 */
	public String getCategory(String userContextJSON)
	{
		return category;
	}
	
	/**
	 * OPTIONAL:  Allows you to Customize the Description of the App on a Per User Basis
	 */
	public String getDescription(String userContextJSON)
	{
		return description;
	}
	
	/**
	 * OPTIONAL:  Allows you to Customize the Lifetime of an App on a Per User Basis
	 */
	public int getLifetime(String userContextJSON)
	{
		return this.lifetime;
	}

	/**
	 * OPTIONAL:  Allows you to Customize the Logo of an App on a Per User Basis
	 */
	public String getLogoPath(String userContextJSON)
	{
		return logoPath;
	}
}
