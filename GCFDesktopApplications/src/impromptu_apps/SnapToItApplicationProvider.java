package impromptu_apps;


import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import openimaj.OpenimajToolkit;

import org.codehaus.plexus.util.FileUtils;

import toolkits.ScreenshotToolkit;

import com.adefreitas.desktopframework.toolkit.CloudStorageToolkit;
import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.desktopframework.toolkit.SftpToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.messages.CommMessage;
import com.google.gson.JsonObject;

public abstract class SnapToItApplicationProvider extends DesktopApplicationProvider
{			
	// OpenImaj (Image Processing / SIFT)
	public  OpenimajToolkit   		 	 openimaj;
	private ArrayList<String> 		 	 photos;
	private HashMap<String, CompareInfo> comparisonHistory;
	
	// File Downloading
	private CloudStorageToolkit cloudToolkit;
	
	// Behavior Flags
	private   boolean debugMode       = false;
	protected boolean storeUserPhotos = false;
	protected int	  maxUserPhotos   = 5;
	protected double  minMatches      = 10.0;
	
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
		
		// Generates the Local Storage Folder on Initialization
		this.getLocalStorageFolder();
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
		String deviceID       = CommMessage.getValue(parameters, "deviceID"); 					  // The Device Sending this Request
		String cloudPhotoPath = CommMessage.getValue(parameters, "photo");	  					  // Cloud Location
		//Long   timestamp      = Long.parseLong(CommMessage.getValue(parameters, "timestamp"));	  // Timestamp when photo was taken
		
