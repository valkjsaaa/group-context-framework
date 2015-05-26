package snap_to_it;

import java.awt.Robot;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import openimaj.OpenimajToolkit;
import toolkits.ScreenshotToolkit;

import com.adefreitas.desktopframework.toolkit.CloudStorageToolkit;
import com.adefreitas.desktopframework.toolkit.SftpToolkit;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;
import com.adefreitas.messages.ContextCapability;
import com.adefreitas.messages.ContextRequest;

public abstract class RemoteControlProvider extends ContextProvider
{
	private static boolean DEBUG = false;
	
	// Match Threshold
	private static final double MIN_MATCH_QUALITY = 25;
	
	// Context Configuration
	private static final String CONTEXT_TYPE = "SNAP_TO_IT";
	private static final String LOG_NAME     = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	// Storage Settings
	protected final static String APP_DATA_FOLDER = "appData/universalremote/";
	private   CloudStorageToolkit cloudToolkit;
	
	// Preferences Desired from Clients (if Available)
	private ArrayList<String> desiredPreferences;
	
	// Interfaces
	private HashMap<String, String> interfaces;
	
	// Java Robot
	private Robot robot;
	
	// OpenImaj (Image Processing / SIFT)
	public  OpenimajToolkit   		 	 openimaj;
	private ArrayList<String> 		 	 photos;
	private HashMap<String, CompareInfo> comparisonHistory;
	private String 						 mostRecentWindow = "";
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public RemoteControlProvider(GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
	
		try
		{
			desiredPreferences = new ArrayList<String>();
			photos   	       = new ArrayList<String>();
			openimaj 	       = new OpenimajToolkit();
			comparisonHistory  = new HashMap<String, CompareInfo>();
			System.out.println("Image Comparison Toolkit Ready");
			
			robot = new Robot();
			System.out.println("Java Robot Ready");
			
			initializeUserInterfaces();
			System.out.println("Static Interfaces Ready");
			
			cloudToolkit = new SftpToolkit();
			System.out.println("Cloud Storage Connected");
		}
		catch (Exception ex)
		{
			System.out.println("A problem occurred while creating the robot: " + ex.getMessage());
		}
	}

