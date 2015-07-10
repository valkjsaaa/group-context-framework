package impromptu_apps.snaptoit;

import impromptu_apps.DesktopApplicationProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

import com.adefreit.openimajtoolkit.OpenImajToolkit;
import com.adefreitas.desktopframework.toolkit.FileToolkit;
import com.adefreitas.desktopframework.toolkit.HttpToolkit;
import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.desktopframework.toolkit.ScreenshotToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.google.gson.JsonObject;

public abstract class SnapToItApplicationProvider extends DesktopApplicationProvider
{			
	public static final String USER_PHOTO_FOLDER = "stiUserPhotos/";
	
	// OpenImaj (Image Processing / SIFT)
	static private OpenImajToolkit   		  openimaj;
	static private HashMap<String, PhotoInfo> photos;
	private HashMap<String, CompareInfo> 	  comparisonHistory;
	
	// Behavior Flags
	private   boolean debugMode       = false;
	protected boolean storeUserPhotos = true;
	protected int	  maxUserPhotos   = 5;
	protected double  minMatches      = 15.0;
	
	boolean realtime 		  = false;
	String  filename 		  = getAppID() + "_";
	int     currentScreenshot = 0;
	int     maxScreenshots    = 0;
	
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
		super(groupContextManager, contextType, name, description, "SNAP-TO-IT", contextsRequired, preferencesToRequest, logoPath, lifetime, commMode, ipAddress, port);
		
		// Creates the Snap-To-It Image Comparison Objects
		photos   	      = (photos == null) ? new HashMap<String, PhotoInfo>() : photos;
		openimaj 	      = (openimaj == null) ? new OpenImajToolkit() : openimaj;
		comparisonHistory = (comparisonHistory == null) ? new HashMap<String, CompareInfo>() : comparisonHistory;
				
		// Creates a Default Logo if one does not already exist
		if (this.logoPath == null || this.logoPath.length() == 0)
		{
			this.logoPath = "http://icons.iconarchive.com/icons/double-j-design/origami-colored-pencil/256/blue-camera-icon.png";
		}
		