		return processPhoto(deviceID, cloudPhotoPath, 0);
	}
	
	public double getFitness(String json)
	{
		JSONContextParser parser   = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		String 			  deviceID = this.getDeviceID(parser);
		
		JsonObject snapToItObject = parser.getJSONObject("snap-to-it");
		
		if (snapToItObject != null && snapToItObject.has("PHOTO") && snapToItObject.has("TIMESTAMP"))
		{
			String cloudPhotoPath = snapToItObject.get("PHOTO").getAsString();
			long   timestamp      = snapToItObject.get("TIMESTAMP").getAsLong();
			return processPhoto(deviceID, cloudPhotoPath, timestamp);
		}
		else
		{
			return 0.0;	
		}
	}
	
	@Override
	public boolean sendAppData(String json)
	{
		if (debugMode)
		{
			return true;
		}
		else
		{
			JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
			
			String deviceID = this.getDeviceID(parser);
			
			JsonObject snapToItObject = parser.getJSONObject("snap-to-it");
			
			if (snapToItObject != null && snapToItObject.has("PHOTO") && snapToItObject.has("TIMESTAMP"))
			{
				String cloudPhotoPath = snapToItObject.get("PHOTO").getAsString();
				long   timestamp      = snapToItObject.get("TIMESTAMP").getAsLong();
				double matches 		  = processPhoto(deviceID, cloudPhotoPath, timestamp);
				System.out.println("Matches = " + matches);
				return matches >= minMatches;
			}

			return false;		
		}	
	}
	
	/**
	 * This performs the SIFT comparison
	 * @param deviceID
	 * @param cloudPhotoPath
	 * @return
	 */
	private double processPhoto(String deviceID, String cloudPhotoPath, long timestamp)
	{
		Date startTime 		    = new Date();
		long featureComputeTime = 0;
		long totalCompareTime   = 0;
		long totalDownloadTime  = 0;
		
		if (cloudPhotoPath != null)
		{
			String filename    	  = cloudPhotoPath.substring(cloudPhotoPath.lastIndexOf("/") + 1); 	// Just the Filename
			String localPhotoPath = this.getLocalStorageFolder() + filename;						// Local Location
			
			System.out.println("\nRequest to Process " + filename);
					
			// Performs Comparisons (as needed)
			if (deviceID != null && cloudPhotoPath != null && (cloudPhotoPath.endsWith("jpeg") || cloudPhotoPath.endsWith("jpg")))
			{				
				// Determines if the Device's Photograph Needs to be Downloaded Again
				if (!comparisonHistory.containsKey(deviceID) || (timestamp > comparisonHistory.get(deviceID).getLastModifiedTime()))
				{								
					Date startDownload = new Date();
					cloudToolkit.downloadFile(cloudPhotoPath, localPhotoPath);
					totalDownloadTime = new Date().getTime() - startDownload.getTime();
					
					// Recomputes the Features for this Photo from Scratch
					openimaj.forgetFeatures(localPhotoPath);
					Date startFeatureCompute = new Date();					
					openimaj.computeFeatures(localPhotoPath);
					featureComputeTime = new Date().getTime() - startFeatureCompute.getTime();
					
					// Erases Existing Photo Path
					comparisonHistory.put(deviceID, new CompareInfo(deviceID, timestamp));
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
						Date startCompare = new Date();
						int matches = openimaj.compareImages(refPhoto, localPhotoPath);
						totalCompareTime += new Date().getTime() - startCompare.getTime();
						
						// Stores the Results
						comparisonHistory.get(deviceID).addComparisonResult(refPhoto, matches);
						
						// Displays the Results
						System.out.println("Match " + cloudPhotoPath + " vs " + refPhoto + ":  "+ matches);
					}
				}
			}
			
			// DEBUG:  Reports Timing Data
			System.out.println("----- ANALYSIS COMPLETE -----");
			System.out.println("Total Time Elapsed:   " + (new Date().getTime() - startTime.getTime()) + "ms");
			System.out.println("Image Download Time:  " + (totalDownloadTime) + "ms");
			System.out.println("Feature Compute Time: " + (featureComputeTime) + "ms");
			System.out.println("Avg Comparison Time:  " + ((double)totalCompareTime / (double)photos.size()) + "ms");
			System.out.println("-----------------------------");
			
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
			
			// Generates an Automatic Icon
			if (photos.size() == 1 && (this.logoPath == null || this.logoPath.length() == 0))
			{
				System.out.println("GENERATING ICON");
		    	
				File logo = DesktopApplicationProvider.resizeImage(photoPath, this.getAppID() + "_logo.jpeg", 320, 240);
				cloudToolkit.uploadFile("/var/www/html/gcf/universalremote/magic/", logo);
				logo.delete();
				
				this.logoPath = "http://" + Settings.DEV_WEB_IP + "/gcf/universalremote/magic/" + logo.getName();
				System.out.println("  ICON: " + logoPath);
			}
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
				
				if (filenames.size() > maxUserPhotos)
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
		
	// RESEARCH METHODS -----------------------------------------------------------------------------------
	public String getDebugDescription()
	{
		String result = "";
		
		for (String deviceID : comparisonHistory.keySet())
		{
			result += comparisonHistory.get(deviceID).getTopMatches(5);
		}
		
		return result;
	}
	
	public String test(String filename)
	{
		String result = "";
		
		System.out.println("\n\n*** Analyzing " + filename + " ***");
		
		HashMap<Integer, Integer> history = new HashMap<Integer, Integer>();
		
		// Results
		boolean top1 = false;
		boolean top3 = false;
		boolean top5 = false;
		boolean top7 = false;
		boolean top9 = false;
		
		int correctAutoLaunch = 0;
		
		// Compares Device Photo to Pictures on Record
		for (String refPhoto : new ArrayList<String>(photos))
		{
			// Performs the Actual Comparison
			int matches = openimaj.compareImages(refPhoto, filename);
			
			// Stores the Results
			if (!history.containsKey(getPrinter(refPhoto)))
			{
				history.put(getPrinter(refPhoto), matches);
			}
			else if (history.get(getPrinter(refPhoto)) < matches)
			{
				history.put(getPrinter(refPhoto), matches);
			}
		}
		
		ArrayList<Double> values = new ArrayList<Double>();
		
		for (int i=0; i<9; i++)
		{
			int    maxMatches = -10000;
			int maxDevice    = -1;
			
			for (Integer photo : history.keySet())
			{
				if (maxMatches < history.get(photo))
				{
					maxMatches = history.get(photo);
					maxDevice    = photo;
				}
			}
			
			values.add((double)maxMatches);
			
			System.out.println(maxDevice + ": " + maxMatches);			
			history.remove(maxDevice);
			
			if (getPrinter(filename) == maxDevice)
			{
				if (i<=8)
				{
					top9 = true;
				}
				if (i<=6)
				{
					top7 = true;
				}
				if (i<=4)
				{
					top5 = true;
				}
				if (i<=2)
				{
					top3 = true;
				}
				if (i==0)
				{
					top1 = true;
				}
			}
		}
		
		result += (top1) ? "Y" : "N";
		result += (top3) ? "Y" : "N";
		result += (top5) ? "Y" : "N";
		result += (top7) ? "Y" : "N";
		result += (top9) ? "Y" : "N";
		
		double  sd    = standardDeviation(values.toArray(new Double[0]));
		double  mean  = mean(values.toArray(new Double[0]));
		boolean skew1 = values.get(0) > (mean + 1*sd);
		boolean skew2 = values.get(0) > (mean + 2*sd);
		boolean skew3 = values.get(0) > (mean + 3*sd);
		correctAutoLaunch += (skew1 && skew2 && values.get(0) > 20 && top1) ? 1 : 0; 
		
		System.out.println("*** Analysis Complete [" + result + "] *** sd:" + sd + " mean:" + mean + "; " + skew1 + "; " + skew2 + "; " + skew3 + "; " + correctAutoLaunch + "\n");
		
		return result;
	}
	
	public void setDebugMode(boolean mode)
	{		
		this.debugMode = mode;
		
		if (debugMode)
		{
			System.out.println("*** DEBUG MODE ENABLED ***");
		}
		else
		{
			System.out.println("*** DEBUG MODE DISABLED ***");
		}
	}
	
	public void viewComparison(String file1, String file2)
	{	
		File f1 = new File(file1);
		File f2 = new File(file2);
		
		if (f1.exists() && f2.exists())
		{	
			int matches = openimaj.compareImages(file1, file2);
			System.out.println("Comparing Results: " + file1 + " vs. " + file2 + ": " + matches);
			openimaj.showResults(file1, file2);
		}
		else
		{
			System.out.println("Invalid Files: " + file1 + " vs. " + file2);
		}
	}
	
	private int getPrinter(String filename)
	{
		String[] temp = filename.split("/");
		
		String printer = temp[temp.length-1].split("-")[0];
		
		return Integer.parseInt(printer);
	}
	
    public static double standardDeviation(Double[] values) {
        double mean = mean(values);
        double n = values.length;
        double dv = 0;
        for (double d : values) {
            double dm = d - mean;
            dv += dm * dm;
        }
        return Math.sqrt(dv / n);
    }
    
    public static strictfp double mean(Double[] values) {
        return sum(values) / values.length;
    }

    public static strictfp double sum(Double[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("The data array either is null or does not contain any data.");
        }
        else {
            double sum = 0;
            for (int i = 0; i < values.length; i++) {
                sum += values[i];
            }
            return sum;
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
	
		public String getTopMatches(int numMatches)
		{
			ArrayList<String> matches = new ArrayList<String>();
			String 			  result  = "";
			
			for (int i=0; i<Math.min(numMatches, results.size()); i++)
			{
				String bestFilename = "";
				double bestMatch    = 0.0;
				
				for (String filename : results.keySet())
				{
					if (results.get(filename) > bestMatch && !matches.contains(filename))
					{
						bestFilename = filename;
						bestMatch    = results.get(filename);
					}
				}
				
				if (bestFilename.length() > 0)
				{
					result += bestFilename + ": " + bestMatch + "\n";	
					matches.add(bestFilename);
				}
			}
			
			return result;
		}
	}
}
