package liveos_apps;

import java.awt.Graphics2D;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import liveos_dns.GeoMath;
import bluetoothcontext.toolkit.JSONContextParser;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.liveos.ApplicationProvider;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public abstract class DesktopApplicationProvider extends ApplicationProvider
{
	// Java Robot
	private Robot robot;
	
	// Link to the Folder Where APP Data is Stored
	private static String APP_DATA_FOLDER = "appData/liveOS/";
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public DesktopApplicationProvider(GroupContextManager groupContextManager, 
			String contextType, 
			String name, 
			String description, 
			String category,
			String[] contextsRequired, 
			String[] preferencesToRequest, 
			String logoPath, 
			int lifetime,
			CommMode commMode,
			String ipAddress, 
			int port) 
	{
		super(groupContextManager, contextType, name, description, category, contextsRequired, preferencesToRequest, logoPath, lifetime, commMode, ipAddress, port);
		
		try
		{
			System.out.print("Initializing Application " + name + " . . . ");
			robot = new Robot();
			System.out.println("DONE!");
		}
		catch (Exception ex)
		{
			System.out.println("A problem occurred while creating the robot: " + ex.getMessage());
		}
	}

	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		// Sends the UI Immediately
		sendMostRecentReading();
		
		// Determines Credentials
		String username = CommMessage.getValue(newSubscription.getParameters(), "credentials");
		System.out.println("Subscription [" + this.getContextType() + "]: " + username);
	}
	
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		// Determines Credentials
		String username = CommMessage.getValue(subscription.getParameters(), "credentials");
		System.out.println("Unsubscription [" + this.getContextType() + "]: " + username);
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		//System.out.println("Received Instruction: " + instruction.toString());
		
//		if (instruction.getCommand().equals("DEVICE_QUERY"))
//		{			
//			String deviceID = CommMessage.getValue(instruction.getParameters(), "DEVICE_ID");
//			String context  = CommMessage.getValue(instruction.getParameters(), "CONTEXT");
//			
//			if (context != null && deviceID != null)
//			{
//				JSONContextParser parser     = new JSONContextParser(JSONContextParser.JSON_TEXT, context);
//				boolean 		  shouldSend = this.sendAppData(context);
//				
//				if (shouldSend)
//				{					
//					getGroupContextManager().sendComputeInstruction("LOS_DNS", new String[] { "LOS_DNS" }, "SEND_ADVERTISEMENT", new String[] { "APP_ID=" + this.getAppID(), "DEVICE_ID=" + deviceID });
//				}
//			}
//			else
//			{
//				System.out.println("CONTEXT OR DEVICE ID IS NULL");
//			}
//		}
	}
	
	public ArrayList<String> getInformation()
	{		
		return super.getInformation();
	}
	
	// HELPER METHODS ---------------------------------------------------------------------------------
	/**
	 * Retrieves the Java Robot, which can press keys/move the mosue
	 * @return
	 */
	public Robot getRobot()
	{
		return robot;
	}
	
	/**
	 * Runs a Command Line Instruction (i.e. "ls -a")
	 * @param command
	 */
	protected void executeRuntimeCommand(String command)
	{
		try
		{
			System.out.print("Trying to execute: " + command + " -- ");
			
			Process p = null;
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			
			System.out.println("Done!");
        }
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Retrieves the Application Specific Folder Where Data Can be Stored
	 * @return
	 */
	public String getLocalStorageFolder()
	{
		File folder = new File(APP_DATA_FOLDER + this.getContextType() + "/");
		
		// Creates Folders if they do not yet exist.
		if (!folder.exists())
		{
			folder.mkdirs();
		}
		
		return APP_DATA_FOLDER + this.getContextType() + "/"; 
	}
	
	/**
	 * Creates a Resized Image
	 * @param originalImage
	 * @param type
	 * @param Width
	 * @param Height
	 * @return
	 */
	public static File resizeImage(String picturePath, String outputName, int Width,int Height)
	{
		File outputFile = null;
		
		try
		{
			BufferedImage originalImage = ImageIO.read(new File(picturePath));
			int 		  type 		    = BufferedImage.TYPE_INT_RGB;
			
			BufferedImage resizedImage = new BufferedImage(Width, Height, type);
		    Graphics2D g = resizedImage.createGraphics();
		    g.drawImage(originalImage, 0, 0, Width, Height, null);
		    g.dispose();

		    outputName = (outputName.endsWith(".jpeg")) ? outputName : outputName + ".jpeg";
			outputFile = new File(outputName);
			ImageIO.write(resizedImage, "jpeg", outputFile);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	    
	    return outputFile;
	}
	
	// JSON CONTEXT HELPER METHODS --------------------------------------------------------------------
	/**
	 * Retrieves the List of Devices from the JSON Context File
	 * @param parser
	 * @return
	 */
	protected ArrayList<String> getDevices(JSONContextParser parser)
	{
		ArrayList<String> result = new ArrayList<String>();
		
		JsonArray devices = parser.getJSONObject("device").get("devices").getAsJsonArray();
		
		for (int i=0; i<devices.size(); i++)
		{
			result.add(devices.get(i).getAsString());
		}
		
		return result;
	}
	
	/**
	 * Retrieves a List of Context Providers from the JSON Context File
	 * @param parser
	 * @return
	 */
	protected ArrayList<String> getContextProviders(JSONContextParser parser)
	{
		ArrayList<String> result = new ArrayList<String>();
		
		JsonArray devices = parser.getJSONObject("device").get("contextproviders").getAsJsonArray();
		
		for (int i=0; i<devices.size(); i++)
		{
			result.add(devices.get(i).getAsString());
		}
		
		return result;
	}

	/**
	 * Retrieves the Device Name Listed on this Context File
	 * @param parser
	 * @return
	 */
	protected String getDeviceName(JSONContextParser parser)
	{
		return parser.getJSONObject("device").get("deviceID").getAsString();
	}

	/**
	 * Calculates the Distance between the Device's Location and a Specified Point
	 * @param parser
	 * @return
	 */
	protected double getDistance(JSONContextParser parser, double startLatitude, double startLongitude)
	{
		JsonObject locationObject = parser.getJSONObject("location");
		
		if (locationObject.has("LATITUDE") && locationObject.has("LONGITUDE"))
		{
			double latitude  = parser.getJSONObject("location").get("LATITUDE").getAsDouble();
			double longitude = parser.getJSONObject("location").get("LONGITUDE").getAsDouble();
			
			return GeoMath.distance(startLatitude, startLongitude, latitude, longitude, 'K');
		}
		else
		{
			return Double.MAX_VALUE;
		}
	}
	
	// REPORTING THREAD -------------------------------------------------------------------------------
	private class DNSUpdateThread extends Thread
	{
		// Amount of Time to Report to the DNS
		public static final int DELAY_TIME = 60000;
		
		// Flag that Tells this Thread to Keep Running
		private boolean keepRunning;
		
		/**
		 * Periodically Sends a Message to the DNS Service
		 */
		public void run()
		{
			keepRunning = true;
			
			try
			{
				while (keepRunning)
				{
					System.out.println("Updating Live OS DNS:");
					//System.out.println(Arrays.toString(getInformation()) + "\n");
					
					sleep((int)(0.05 * DELAY_TIME));	
					getGroupContextManager().sendComputeInstruction("LOS_DNS", new String[] { "LOS_DNS" }, "REGISTER", getInformation().toArray(new String[0]));
					sleep((int)(0.95 * DELAY_TIME));
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
