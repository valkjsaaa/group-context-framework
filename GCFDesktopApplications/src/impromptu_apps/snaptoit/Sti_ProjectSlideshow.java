package impromptu_apps.snaptoit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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

public class Sti_ProjectSlideshow extends SnapToItApplicationProvider
{
	public static final String APP_CONTEXT_TYPE = "STI_PROJECTS";
	public static final String APP_NAME			= "Project";
	public static final String APP_DESCRIPTION  = "Click here to get the paper associated with this slide.";
	public static final String APP_LOGO		    = "http://www.etc.cmu.edu/wp-content/uploads/2014/06/hcii.png";
	
	// Optional
	public static final double APP_SCREENSHOT_AZIMUTH = 0.0;
	public static final double APP_SCREENSHOT_PITCH   = 0.0;
	public static final double APP_SCREENSHOT_ROLL    = 0.0;
	
	// Project Information
	private int 						 count = 0;
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
	public Sti_ProjectSlideshow(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
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
				
		// Saves the Current Project in Memory
		userSelectedProjects.put(deviceID, currentProject);
		
		// Returns the Value
		return result;
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
	
	public class LayeredPaneDemo extends JFrame implements MouseListener
	{
		JPanel presentationPanel;
		JPanel qrPanel;
		JPanel titlePanel;
		
		int qrMode = 0;
		
		  public LayeredPaneDemo() 
		  {
		    super("");
		    this.setExtendedState(MAXIMIZED_BOTH);
		    this.setUndecorated(true);
		    getContentPane().setBackground(new Color(244, 232, 152));
		    getLayeredPane().setOpaque(true);
		    addMouseListener(this);
		    this.pack();

		    presentationPanel = new JPanel();
		    presentationPanel.setBounds(0, 0, 500, 500);
		    presentationPanel.setBackground(Color.RED);
		    presentationPanel.setOpaque(true);
		    getLayeredPane().add(presentationPanel, new Integer(0), 0);
		    
		    qrPanel = new JPanel();
		    qrPanel.setBounds(0, 0, 300, 300);
		    qrPanel.setBackground(null);
		    qrPanel.setOpaque(true);
		    getLayeredPane().add(qrPanel, new Integer(1), 0);
		    
		    titlePanel = new JPanel();
		    titlePanel.setBounds(0, 0, 100, 100);
		    titlePanel.setBackground(Color.GREEN);
		    titlePanel.setOpaque(true);
		    getLayeredPane().add(titlePanel, new Integer(2), 0);
		    
		    WindowListener l = new WindowAdapter() {
		      public void windowClosing(WindowEvent e) {
		        System.exit(0);
		      }
		    };

		    addWindowListener(l);
		    setVisible(true);
		  }
		  
		  public void update(String imageURL)
		  {
			  try
			  {
				  // Update the Image
				  Image  image    = ImageIO.read(new URL(imageURL));
				  image  		  = image.getScaledInstance(this.getWidth(), this.getHeight(), Image.SCALE_SMOOTH);
				  JLabel lblimage = new JLabel(new ImageIcon(image));
				  
				  presentationPanel.removeAll();
				  presentationPanel.setBounds(0, 0, this.getWidth(), this.getHeight());
				  presentationPanel.setBackground(Color.BLACK);
				  presentationPanel.add(lblimage);
				  presentationPanel.revalidate();
				  presentationPanel.repaint();
				  
				  // Update the QR Code
				  Image  qrImage  = ImageIO.read(new URL("https://www.unitag.io/qreator/generate?crs=e2ZRfLkGhCcNX0uPU0VF3psv%252Bf3Ob5SH7gn%252BI7vD1ScFtLnmBa7IedK8uUREEatiH6vdUOU6Z69yiDf4U6Gf6KN7MLO9VWBF%252FFmXZgwV8C7HQyYOkFvwyqzsu8AVVphHfA3lGyf2NEoiFB5Ry%252BWvtH3lcrNHJIFcPzGHCTueZ69sKTBd0fxecCk7d9OyaHDI%252FMtoLG6kmYx5OCTWJVwJlBg%252BDrYIGB8tg%252BHU9PFFb62Itrv%252BQYdNN31hVYnXzVgDslEWWdBponqOvJ2JY2aQUOfl4hDXf9Sk3GYxeG3KsPm2TKdFvFliNHG0779gKrIKdgUKM8KRLMeV4%252BrBIQGogA%253D%253D&crd=mXKe51B3LcpJnGj6pepi6Tk4f%252BnZcgru0sIAt3NXY0LUisOQdnSwPoa32Ec%252BnTPHmz%252BB0Jo2oicbUf%252F6gB%252BpaQ%253D%253D"));
				  qrImage  		  = qrImage.getScaledInstance(this.getWidth()/10, this.getWidth()/10, Image.SCALE_SMOOTH);
				  JLabel lblQR    = new JLabel(new ImageIcon(qrImage));
				  
				  qrPanel.removeAll();
				  
				  if (qrMode == 0)
				  {
					  qrPanel.setBounds(0,0,0,0);  
				  }
				  else if (qrMode == 1)
				  {
					  qrPanel.setBounds(20, 50, qrImage.getWidth(null), qrImage.getHeight(null));
				  }
				  else if (qrMode == 2)
				  {
					  qrPanel.setBounds(this.getWidth() - qrImage.getWidth(null) - 20, 50, qrImage.getWidth(null), qrImage.getHeight(null));
				  }
				  else if (qrMode == 3)
				  {
					  qrPanel.setBounds(this.getWidth() - qrImage.getWidth(null) - 20, this.getHeight() - qrImage.getHeight(null) - 20, qrImage.getWidth(null), qrImage.getHeight(null));
				  }
				  else if (qrMode == 4)
				  {
					  qrPanel.setBounds(20, this.getHeight() - qrImage.getHeight(null) - 20, qrImage.getWidth(null), qrImage.getHeight(null));
				  }
				  
				  qrPanel.setBackground(null);
				  qrPanel.add(lblQR);
				  qrPanel.revalidate();
				  qrPanel.repaint();
				  
				  // Update the Title
				  JLabel lblTitle = new JLabel("Take A Picture of this Screen Using the CMU Impromptu App (Android Only) to get the Full Paper");
				  lblTitle.setFont(new Font("Arial", Font.PLAIN, 24));
				  lblTitle.setForeground(Color.WHITE);
				  
				  titlePanel.removeAll();
				  titlePanel.setBounds(0, 0, this.getWidth(), 35);
				  titlePanel.setBackground(new Color(1, 99, 196));
				  titlePanel.add(lblTitle);
				  titlePanel.revalidate();
				  titlePanel.repaint();
				  
				  // Saves the Current Project
				  currentProject = projects.get(count);	
			  }
			  catch (Exception ex)
			  {
				  
			  }
		  }

		@Override
		public void mouseClicked(MouseEvent e) {
			qrMode = (qrMode + 1) % 5;
			System.out.println("QR Mode: " + qrMode);
			update(currentProject.image);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
	}
	
	/**
	 * An Auto Update Method for the Slideshow
	 * @author adefreit
	 *
	 */
	public class SlideshowThread extends Thread
	{		
		public void run()
		{
			LayeredPaneDemo lp = new LayeredPaneDemo();
			
			while (projects.size() > 0)
			{
				try 
				{				    
				    lp.update(projects.get(count).image);
				    Sti_ProjectSlideshow.this.takeScreenshot(APP_SCREENSHOT_AZIMUTH, APP_SCREENSHOT_PITCH, APP_SCREENSHOT_ROLL);
				    sleep(29000);
				    count = (count + 1) % projects.size();
				} 
				catch (Exception e) 
				{
					try
					{
						System.err.println("Problem Rendering Slideshow: " + e.getMessage());
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