		// Generates the Local Storage Folder on Initialization
		this.getLocalStorageFolder();
	}
	
	/**
	 * GCF Method:  Occurs when a Device Subscribes to this Provider
	 */
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		// Retrieves the File Name
		String 			  json 	   	   = CommMessage.getValue(newSubscription.getParameters(), "context");
		JSONContextParser parser   	   = new JSONContextParser(JSONContextParser.JSON_TEXT, json);	
		JsonObject 		  snapToItJSON = parser.getJSONObject("snap-to-it");
		
		// Makes a Copy of the File to Use Later!
		if (snapToItJSON != null && storeUserPhotos)
		{
			try
			{
				if (snapToItJSON.has("PHOTO"))
				{
					String cloudPhotoPath = snapToItJSON.get("PHOTO").getAsString();

					String filename = cloudPhotoPath.substring(cloudPhotoPath.lastIndexOf("/") + 1); 	// Just the Filename
					File   photo    = new File(DesktopApplicationProvider.APP_DATA_FOLDER + USER_PHOTO_FOLDER + filename);
					File   newFile  = new File(this.getLocalStorageFolder() + "userPhoto_" + new Date().getTime() + ".jpeg");
					
					// Only Adds a File to the Library if it Doesn't Already Match a Lot with an Existing Photograph
					if (comparisonHistory.containsKey(newSubscription.getDeviceID()) && comparisonHistory.get(newSubscription.getDeviceID()).containsResults())
					{
						if (comparisonHistory.get(newSubscription.getDeviceID()).getBestMatch() < 30.0)
						{
							FileToolkit.copyFile(photo, newFile);
							this.addAppliancePhoto(newFile.getAbsolutePath(), System.currentTimeMillis(), false, true, false, this.getOrientation(parser, "AZIMUTH"), this.getOrientation(parser, "PITCH"), this.getOrientation(parser, "ROLL"));	
						}
					}
					
					// Ensures that we don't have too many files
					cleanUserPhotos();	
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * GCF Method:  Returns the Most Recent Comparison Result
	 */
	@Override
	public double getFitness(String[] parameters) 
	{
		// Extracts the Parameters from the Request Message
		String deviceID = CommMessage.getValue(parameters, "deviceID");
		
		// Returns the Most Recent Comparison Result
		if (comparisonHistory.containsKey(deviceID))
		{
			return comparisonHistory.get(deviceID).getBestMatch();
		}
		else
		{
			return 0.0;
		}
	}
	
	/**
	 * Impromptu Method:  Returns the Fitness
	 * @param json
	 * @return
	 */
	public double getFitness(String json)
	{
		JSONContextParser parser   = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		String 			  deviceID = this.getDeviceID(parser);
		
		JsonObject snapToItObject = parser.getJSONObject("snap-to-it");
		
		if (snapToItObject != null && snapToItObject.has("PHOTO") && snapToItObject.has("TIMESTAMP"))
		{
			String cloudPhotoPath = snapToItObject.get("PHOTO").getAsString();
			long   timestamp      = snapToItObject.get("TIMESTAMP").getAsLong();
			return processPhoto(deviceID, cloudPhotoPath, timestamp, this.getOrientation(parser, "AZIMUTH"), this.getOrientation(parser, "PITCH"), this.getOrientation(parser, "ROLL"));
		}
		else
		{
			return 0.0;	
		}
	}
	
	/**
	 * Impromptu Method:  Returns TRUE if the app should respond; FALSE otherwise
	 */
	@Override
	public boolean sendAppData(String json)
	{
		if (debugMode)
		{
			// In debug mode, the device ALWAYS responds to any photo
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
				String applianceName  = snapToItObject.get("APPLIANCE_NAME").getAsString();
				long   timestamp      = snapToItObject.get("TIMESTAMP").getAsLong();
				double azimuth 		  = this.getOrientation(parser, "AZIMUTH");
				double pitch 		  = this.getOrientation(parser, "PITCH");
				double roll           = this.getOrientation(parser, "ROLL");
				
				if (applianceName.equals(this.getContextType()))
				{
					System.out.println("    ONBOARDING " + cloudPhotoPath);
					
					// Downloads the File
					String localPhotoPath = this.getLocalStorageFolder() + this.getContextType() + "_" + System.currentTimeMillis() + ".jpeg";		// Local File Location
					HttpToolkit.downloadFile(cloudPhotoPath, localPhotoPath);
					this.addAppliancePhoto(localPhotoPath, timestamp, false, true, false, azimuth, pitch, roll);
				}
				else
				{
					double matches = processPhoto(deviceID, cloudPhotoPath, timestamp, azimuth, pitch, roll);
					return matches >= minMatches;	
				}
			}
			else
			{
				if (snapToItObject == null)
				{
					System.out.print("Missing Snap-To-It JSON Element in Context ");
				}
				else if (!snapToItObject.has("PHOTO"))
				{
					System.out.print("Missing Photo ");
				}
				else if (!snapToItObject.has("TIMESTAMP"))
				{
					System.out.print("Missing Timestamp ");
				}
			}

			return false;		
		}	
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
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, userContextJSON);
		
		String newDescription = description + "\nConfidence: ";
		
		if (comparisonHistory.containsKey(this.getDeviceID(parser)))
		{
			CompareInfo info = comparisonHistory.get(this.getDeviceID(parser));

			if (info.getBestMatch() >= 100)
			{
				newDescription += "Very High";
			}
			else if (info.getBestMatch() >= 50)
			{
				newDescription += "High";
			}
			else if (info.getBestMatch() >= 25)
			{
				newDescription += "Medium";
			}
			else if (info.getBestMatch() >= 15)
			{
				newDescription += "Low";
			}
			else
			{
				newDescription += "UNKNOWN";
			}
			
			newDescription += " (" + info.getBestMatch() + " matches)";
		}
		
		return newDescription;
	}
	
	/**
	 * OPTIONAL:  Allows you to Customize the Lifetime of an App on a Per User Basis
	 */
	public int getLifetime(String userContextJSON)
	{
		return this.lifetime;
	}

	/**
	 * This performs the SIFT comparison
	 * @param deviceID
	 * @param cloudPhotoPath
	 * @return
	 */
	private double processPhoto(String deviceID, String cloudPhotoPath, long timestamp, double azimuth, double pitch, double roll)
	{
		Date startTime 		    = new Date();
		long featureComputeTime = 0;
		long totalCompareTime   = 0;
		long totalDownloadTime  = 0;
		ArrayList<PhotoInfo> photosToCompare = new ArrayList<PhotoInfo>(); 
		
		if (cloudPhotoPath != null)
		{
			System.out.println("\n    Request to Process " + cloudPhotoPath + " [" + timestamp + "]");
			
			// Extracts the Path for the User's Photo
			//String localPhotoPath = this.getLocalStorageFolder() + filename;						// Local Location
			String filename    	  = cloudPhotoPath.substring(cloudPhotoPath.lastIndexOf("/") + 1); 	// Just the Filename
			String localPhotoPath = DesktopApplicationProvider.APP_DATA_FOLDER + USER_PHOTO_FOLDER + filename;	
			
			// Performs Comparisons (as needed)
			if (deviceID != null && cloudPhotoPath != null && (cloudPhotoPath.endsWith("jpeg") || cloudPhotoPath.endsWith("jpg")))
			{				
				// STEP 1:  Determines if the Device's Photograph Needs to be Downloaded Again
				if (!photos.containsKey(localPhotoPath) || timestamp > photos.get(localPhotoPath).getTimestamp())
				{	
					System.out.println("    Downloading " + filename);
					
					// Downloads the Photo (Overwrites the Existing One)
					Date startDownload = new Date();
					HttpToolkit.downloadFile(cloudPhotoPath, localPhotoPath);
					totalDownloadTime = new Date().getTime() - startDownload.getTime();
										
					// Recomputes the Features for this Photo from Scratch
					Date startFeatureCompute = new Date();
					openimaj.forgetFeatures(localPhotoPath);
					openimaj.computeFeatures(localPhotoPath);
					featureComputeTime = new Date().getTime() - startFeatureCompute.getTime();
					
					photos.put(localPhotoPath, new PhotoInfo(localPhotoPath, timestamp, deviceID, true, false, false, azimuth, pitch, roll));
					
					// Takes a Screenshot and Extracts the SIFT Features
					if (realtime)
					{
						takeScreenshot();
					}
				}
				else
				{
					System.out.println("    " + filename + " Already Downloaded.  Using Existing Copy.");
				}
				
				// STEP 2:  Erases the Comparison History if we Downloaded a new Photo
				if (!comparisonHistory.containsKey(deviceID) || timestamp > comparisonHistory.get(deviceID).getLastModifiedTime())
				{	
					// Creates a New Empty Comparison Entry for this Photo
					comparisonHistory.put(deviceID, new CompareInfo(deviceID, timestamp));
				}
				
				// STEP 3:  Perform Comparisons (or Use Preexisting Cache)
				if (comparisonHistory.containsKey(deviceID) && comparisonHistory.get(deviceID).containsResults())
				{
					System.out.println("    Using Precached Result for " + cloudPhotoPath);
				}
				else
				{	
					System.out.println("    Looking at Photo Library for Candidates (" + photos.size() + " photographs total)");
					
					// Looks for Likely Photo Candidates
					for (PhotoInfo photo : photos.values())
					{
						if (photo.isAppliance && photo.getContextType().equals(this.getContextType()))
						{							
							if (photo.isCloseEnough(azimuth, pitch, roll))
							{
								System.out.println("*");		
								photosToCompare.add(photo);	
							}
							else
							{
								System.out.println();
							}
						}
					}
						
					// Compares Device Photo to Pictures on Record
					System.out.println("    Comparing user photo to " + photosToCompare.size() + " library images.");
					for (PhotoInfo photo : photosToCompare)
					{
						// Performs the Actual Comparison
						Date startCompare = new Date();
						int matches = openimaj.compareImages(photo.getPhotoPath(), localPhotoPath);
						totalCompareTime += new Date().getTime() - startCompare.getTime();
						
						// Stores the Results
						comparisonHistory.get(deviceID).addComparisonResult(photo.getPhotoPath(), matches);
						
						// Displays the Results
						System.out.println("     Match " + cloudPhotoPath + " vs " + photo.getPhotoPath() + ":  "+ matches);
					}
				}
			}
			
			// DEBUG:  Reports Timing Data
			System.out.println("    ----- ANALYSIS COMPLETE [" + this.getContextType() + "] -----");
			System.out.println("    Total Time Elapsed:   " + (new Date().getTime() - startTime.getTime()) + "ms");
			System.out.println("    Image Download Time:  " + (totalDownloadTime) + "ms");
			System.out.println("    Feature Compute Time: " + (featureComputeTime) + "ms");
			System.out.println("    Avg Comparison Time:  " + ((double)totalCompareTime / (double)Math.max(1, photosToCompare.size())) + "ms");
			System.out.println("    -----------------------------");
			
			// Returns the Results of the Comparison
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
	
	/**
	 * Extracts the Camera Orientation from the User's Context File
	 * @param parser
	 * @param type
	 * @return
	 */
	private double getOrientation(JSONContextParser parser, String type)
	{
		if (parser.getJSONObject("snap-to-it").has(type))
		{
			return parser.getJSONObject("snap-to-it").get(type).getAsDouble();
		}
		
		return 0.0;
	}
	
	// HELPER METHODS ---------------------------------------------------------------------------------
	public void addAppliancePhotoFromURL(String[] urls)
	{
		ArrayList<String> metadataFiles    = new ArrayList<String>();
		ArrayList<String> photosDownloaded = new ArrayList<String>();
		
		// Step 1:  Get Photos
		for (String url : urls)
		{
			String filename    	  = url.substring(url.lastIndexOf("/") + 1); 	// Just the Filename
			String folder    	  = url.substring(0, url.lastIndexOf("/") + 1); // Just the Folder
			String metadataFile   = folder + "metadata.txt";
			String localPhotoPath = this.getLocalStorageFolder() + filename;	// Local Location
			
			// Looks for the Metadata folders
			if (!metadataFiles.contains(metadataFile))
			{
				metadataFiles.add(metadataFile);
			}
			
			// Downloads the File
			HttpToolkit.downloadFile(url, localPhotoPath);
			
			if (!photosDownloaded.contains(filename))
			{
				photosDownloaded.add(filename);	
			}
		}
		
		// Step 2:  Get Metadata
		for (String metadataFileURL : metadataFiles)
		{
			String  metadata = HttpToolkit.get(metadataFileURL);
			Scanner s 		 = new Scanner(metadata);
			
			while (s.hasNext())
			{
				String   line  = s.nextLine();
				String[] entry = line.split(",");
				
				if (entry.length >= 4)
				{
					String name    = entry[0];
					double azimuth = Double.parseDouble(entry[1]);
					double roll    = Double.parseDouble(entry[2]);
					double pitch   = Double.parseDouble(entry[3]); 
					
					if (photosDownloaded.contains(name))
					{
						addAppliancePhoto(this.getLocalStorageFolder() + name, System.currentTimeMillis(), false, true, false, azimuth, pitch, roll);
					}	
				}
			}
			
			s.close();
		}
	}
	
	/**
	 * Associates a Photograph with this Application Service
	 * @param photoPath
	 */
	public void addAppliancePhoto(String photoPath, long timestamp, boolean isUserPhoto, boolean isAppliance, boolean isScreenshot, double azimuth, double pitch, double roll)
	{
		System.out.printf("Adding %s [isAppliance=%s, screenshot=%s, azimuth=%1.1f, pitch=%1.1f, roll=%1.1f] . . . ", photoPath, isAppliance, isScreenshot, azimuth, pitch, roll);
		
		if (!photos.containsKey(photoPath))
		{
			photos.put(photoPath, new PhotoInfo(photoPath, timestamp, this.getContextType(), isUserPhoto, isAppliance, isScreenshot, azimuth, pitch, roll));
			openimaj.computeFeatures(photoPath);
			System.out.println("SUCCESS");
		}
		else
		{
			System.out.println("ALREADY ADDED");
		}
	}
	
	/**
	 * Deprecated:  Adds a Photo
	 * @param photoPath
	 */
	public void addPhoto(String photoPath)
	{
		addAppliancePhoto(photoPath, 0, true, false, false, 0.0, 0.0, 0.0);
	}
	
	/**
	 * Disassociates a Photograph with this Application Service
	 * @param photoPath
	 */
	public void removePhoto(String photoPath)
	{
		System.out.print("Removing Photograph: " + photoPath + " . . . ");
				
		if (photos.containsKey(photoPath))
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
	
	/** Allows application to take screenshots of the main display at periodic Intervals
	 * @param refreshRate
	 * @param numScreenshots
	 */
	public void enableScreenshots(final int refreshRate, final int numScreenshots)
	{		
		// Don't Use User Photos for Realtime Stuff!
		storeUserPhotos = false;
		
		maxScreenshots = numScreenshots;
		
		Thread t = new Thread()
		{
			public void run()
			{				
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
						takeScreenshot();
						
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
	 * Allows application to take screenshots of the main display at the moment a request comes in
	 */
	public void enableRealtimeScreenshots()
	{
		realtime       = true;
		maxScreenshots = 1;
	}
	
	/**
	 * Takes a Screenshot and Calculates the SIFT Features
	 */
	private void takeScreenshot()
	{
		System.out.print("TAKING SCREENSHOT: ");
		
		// Takes the Screenshot
		File screenshot = ScreenshotToolkit.takeScreenshot(640, 480, getLocalStorageFolder() + filename + currentScreenshot);

		// Deletes any Previous Features for this Filename
		openimaj.forgetFeatures(screenshot.getAbsolutePath());
		
		// Computes New Features
		photos.remove(screenshot.getPath());
		addAppliancePhoto(screenshot.getPath(), System.currentTimeMillis(), false, true, true, 0.0, 0.0, 0.0);
		
		// Updates Screenshot
		currentScreenshot = (currentScreenshot + 1) % maxScreenshots;
	}
	
	/**
	 * Removes User Submitted Photos once a Predefined Threshhold is Reached
	 */
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
		
	// RESEARCH EXPERIMENT METHODS ---------------------------------------------------------------------
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
		for (String refPhoto : new ArrayList<String>(photos.keySet()))
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

	public class PhotoInfo
	{
		private String  photoPath;
		private long    timestamp;
		private String  contextType;
		private boolean isUserPhoto;
		private boolean isAppliance;
		private boolean isScreenshot;
		private double  azimuth;
		private double  pitch;
		private double  roll;
		
		public PhotoInfo(String photoPath, long timestamp, String contextType, boolean isUserPhoto, boolean isAppliance, boolean isScreenshot, double azimuth, double pitch, double roll)
		{
			this.photoPath   = photoPath;
			this.timestamp   = timestamp;
			this.contextType = contextType;
			this.isUserPhoto = isUserPhoto;
			this.isAppliance = isAppliance;
			this.azimuth     = normalizeAngle(azimuth);
			this.pitch       = normalizeAngle(pitch);
			this.roll        = normalizeAngle(roll);
		}

		public boolean isCloseEnough(double azimuth, double pitch, double roll)
		{			
			double difference = getAngleDifference(azimuth, pitch, roll);
						
			System.out.printf("      %s: [Appliance = %1.1f; User = %1.1f; Difference = %1.1f]  ", photoPath, this.azimuth, azimuth, difference);
			
			if (this.azimuth <0.1 && this.pitch <0.1 && this.roll <0.1)
			{
				return true;
			}
			else if (azimuth <0.1 && pitch < 0.1 && roll < 0.1)
			{
				return true;
			}
			else if (difference < 45.0)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		
		double getAngleDifference(double azimuth, double pitch, double roll)
		{
			azimuth = normalizeAngle(azimuth);
			roll    = normalizeAngle(roll);
			pitch   = normalizeAngle(pitch);
			
			return 180 - Math.abs(Math.abs(azimuth - this.azimuth) - 180);
		}
		
		double normalizeAngle(double angle)
		{
		    double newAngle = angle;
		    if (newAngle < 0) 
		    {
		    	newAngle += 360;
		    }
		    else if (newAngle > 360) 
		    {
		    	newAngle -= 360;
		    }
		    
//		    System.out.println(" Normalizing: " + angle + " -> " + newAngle);
		    
		    return newAngle;
		}
		
		public String getPhotoPath() {
			return photoPath;
		}

		public String getContextType()
		{
			return contextType;
		}
		
		public long getTimestamp()
		{
			return timestamp;
		}
		
		public boolean isUserPhoto()
		{
			return isUserPhoto;
		}
		
		public boolean isAppliance() {
			return isAppliance;
		}

		public boolean isScreenshot()
		{
			return isScreenshot;
		}
		
		public double getAzimuth() {
			return azimuth;
		}

		public double getPitch() {
			return pitch;
		}

		public double getRoll() {
			return roll;
		}
	}
}
