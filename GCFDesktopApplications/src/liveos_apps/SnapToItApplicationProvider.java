package liveos_apps;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.codehaus.plexus.util.FileUtils;

import openimaj.OpenimajToolkit;
import snap_to_it.RemoteControlProvider.CompareInfo;
import toolkits.ScreenshotToolkit;

import bluetoothcontext.toolkit.JSONContextParser;

import com.adefreitas.desktoptoolkits.CloudStorageToolkit;
import com.adefreitas.desktoptoolkits.SftpToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.google.gson.JsonObject;

public abstract class SnapToItApplicationProvider extends DesktopApplicationProvider
{			
	// OpenImaj (Image Processing / SIFT)
	public  OpenimajToolkit   		 	 openimaj;
	private ArrayList<String> 		 	 photos;
	private HashMap<String, CompareInfo> comparisonHistory;
	
	// File Downloading
	private CloudStorageToolkit          cloudToolkit;
	
	// Behavior Flags
	private boolean storeUserPhotos;
	private int		maxPhotos = 5;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public SnapToItApplicationProvider(GroupContextManager groupContextManager, 
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
		
		// Creates the Snap-To-It Storage Objects
		photos   	      = new ArrayList<String>();
		openimaj 	      = new OpenimajToolkit();
		comparisonHistory = new HashMap<String, CompareInfo>();
		cloudToolkit      = new SftpToolkit();
		
		// Store User Photos
		this.storeUserPhotos = true;
	}
	
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		// Retrieves the File Name
		String 			  json 	   		 = CommMessage.getValue(newSubscription.getParameters(), "context");
		JSONContextParser parser   		 = new JSONContextParser(JSONContextParser.JSON_TEXT, json);	
		JsonObject 		  snapToItObject = parser.getJSONObject("snap-to-it");
		