	@Override
	public void start() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started");
	}

	@Override
	public void stop() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
	}
	
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		// Sends the UI Immediately
		sendContext();
		
		// Determines Credentials
		String username = CommMessage.getValue(newSubscription.getParameters(), "credentials");
		System.out.println("Subscription: " + username);
	}
	
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		// Determines Credentials
		String username = CommMessage.getValue(subscription.getParameters(), "credentials");
		System.out.println("Unsubscription: " + username);
	}
	
	@Override
	public double getFitness(String[] parameters) 
	{
		// Extracts the Parameters from the Request Message
		String deviceID       = CommMessage.getValue(parameters, "deviceID"); // The Device Sending this Request
		String cloudPhotoPath = CommMessage.getValue(parameters, "photo");	  // Cloud Location
		
		if (cloudPhotoPath != null)
		{
			String filename    	  = cloudPhotoPath.substring(cloudPhotoPath.lastIndexOf("/") + 1); // Just the Filename
			String localPhotoPath = APP_DATA_FOLDER + this.getGroupContextManager().getDeviceID() + "-" + filename;									   // Local Location
			
			System.out.println("\nRequest to Process " + filename);
					
			// Performs Comparisons (as needed)
			if (deviceID != null && cloudPhotoPath != null && (cloudPhotoPath.endsWith("jpeg") || cloudPhotoPath.endsWith("jpg")))
			{
				// Looks at the File and Determines when this File was Last Modified
				Date lastModified = cloudToolkit.getLastModified(cloudPhotoPath);
				
				System.out.print("  " + filename + " modified: " + lastModified + " . . . ");
				
				// Determines if the Device's Photograph Needs to be Downloaded Again
				if (!comparisonHistory.containsKey(deviceID) || (lastModified != null && lastModified.getTime() > comparisonHistory.get(deviceID).getLastModifiedTime()))
				{				
					if (comparisonHistory.containsKey(deviceID))
					{
						System.out.println("  Downloading " + filename + " [" + new Date(comparisonHistory.get(deviceID).getLastModifiedTime()) + "]");
					}
					else
					{
						System.out.println("  Downloading " + filename + " [FIRST DOWNLOAD]");
					}
					
					cloudToolkit.downloadFile(cloudPhotoPath, localPhotoPath);
					
					// Recomputes the Features for this Photo from Scratch
					openimaj.forgetFeatures(localPhotoPath);
					openimaj.computeFeatures(localPhotoPath);
				
					// Erases Existing Photo Path
					comparisonHistory.remove(deviceID);
					comparisonHistory.put(deviceID, new CompareInfo(deviceID, lastModified.getTime()));
				}
				else
				{
					System.out.println("  Already Downloaded.");
				}
				
				// Returns Precalculated Value if One Exists
				if (comparisonHistory.containsKey(deviceID) && comparisonHistory.get(deviceID).containsResults())
				{
					System.out.println("  Using Precached Result for " + cloudPhotoPath);
				}
				else
				{	
					// Compares Device Photo to Pictures on Record
					for (String refPhoto : new ArrayList<String>(photos))
					{
						// Performs the Actual Comparison
						int matches = openimaj.compareImages(refPhoto, localPhotoPath);
						
						// Stores the Results
						comparisonHistory.get(deviceID).addComparisonResult(refPhoto, matches);
						
						// Displays the Results
						System.out.println("Match " + cloudPhotoPath + " vs " + refPhoto + ":  "+ matches);
					}
				}
			}
			
			// Makes Sure that there
			if (comparisonHistory.containsKey(deviceID))
			{
				double result = comparisonHistory.get(deviceID).getBestMatch();
				
				// This is a bad hack . . . but it's okay because it's a debug tool
				// This prevents the application from opening up multiple views to show the same image comparison
				String tmp = localPhotoPath + comparisonHistory.get(deviceID).getBestMatchFilename() + "-" + result;
				
				if (DEBUG && result > 0 && !mostRecentWindow.equals(tmp))
				{
					mostRecentWindow = tmp;
					openimaj.showResults(localPhotoPath, comparisonHistory.get(deviceID).getBestMatchFilename());
				}
				
				return result;
			}
			else
			{
				return 0.0;
			}
		}
//		else if (device != null)
//		{
//			if (this.getGroupContextManager().getDeviceID().equals(device))
//			{
//				return 1.0;
//			}
//			else
//			{
//				return 0.0;
//			}
//		}
		else
		{
			// Default:  No Quality
			return 0.0;
		}
	}
	
	public boolean sendCapability(ContextRequest request)
	{
		System.out.println("Examining: " + request);
		
		// Looks at the Traditional Critieria
		boolean result = super.sendCapability(request);
		
		// Determines if the Device is Being Asked for By Name 
//		String device = CommMessage.getValue(request.getParameters(), "device");
//		
//		if (device != null)
//		{
//			System.out.println("By Name Request Detected:  Looking for " + device);
//			return this.getGroupContextManager().getDeviceID().equals(device);
//		}
		
		return result && (getFitness(request.getPayload()) >= MIN_MATCH_QUALITY);
	}
	
	// METHODS TO OVERLOAD ----------------------------------------------------------------------------
	@Override
	public abstract void sendContext();
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
//		
//		if (instruction.getCommand().equals("KEYPRESS"))
//		{
//			String key = CommMessage.getValue(instruction.getParameters(), "keycode");
//			int keycode = -1;
//			
//			if (key.equals("up"))
//			{
//				keycode = KeyEvent.VK_UP;
//			}
//			else if (key.equals("down"))
//			{
//				keycode = KeyEvent.VK_DOWN;
//			}
//			else if (key.equals("left"))
//			{
//				keycode = KeyEvent.VK_LEFT;
//			}
//			else if (key.equals("right"))
//			{
//				keycode = KeyEvent.VK_RIGHT;
//			}
//			else if (key.equals("a"))
//			{
//				keycode = KeyEvent.VK_D;
//			}
//			else if (key.equals("b"))
//			{
//				keycode = KeyEvent.VK_S;
//			}
//			
//			System.out.println("Pressing " + keycode);
//			
//		    robot.delay(40);
//		    robot.keyPress(keycode);
//		    robot.delay(100);
//		    robot.keyRelease(keycode);
//		}
//		else if (instruction.getCommand().equals("UPLOAD"))
//		{
//			String filePath = CommMessage.getValue(instruction.getParameters(), "uploadPath");
//			System.out.println("I got notified of an upload at: " + filePath);
//			
//			String folder      = filePath.substring(0, filePath.lastIndexOf("/") + 1);
//			String filename    = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length());
//			String destination = APP_DATA_FOLDER + instruction.getSourceDeviceID().replace(" ", "") + "/" + filename;
//			
//			System.out.println("Folder: " + folder);
//			System.out.println("File:   " + filename);
//			System.out.println("Dest:   " + destination);
//			
//			// Tries to Download the File
//			dropboxToolkit.downloadFile(folder, filename, destination);
//			
//			File file = new File(destination);
//			
//			if (file.exists())
//			{
//				// Opens Using Default Program
//				runCommand("open " + file.getAbsolutePath());
//			}
//		}
//		else if (instruction.getCommand().equals("LIGHT_RGB"))
//		{
//			System.out.println("Fowarding Command: " + instruction.getCommand());
//			this.getGroupContextManager().sendComputeInstruction("HUE", instruction.getCommand(), instruction.getParameters());
//		}
	}
	
	protected void initializeUserInterfaces()
	{
		interfaces = new HashMap<String, String>();
		
//		interfaces.put("SAMPLE", 
//				"<html><body><h1>Adrian's Macbook Air</h1></body>" +
//					"<div><input value=\"upload test\" type=\"button\" onclick=\"device.uploadFile('UPLOAD', '/UniversalRemote/Server/', '.pptx');\"/></div>" +
//					"<div><input value=\"download file\" type=\"button\" onclick=\"device.downloadFile('/UniversalRemote/paintball.pptx');\"/></div>" +
//					"<div><input value=\"up\" type=\"button\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=up']);\"/></div>" +
//					"<div><input value=\"down\" type=\"button\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=down']);\"/></div>" +
//					"<div><input value=\"left\" type=\"button\" style=\"height:50px; width:50px\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=left']);\"/></div>" +
//					"<div><input value=\"right\" type=\"button\" style=\"height:50px; width:50px\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=right']);\"/></div>" +
//					"<div><input value=\"a\" type=\"button\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=a']);\"/></div>" +
//					"<div><input value=\"b\" type=\"button\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=b']);\"/></div>" +
//				"</html>");
		
//		interfaces.put("CONTROLLER", 
//				"<html>" + 
//				"<body>" +
//					"<table width=100%>" +
//					"<tr>" +
//						"<td></td>" +
//						"<td><input value=\"\" type=\"button\" style=\"height:50px; width:50px; background-color:gray;\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=up']);\"/></td>" + 
//						"<td></td>" + 
//						"<td></td>" +
//						"<td></td>" +
//						"<td></td>" + 
//						"<td></td>" +
//					"</tr>" +
//					"<tr>" +
//						"<td><input value=\"\" type=\"button\" style=\"height:50px; width:50px; background-color:gray;\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=left']);\"/></td>" + 
//						"<td></td>" +  
//						"<td><input value=\"\" type=\"button\" style=\"height:50px; width:50px; background-color:gray;\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=right']);\"/></td>" + 
//						"<td></td>" + 
//						"<td></td>" + 
//						"<td><input value=\"B\" type=\"button\" style=\"height:50px; width:50px; background-color:red;\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=b']);\"/></td>" + 
//						"<td><input value=\"A\" type=\"button\" style=\"height:50px; width:50px; background-color:red;\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=a']);\"/></td>" +  
//					"</tr>" +
//					"<tr>" +
//					"<td></td>" +
//					"<td><input value=\"\" type=\"button\" style=\"height:50px; width:50px; background-color:gray;\" onclick=\"device.sendComputeInstruction('KEYPRESS', ['keycode=down']);\"/></td>" + 
//					"<td></td>" + 
//                	"<td></td>" +
//                	"<td></td>" +
//                	"<td></td>" + 
//                	"<td></td>" +
//                	"</tr>" +
//					"</table>" +
//				"</body>" +
//				"</html>");
	}
		
	protected String getUserInterface(String deviceID)
	{	
		return "YOU SHOULD NEVER SEE THIS";
	}
	
	// HELPER METHODS ---------------------------------------------------------------------------------
	public void addPhoto(String photoPath)
	{
		System.out.print("Trying to add: " + photoPath + " . . . ");
		
		openimaj.computeFeatures(photoPath);
		
		if (!photos.contains(photoPath))
		{
			photos.add(photoPath);
			System.out.println("SUCCESS");
		}
		else
		{
			System.out.println("ALREADY ADDED");
		}
	}
	
	public ArrayList<String> getPhotos()
	{
		return new ArrayList<String>(photos);
	}
	
	public void addInterface(String interfaceName, String html)
	{
		interfaces.put(interfaceName, html);
	}
	
	public Robot getRobot()
	{
		return robot;
	}
	
	public String getInterface(String interfaceName)
	{
		if (interfaces.containsKey(interfaceName))
		{
			return interfaces.get(interfaceName);
		}
		
		return null;
	}
	
	public CloudStorageToolkit getCloudStorageToolkit()
	{
		return cloudToolkit;
	}
	
	public String getDiagnosticHTML()
	{
		// Creates the First Line of the Results
		String results = "<html><title>" + CONTEXT_TYPE + " DIAGNOSTICS</title>";
			
		for (String deviceID : comparisonHistory.keySet())
		{
			results += "<h2>" + deviceID + "</h2>";
			
			CompareInfo info = comparisonHistory.get(deviceID);
			
			for (String filename : info.getComparisonFilenames())
			{
				results += "<div>" + filename + ": " + info.getResult(filename) + "</div>";
			}
		}
		
		// Creates the Last Line
		results += "</body></html>";
		
		return results;
	}
	
	public void setUserPreference(String deviceID, String key, String value)
	{
		this.getGroupContextManager().sendComputeInstruction("PREF", new String[] { deviceID }, "SET_PREFERENCE", new String[] { key + "=" + value });
	}

	public void setDesiredPreference(String[] keys)
	{
		for (String key : keys)
		{
			if (!desiredPreferences.contains(key))
			{
				desiredPreferences.add(key);
			}
		}
	}
	
	/**
	 * Allows application to take screenshots of the main display
	 * @param refreshRate
	 * @param numScreenshots
	 */
	public void enableScreenshots(final int refreshRate, final int numScreenshots)
	{		
		Thread t = new Thread()
		{
			public void run()
			{
				String filename 		 = "screenshot_";
				int    currentScreenshot = 0;
				
				for (int i=0; i<numScreenshots; i++)
				{
					File file = new File(APP_DATA_FOLDER + filename + i);
					
					if (file.exists())
					{
						System.out.println("Deleting " + file.getPath());
						file.delete();
					}
				}
				
				try
				{
					while (true)
					{
						// Takes the Screenshot
						File screenshot = ScreenshotToolkit.takeScreenshot(640, 480, APP_DATA_FOLDER + filename + currentScreenshot);

						// Deletes any Previous Features for this Filename
						openimaj.forgetFeatures(screenshot.getAbsolutePath());
						
						// Computes New Features
						photos.remove(screenshot.getPath());
						addPhoto(screenshot.getPath());
						
						// Updates Screenshot
						currentScreenshot = (currentScreenshot + 1) % numScreenshots;
						
						// Sleeps
						sleep(refreshRate);
					}
				}
				catch (Exception ex)
				{
					System.out.println("Thread Error: " + ex.getMessage());
				}
			}
		};
		
		t.start();
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

	// HELPER CLASSES ----------------------------------------------------------------------------------
	public class CompareInfo
	{
		private String 				    deviceID;
		private long   				    lastModifiedTime;
		private HashMap<String, Double> results;
		
		public CompareInfo(String deviceID, long lastModifiedTime)
		{
			this.deviceID 		  = deviceID;
			this.lastModifiedTime = lastModifiedTime;
			this.results 		  = new HashMap<String, Double>();
		}
		
		public String getDeviceID()
		{
			return deviceID;
		}
		
		public long getLastModifiedTime()
		{
			return lastModifiedTime;
		}
	
		public boolean containsResults()
		{
			return results.size() > 0;
		}
		
		public void addComparisonResult(String filename, double numMatches)
		{
			results.put(filename, numMatches);
		}
		
		public String[] getComparisonFilenames()
		{
			return results.keySet().toArray(new String[0]);
		}
		
		public double getResult(String filename)
		{
			if (results.containsKey(filename))
			{
				return results.get(filename);
			}
			
			return 0.0;
		}
		
		public double getBestMatch()
		{
			double result = 0.0;
			
			for (Double value : results.values())
			{
				result = Math.max(result, value);
			}
			
			return result;
		}
	
		public String getBestMatchFilename()
		{
			String bestFilename = "";
			double bestMatch    = 0.0;
			
			for (String filename : results.keySet())
			{
				if (results.get(filename) > bestMatch)
				{
					bestFilename = filename;
					bestMatch    = results.get(filename);
				}
			}
			
			return bestFilename;
		}
	}
}
