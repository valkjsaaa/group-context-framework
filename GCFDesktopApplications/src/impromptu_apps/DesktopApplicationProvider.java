package impromptu_apps;

import impromptu_app_directory.GeoMath;

import java.awt.Graphics2D;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;


import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.hashlibrary.SHA1;
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
		sendContext();
		
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
	protected String getDeviceID(JSONContextParser parser)
	{
		return parser.getJSONObject("device").get("deviceID").getAsString();
	}

	protected double getLatitude(JSONContextParser parser)
	{
		try
		{
			return parser.getJSONObject("location").get("LATITUDE").getAsDouble();	
		}
		catch (Exception ex)
		{
			return 0.0;
		}
	}

	protected double getLongitude(JSONContextParser parser)
	{
		try
		{
			return parser.getJSONObject("location").get("LONGITUDE").getAsDouble();	
		}
		catch (Exception ex)
		{
			return 0.0;
		}
	}

	protected String getActivity(JSONContextParser parser)
	{
		String activity = "unspecified";
		
		try
		{
			activity = parser.getJSONObject("activity").get("type").getAsString();
		}
		catch (Exception ex)
		{
			// Do Nothing!
		}
		
		return activity;
	}
	
	protected int getConfidence(JSONContextParser parser)
	{
		int confidence = 0;
		
		try
		{
			confidence = parser.getJSONObject("activity").get("confidence").getAsInt();
		}
		catch (Exception ex)
		{
			// Do Nothing!
		}
		
		return confidence;
	}
	
	protected boolean signedDisclaimer(JSONContextParser parser)
	{
		JsonObject preferences = parser.getJSONObject("preferences");
		
		return preferences != null && preferences.get("disclaimer") != null;
	}
	
	/**
	 * Calculates the Distance between the Device's Location and a Specified Point
	 * @param parser
	 * @return
	 */
	protected double getDistance(JSONContextParser parser, double startLatitude, double startLongitude)
	{
		JsonObject locationObject = parser.getJSONObject("location");
		
		if (locationObject != null && locationObject.has("LATITUDE") && locationObject.has("LONGITUDE"))
		{
			double latitude  = getLatitude(parser);
			double longitude = getLongitude(parser);
			
			return GeoMath.distance(startLatitude, startLongitude, latitude, longitude, 'K');
		}
		else
		{
			return Double.MAX_VALUE;
		}
	}
	
	/**
	 * Determines if a particular email address is inside of this context file
	 * @param parser
	 * @param emailAddress
	 * @return
	 */
	protected boolean hasEmailAddress(JSONContextParser parser, String emailAddress)
	{
		if (parser.getJSONObject("identity") != null && parser.getJSONObject("identity").has("email"))
		{
			String 	  hash 			 = SHA1.getHash(emailAddress);
			JsonArray emailAddresses = parser.getJSONObject("identity").get("email").getAsJsonArray();
			
			for (int i=0; i<emailAddresses.size(); i++)
			{
				if (emailAddresses.get(i).getAsString().equals(hash))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Determines if ONE of the following email addresses is inside of this context file
	 */
	protected boolean hasEmailAddress(JSONContextParser parser, String[] emailAddresses)
	{
		if (parser.getJSONObject("identity") != null && parser.getJSONObject("identity").has("email"))
		{
			for (String emailAddress : emailAddresses)
			{
				String 	  hash 			     = SHA1.getHash(emailAddress);
				JsonArray userEmailAddresses = parser.getJSONObject("identity").get("email").getAsJsonArray();
				
				for (int i=0; i<userEmailAddresses.size(); i++)
				{
					if (userEmailAddresses.get(i).getAsString().equals(hash))
					{
						return true;
					}
				}	
			}
		}
		
		return false;
	}
	
	/**
	 * Determines if a particular email domain is inside of this context
	 * @param parser
	 * @param emailDomain
	 * @return
	 */
	protected boolean hasEmailDomain(JSONContextParser parser, String emailDomain)
	{
		JsonArray emailDomains = parser.getJSONObject("identity").get("emailDomains").getAsJsonArray();
		
		for (int i=0; i<emailDomains.size(); i++)
		{
			if (emailDomains.get(i).getAsString().equalsIgnoreCase(emailDomain))
			{
				return true;
			}
		}
		
		return false;
	}
}