		// Makes a Copy of the File to Use Later!
		if (snapToItObject != null && storeUserPhotos)
		{
			try
			{
				String cloudPhotoPath = snapToItObject.get("PHOTO").getAsString();

				String filename = cloudPhotoPath.substring(cloudPhotoPath.lastIndexOf("/") + 1); 	// Just the Filename
				File   photo    = new File(this.getLocalStorageFolder() + filename);
				File   newFile  = new File(this.getLocalStorageFolder() + "userPhoto_" + new Date().getTime() + ".jpeg");
				
				FileUtils.copyFile(photo, newFile);
				
				this.addPhoto(newFile.getAbsolutePath());	
				
				// Ensures that we don't have too many files
				cleanUserPhotos();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
		
	@Override
	public double getFitness(String[] parameters) 
	{
		// Extracts the Parameters from the Request Message
		String deviceID       = CommMessage.getValue(parameters, "deviceID"); // The Device Sending this Request
		String cloudPhotoPath = CommMessage.getValue(parameters, "photo");	  // Cloud Location
		
		return processPhoto(deviceID, cloudPhotoPath);
	}
	
	public double getFitness(String json)
	{
		JSONContextParser parser   = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		String 			  deviceID = this.getDeviceName(parser);
		
		JsonObject snapToItObject = parser.getJSONObject("snap-to-it");
		
		if (snapToItObject != null)
		{
			String cloudPhotoPath = snapToItObject.get("PHOTO").getAsString();
			return processPhoto(deviceID, cloudPhotoPath);
		}
		else
		{
			return 0.0;	
		}
	}
	
	@Override
	public boolean sendAppData(String json)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		
		String deviceID		  = this.getDeviceName(parser);
		
		JsonObject snapToItObject = parser.getJSONObject("snap-to-it");
		
		if (snapToItObject != null)
		{
			String cloudPhotoPath = snapToItObject.get("PHOTO").getAsString();
			double matches = processPhoto(deviceID, cloudPhotoPath);
			System.out.println("Matches = " + matches);
			
			return matches > 10.0;
		}
		
		return false;		
	}
	
	private double processPhoto(String deviceID, String cloudPhotoPath)
	{
		if (cloudPhotoPath != null)
		{
			String filename    	  = cloudPhotoPath.substring(cloudPhotoPath.lastIndexOf("/") + 1); 	// Just the Filename
			String localPhotoPath = this.getLocalStorageFolder() + filename;						// Local Location
			
			System.out.println("\nRequest to Process " + filename);
					
			// Performs Comparisons (as needed)
			if (deviceID != null && cloudPhotoPath != null && (cloudPhotoPath.endsWith("jpeg") || cloudPhotoPath.endsWith("jpg")))
			{
				// Looks at the File and Determines when this File was Last Modified
				Date lastModified = cloudToolkit.getLastModified(cloudPhotoPath);
				
				// Determines if the Device's Photograph Needs to be Downloaded Again
				if (!comparisonHistory.containsKey(deviceID) || (lastModified != null && lastModified.getTime() > comparisonHistory.get(deviceID).getLastModifiedTime()))
				{									
					cloudToolkit.downloadFile(cloudPhotoPath, localPhotoPath);
					
					// Recomputes the Features for this Photo from Scratch
					openimaj.forgetFeatures(localPhotoPath);
					openimaj.computeFeatures(localPhotoPath);
				
					// Erases Existing Photo Path
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
					System.out.println("  Comparing Image Against " + photos.size() + " Photographs.");
					
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
				return comparisonHistory.get(deviceID).getBestMatch();
			}
			else
			{
				return 0.0;
			}
		}
		else
		{
			// Default:  No Quality
			return 0.0;
		}
	}
	
	// HELPER METHODS ---------------------------------------------------------------------------------
	/**
	 * Associates a Photograph with this Application Service
	 * @param photoPath
	 */
	public void addPhoto(String photoPath)
	{
		System.out.print("Adding Photograph: " + photoPath + " . . . ");
		
		if (!photos.contains(photoPath))
		{
			photos.add(photoPath);
			openimaj.computeFeatures(photoPath);
			System.out.println("SUCCESS");
		}
		else
		{
			System.out.println("ALREADY ADDED");
		}
	}
	
	/**
	 * Disassociates a Photograph with this Application Service
	 * @param photoPath
	 */
	public void removePhoto(String photoPath)
	{
		System.out.print("Removing Photograph: " + photoPath + " . . . ");
				
		if (photos.contains(photoPath))
		{
			photos.remove(photoPath);
			openimaj.forgetFeatures(photoPath);
			System.out.println("SUCCESS");
		}
		else
		{
			System.out.println("NOT FOUND");
		}
	}
	
	/** Allows application to take screenshots of the main display
	 * @param refreshRate
	 * @param numScreenshots
	 */
	public void enableScreenshots(final int refreshRate, final int numScreenshots)
	{		
		// Don't Use User Photos for Realtime Stuff!
		storeUserPhotos = false;
		
		Thread t = new Thread()
		{
			public void run()
			{
				String filename 		 = getAppID() + "_";
				int    currentScreenshot = 0;
				
				for (int i=0; i<numScreenshots; i++)
				{
					File file = new File(getLocalStorageFolder() + filename + i);
					
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
						File screenshot = ScreenshotToolkit.takeScreenshot(640, 480, getLocalStorageFolder() + filename + currentScreenshot);

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
	
	private void cleanUserPhotos()
	{
		File 			  folder    = new File(this.getLocalStorageFolder());
		ArrayList<String> filenames = new ArrayList<String>();
		
		for (String filename: folder.list())
		{
			if (filename.startsWith("userPhoto"))
			{
				filenames.add(this.getLocalStorageFolder() + filename);
				
				if (filenames.size() > maxPhotos)
				{
					// Grab the File
					File file = new File(filenames.get(0));
					
					// Remove it from the Database
					removePhoto(filenames.get(0));
					
					// Remove it from our temporary list
					filenames.remove(0);
					
					// Delete the file from the computer
					file.delete();
				}
			}
		}
	}
	
	// DEBUG METHODS -----------------------------------------------------------------------------------
	public String getDebugDescription()
	{
		String result = "";
		
		for (String deviceID : comparisonHistory.keySet())
		{
			result += comparisonHistory.get(deviceID).getBestMatchFilename() + " [" + comparisonHistory.get(deviceID).getBestMatch() + "]\n"; 
		}
		
		return result;
	}
	
	public void test(String filename)
	{
		System.out.println("\n\n*** Analyzing " + filename + " ***");
		
		HashMap<String, Integer> history = new HashMap<String, Integer>();
		
		// Compares Device Photo to Pictures on Record
		for (String refPhoto : new ArrayList<String>(photos))
		{
			// Performs the Actual Comparison
			int matches = openimaj.compareImages(refPhoto, filename);
			
			// Stores the Results
			history.put(refPhoto, matches);
		}
		
		while (history.size() > 0)
		{
			int    maxMatches = -10000;
			String maxFile    = "";
			
			for (String photo : history.keySet())
			{
				if (maxMatches < history.get(photo))
				{
					maxMatches = history.get(photo);
					maxFile    = photo;
				}
			}
			
			System.out.println(maxFile + ": " + maxMatches);			
			history.remove(maxFile);
		}
		
		System.out.println("*** Analysis Complete ***\n\n");
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
