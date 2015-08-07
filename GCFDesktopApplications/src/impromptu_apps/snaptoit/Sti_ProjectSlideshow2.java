package impromptu_apps.snaptoit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.desktop.toolkit.HttpToolkit;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class Sti_ProjectSlideshow2 extends SnapToItApplicationProvider
{

	public static final String APP_CONTEXT_TYPE = "STI_PROJECTS";
	public static final String APP_NAME			= "Project";
	public static final String APP_DESCRIPTION  = "Click here to get the paper associated with this slide.";
	public static final String APP_LOGO		    = "http://www.etc.cmu.edu/wp-content/uploads/2014/06/hcii.png";
	
	// Optional
	public static final double APP_SCREENSHOT_AZIMUTH = 0.0;
	public static final double APP_SCREENSHOT_PITCH   = 0.0;
	public static final double APP_SCREENSHOT_ROLL    = 0.0;
	
	// Application Components
	private Image  image = null;
	private JFrame frame;
	private JLabel lblimage;
	private JPanel mainPanel;
	
	private ProjectInfo 				 currentProject 		 = null;
	private ArrayList<ProjectInfo>  	 projects;
	private HashMap<String, ProjectInfo> userSelectedProjects = new HashMap<String, ProjectInfo>();
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param commMode The means by which this device communicates with other devices (TCP, MQTT)
	 * @param ipAddress The IP address
	 * @param port The network port
	 */
	public Sti_ProjectSlideshow2(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				APP_CONTEXT_TYPE,
				APP_NAME,
				APP_DESCRIPTION,
				"SNAP-TO-IT",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				APP_LOGO,				   // LOGO
				60,
				commMode,
				ipAddress,
				port);
		
		// Example #2:  Enable "Just In Time" Screenshots (With Azimuth, Pitch, and Roll)
		//this.enableRealtimeScreenshots(APP_SCREENSHOT_AZIMUTH, APP_SCREENSHOT_PITCH, APP_SCREENSHOT_ROLL);
		
		// Initializes the List of Projects
		projects = new ArrayList<ProjectInfo>();
		
		Gson gson = new Gson();
		String 	   json 	  = HttpToolkit.get("http://gcf.cmu-tbank.com/apps/hci_projects/config.txt");
		JsonParser jsonParser = new JsonParser();
		
		JsonElement rootElement  = jsonParser.parse(json);
		JsonArray   projectArray = rootElement.getAsJsonObject().get("projects").getAsJsonArray(); 
		
		System.out.println("Num Projects: " + projectArray.size());
		
		for (int i=0; i<projectArray.size(); i++)
		{
			ProjectInfo project = gson.fromJson(projectArray.get(i), ProjectInfo.class);
			System.out.println("  " + project.name);
			System.out.println("    " + project.image);
			projects.add(project);
		}
		
		// Initializes the Frame with a Starting Photo
		try 
		{
		    image = ImageIO.read(new URL("http://www.personal.psu.edu/acr117/blogs/audrey/images/image-2.jpg"));
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		// Use a label to display the image
		lblimage = new JLabel(new ImageIcon(image));
		frame    = new JFrame();
		frame.getContentPane().add(lblimage, BorderLayout.CENTER);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH); 
		frame.setUndecorated(true);
		frame.setVisible(true);	
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		
		
		mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(lblimage);
		frame.add(mainPanel);
		frame.setVisible(true);
		
		Thread t = new SlideshowThread();
		t.start();
	}
			
	/**
	 * This returns an Interface for a Specific User
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String website = userSelectedProjects.containsKey(subscription.getDeviceID()) ? userSelectedProjects.get(subscription.getDeviceID()).url : currentProject.url;
				
		return new String[] { "WEBSITE=" + website };
	}

	public double analyzePhoto(String deviceID, String photoURL, long timestamp, double azimuth, double pitch, double roll)
	{
		// Use the Parent Class's Analyze Method
		double result = super.analyzePhoto(deviceID, photoURL, timestamp, azimuth, pitch, roll);
		
		// Updates the Frame
		frame.setTitle("STI Result for " + deviceID + ": " + result);
		
		// Saves the Current Project in Memory
		userSelectedProjects.put(deviceID, currentProject);
		
		// Returns the Value
		return result;
		
//		String filename    	  = cloudPhotoPath.substring(cloudPhotoPath.lastIndexOf("/") + 1); 	// Just the Filename
//		String userPhotoPath = DesktopApplicationProvider.APP_DATA_FOLDER + USER_PHOTO_FOLDER + filename;
//		
//		// Takes the Screenshot
//		File screenshot = ScreenshotToolkit.takeScreenshot(640, 480, getLocalStorageFolder() + filename + currentScreenshot);
//		
//		HttpToolkit.downloadFile(cloudPhotoPath, userPhotoPath);
//		
//		System.out.println("Analyzing: " + screenshot.getAbsolutePath() + " vs " + userPhotoPath);
//		double matches = SnapToItApplicationProvider.openimaj.compareImages(screenshot.getAbsolutePath(), userPhotoPath);
//		SnapToItApplicationProvider.openimaj.forgetFeatures(screenshot.getAbsolutePath());
//		SnapToItApplicationProvider.openimaj.forgetFeatures(userPhotoPath);
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
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, userContextJSON);
		
		if (userSelectedProjects.containsKey(this.getDeviceID(parser)))
		{
			return userSelectedProjects.get(this.getDeviceID(parser)).name;
		}
		
		return currentProject.name;
	}
	
	/**
	 * OPTIONAL:  Allows you to Customize the Category of the App on a Per User Basis
	 */
	public String getCategory(String userContextJSON)
	{
		return category;
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

	/**
	 * Contains Information About a Specific Product
	 * @author adefreit
	 */
	public class ProjectInfo
	{
		public String name;
		public String image;
		public String url;
	}
	
	/**
	 * An Auto Update Method for the Slideshow
	 * @author adefreit
	 *
	 */
	public class SlideshowThread extends Thread
	{
		int count = 0;
		
		public void run()
		{
			while (true && projects.size() > 0)
			{
				lblimage.removeAll();
				
				try 
				{
					mainPanel.removeAll();
					
					System.out.print("Updating Image (" + projects.get(count).name + "): ");
					image = ImageIO.read(new URL(projects.get(count).image));
					image = image.getScaledInstance(frame.getWidth(), frame.getHeight(), Image.SCALE_SMOOTH);
				    lblimage = new JLabel(new ImageIcon(image));
					
				    Image  qrCode  = ImageIO.read(new URL("http://www.sanzospecialties.com/images/product/QR_label_larg.jpg"));
				    qrCode = qrCode.getScaledInstance(frame.getWidth()/10, frame.getWidth()/10, Image.SCALE_SMOOTH);
				    JLabel qrLabel = new JLabel(new ImageIcon(qrCode));
				    
				    // This is the Text Asking for STI
					JLabel test = new JLabel();
					test.setText("For the Full Paper, Take A Picture of this Screen Using the CMU Impromptu App (Android Only)");
					test.setFont(new Font("Arial", Font.PLAIN, 24));
					test.setForeground(Color.BLACK);
					
					// Places the Components on the Screen
					JPanel mainPanel = new JPanel(new BorderLayout());
					mainPanel.setBackground(Color.ORANGE);
					mainPanel.add(test, BorderLayout.NORTH);
					mainPanel.add(qrLabel, BorderLayout.WEST);
					mainPanel.add(lblimage);
					
					// Places the Components in the Application's View
					frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
					
					
					mainPanel.revalidate();
					mainPanel.repaint();
				    
				    // Saves the Current Project
				    currentProject = projects.get(count);	
				    count = (count + 1) % projects.size();
				    
				    sleep(1000);
				    //frame.setTitle("Last Screenshot: " + new Date());
				    Sti_ProjectSlideshow2.this.takeScreenshot(APP_SCREENSHOT_AZIMUTH, APP_SCREENSHOT_PITCH, APP_SCREENSHOT_ROLL);
				    sleep(29000);
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
					
					try
					{
						sleep(5000);
					}
					catch (Exception e2)
					{
						e2.printStackTrace();
					}
				}
			}
		}
	}
}
