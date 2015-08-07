package impromptu_apps;

import impromptu_apps.creationfest.*;
import impromptu_apps.desktop.*;
import impromptu_apps.favors.*;
import impromptu_apps.snaptoit.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.GregorianCalendar;


import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.CommThread;
import com.adefreitas.gcf.Settings;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.DesktopBatteryMonitor;
import com.adefreitas.gcf.desktop.DesktopGroupContextManager;
import com.adefreitas.gcf.desktop.EventReceiver;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.desktop.toolkit.SQLToolkit;
import com.adefreitas.gcf.impromptu.ApplicationSettings;
import com.adefreitas.gcf.messages.ContextData;
import com.google.gson.JsonObject;

public class Application implements EventReceiver
{
	// Creates a Unique Computer Name (Needed for GCM to Operate, But You Can Change it to Anything Unique)
	public String COMPUTER_NAME 		  = "";
	public int    UPDATE_THREAD_WAIT_TIME = 30000;
	
	// GCF Communication Settings (BROADCAST_MODE Assumes a Functional TCP Relay Running)
	public static final CommManager.CommMode COMM_MODE       = CommMode.MQTT;
	public static final String 				 IP_ADDRESS      = Settings.DEV_MQTT_IP;
	public static final int    				 PORT 	         = Settings.DEV_MQTT_PORT;
	
	// GCF Variables
	public DesktopBatteryMonitor      batteryMonitor;
	public DesktopGroupContextManager gcm;

	// List of All Application Providers Currently Loaded
	public ArrayList<DesktopApplicationProvider> appProviders = new ArrayList<DesktopApplicationProvider>();

	// Context Provider Representing the Application
	public QueryApplicationProvider queryProvider;
	
	// Dispatchers
	public UpdateThread    updateThread;
	public TaskDispatcher  t;
	public FavorDispatcher f;
	
	// SQL Toolkit
	public static final String SQL_SERVER   = "epiwork.hcii.cs.cmu.edu";
	public static final String SQL_USERNAME = "adrian";
	public static final String SQL_PASSWORD = "@dr1@n1234";
	public static final String SQL_DB		= "gcf_impromptu";
	public SQLToolkit sqlToolkit;
	
	/**
	 * Constructor
	 * @param appName
	 * @param useBluetooth
	 */
	public Application(String appName, boolean useBluetooth)
	{
		COMPUTER_NAME  = (appName.length() > 0) ? appName : "APP_" + (System.currentTimeMillis() % 1000);
		gcm 		   = new DesktopGroupContextManager(COMPUTER_NAME, false);
		
		// Opens the Connection
		String connectionKey = gcm.connect(COMM_MODE, IP_ADDRESS, PORT);
		gcm.subscribe(connectionKey, ApplicationSettings.DNS_APP_CHANNEL);
		
		// Manages Debugging
		gcm.setDebugMode(false);
		
		// Registers Objects to Handle Requests and Messages
		gcm.registerEventReceiver(this);
		
		// Creates the Query Manager
		queryProvider = new QueryApplicationProvider(gcm, connectionKey);
		gcm.registerContextProvider(queryProvider);

		// Initializes SQL Connection
		//sqlToolkit = new SQLToolkit("citrus-acid.com", "citrusa_michael", "mysql1234", "citrusa_michael");
		sqlToolkit = new SQLToolkit(SQL_SERVER, SQL_USERNAME, SQL_PASSWORD, SQL_DB);
		
		// Creates the Individual Apps that this Application will Host
		initializeApps();
		
		// Initializes Communications Channel
		for (DesktopApplicationProvider app : appProviders)
		{
			System.out.print("Loading " + app.getAppID() + " [" + app.getContextType() + "] . . . ");
			app.setSQLEventLogger(sqlToolkit);
			gcm.registerContextProvider(app);
			gcm.subscribe(connectionKey, app.getContextType());
			System.out.println("Done!");
		}
		
		// Removes Public Channel (No Reason to Listen to It)
		//gcm.unsubscribe(connectionKey, CommThread.PUBLIC_CHANNEL);
		
		// And We're Done!
		System.out.println(appProviders.size() + " App(s) Initialized!\n");
	}

	/**
	 * This Creates all of the Apps that Impromptu Users will See
	 */
	private void initializeApps()
	{
		// Impromptu
		//initializeImpromptuApps();
		
		// Snap-To-It
		initializeSnapToItApps();
		
		// Favor Banking
		//initializeFavorBank();
		
		// Google IOT
		//initializeSchedule();
		
		// This is Used by the Dispatchers!
		if (f != null || t != null)
		{
			updateThread = new UpdateThread();
			updateThread.start();
		}
	}
		
	private void initializeImpromptuApps()
	{		
		// Standard Apps (IMPROMPTU_CORE)
		appProviders.add(new App_Disclaimer(gcm, COMM_MODE, IP_ADDRESS, PORT));
		appProviders.add(new App_Feedback(gcm, sqlToolkit, COMM_MODE, IP_ADDRESS, PORT));
		appProviders.add(new App_BluewavePermissions(gcm, COMM_MODE, IP_ADDRESS, PORT));
		appProviders.add(new App_BluewaveDebug(gcm, COMM_MODE, IP_ADDRESS, PORT));
		appProviders.add(new App_Troubleshooting(gcm, COMM_MODE, IP_ADDRESS, PORT));
		
		// Convenience Apps
		appProviders.add(new App_Bus(gcm, COMM_MODE, IP_ADDRESS, PORT));
		appProviders.add(new App_Starbucks(gcm, COMM_MODE, IP_ADDRESS, PORT));
		appProviders.add(new App_CMU(gcm, COMM_MODE, IP_ADDRESS, PORT));
		appProviders.add(new App_Target(gcm, COMM_MODE, IP_ADDRESS, PORT));
		appProviders.add(new App_BestBuy(gcm, COMM_MODE, IP_ADDRESS, PORT));
		appProviders.add(new App_Porch(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_Weather(gcm, COMM_MODE, IP_ADDRESS, PORT));
		
//		// CMU Map Application
//		App_LocationWebsite outletApp = new App_LocationWebsite(gcm, 
//				"Grove City Outlet App", 
//				"http://www.premiumoutlets.com/outlets/store_listing.asp?id=85", 
//				"Store Listing for the Outlet Mall.", 
//				"SHOPPING", 
//				"https://cdn4.iconfinder.com/data/icons/whsr-january-flaticon-set/512/shopping_bag.png", 
//				1.0, 
//				COMM_MODE, IP_ADDRESS, PORT);
//		outletApp.addLocation("Outlet Center", 41.140840, -80.157141);
//		appProviders.add(outletApp);
	}
	
	private void initializeSnapToItApps()
	{		
		appProviders.add(new Sti_ProjectSlideshow(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new Sti_Map(gcm, COMM_MODE, IP_ADDRESS, PORT));
		
		// LIST CONDITION:  BE SURE TO ACTIVATE STI_DIGITALPROJECTOR
//		appProviders.add(new Sti_Dummy("Digital Projector (MITSUBISHI)", "Lets you upload PowerPoint presentations and control them on this digital projector.", "Devices", "http://png-4.findicons.com/files/icons/2711/free_icons_for_windows8_metro/128/video_projector.png", gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_Dummy("Digital Projector (EIKI 2)", "Lets you upload PowerPoint presentations and control them on this digital projector.", "Devices", "http://png-4.findicons.com/files/icons/2711/free_icons_for_windows8_metro/128/video_projector.png", gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_Dummy("Printer Controls (Ricoh)", "Controls the Printer Ricoh.  Powered by Snap-To-It!", "Devices", "http://icons.iconarchive.com/icons/iconshock/real-vista-computer-gadgets/256/multifunction-printer-icon.png", gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_Dummy("Printer Controls (HP Digital Sender 9020n)", "Controls the Printer HP Digital Sender 9020n.  Powered by Snap-To-It!", "Devices", "http://icons.iconarchive.com/icons/iconshock/real-vista-computer-gadgets/256/multifunction-printer-icon.png", gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_Dummy("Printer Controls (LaserJet)", "Controls the Printer LaserJet.  Powered by Snap-To-It!", "Devices", "http://icons.iconarchive.com/icons/iconshock/real-vista-computer-gadgets/256/multifunction-printer-icon.png", gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_Dummy("Macbook", "Lets you control the application on this computer.", "Devices", "http://www.gedtestingservice.com/uploads/images/medium/0a54c4f41f9bb1a1b74fe0cdaedbc0a7.jpeg", gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_Dummy("Dell Inspiron", "Lets you control the application on this computer.", "Devices", "http://www.gedtestingservice.com/uploads/images/medium/0a54c4f41f9bb1a1b74fe0cdaedbc0a7.jpeg", gcm, COMM_MODE, IP_ADDRESS, PORT));
//		Sti_DigitalProjector stip = new Sti_DigitalProjector(gcm, COMM_MODE, IP_ADDRESS, PORT);
//		Sti_Game             stig = new Sti_Game(gcm, COMM_MODE, IP_ADDRESS, PORT);
//		stip.listMode = true;
//		stig.listMode = true;
//		appProviders.add(stip);
//		appProviders.add(stig);
		
		// STI CONDITION (CODE && QR)
//		Sti_DigitalProjector stip = new Sti_DigitalProjector(gcm, COMM_MODE, IP_ADDRESS, PORT);
//		Sti_Game             stig = new Sti_Game(gcm, COMM_MODE, IP_ADDRESS, PORT);
//		stip.listMode = false;
//		stig.listMode = false;
//		appProviders.add(stip);
//		appProviders.add(stig);
		
//		appProviders.add(new Sti_Printer(gcm, "ZIRCON", "\\\\monolith.scs.ad.cs.cmu.edu\\zircon",
//				new String[] {
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/zircon/zircon_0.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/zircon/zircon_1.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/zircon/zircon_2.jpeg"
//				},
//			    COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_Printer(gcm, "PEWTER", "\\\\monolith.scs.ad.cs.cmu.edu\\pewter", 
//				new String[] {
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/pewter/pewter_0.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/pewter/pewter_1.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/pewter/pewter_2.jpeg"
//				},
//			    COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_Printer(gcm, "NSH Color 3508", "\\\\monolith.scs.ad.cs.cmu.edu\\NSH3508color",
//				new String[] {
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/nshcolor/nshcolor_0.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/nshcolor/nshcolor_1.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/nshcolor/nshcolor_2.jpeg"
//				},
//			    COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_Printer(gcm, "SAND", "\\\\monolith.scs.ad.cs.cmu.edu\\sand",
//				new String[] {
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/sand/sand_0.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/sand/sand_1.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/sand/sand_2.jpeg"
//				},
//			    COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_Printer(gcm, "PRISM COLOR", "\\\\monolith.scs.ad.cs.cmu.edu\\prismcolor",
//				new String[] {
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/prismcolor/prismcolor_0.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/prismcolor/prismcolor_1.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/prismcolor/prismcolor_2.jpeg"
//				},
//			    COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_Printer(gcm, "DEVLAB", "Brother HL-L2380DW series Printer",
//				new String[] {
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/brother/brother_0.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/brother/brother_1.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/brother/brother_2.jpeg"
//				},
//			    COMM_MODE, IP_ADDRESS, PORT));
		
		//appProviders.add(new Sti_GenericDevice(gcm, "object1", new String[0], COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_GenericDevice(gcm, "object2", new String[0], COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_GenericDevice(gcm, "object3", new String[0], COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_Game(gcm, COMM_MODE, IP_ADDRESS, PORT));
		
//		appProviders.add(new Sti_Printer(gcm, "HCI Copy Machine", 
//				new String[] {
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/hcicopy/hcicopy_0.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/hcicopy/hcicopy_1.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/hcicopy/hcicopy_2.jpeg"
//				},
//			    COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_GenericDevice(gcm, "Sign", 
//				new String[] { 
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/sign/sign_0.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/sign/sign_1.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/sign/sign_2.jpeg"}, 
//				COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_GenericDevice(gcm, "Coffee", 
//				new String[] { 
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/coffee/coffee_0.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/coffee/coffee_1.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/coffee/coffee_2.jpeg"},
//				COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_GenericDevice(gcm, "NSH 2nd Floor Copy", 
//				new String[] { 
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/nsh2copy/nsh2copy_0.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/nsh2copy/nsh2copy_1.jpeg",
//					"http://gcf.cmu-tbank.com/snaptoit/appliances/nsh2copy/nsh2copy_2.jpeg"},
//				COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new Sti_GenericDevice(gcm, "Cup", new String[0], COMM_MODE, IP_ADDRESS, PORT));
//				
//		appProviders.add(new Sti_GenericDevice(gcm, "Ubicomp Lab Sign", 
//		new String[] { 
//			"http://gcf.cmu-tbank.com/snaptoit/appliances/ubicomp/ubicomp_0.jpeg",
//			"http://gcf.cmu-tbank.com/snaptoit/appliances/ubicomp/ubicomp_1.jpeg",
//			"http://gcf.cmu-tbank.com/snaptoit/appliances/ubicomp/ubicomp_2.jpeg"}, 
//		COMM_MODE, IP_ADDRESS, PORT));
	}
	
	private void initializeFavorBank()
	{
		// Favors
		appProviders.add(new App_FavorListener(gcm, COMM_MODE, IP_ADDRESS, PORT, sqlToolkit));
		appProviders.add(new App_FavorRequester(gcm, COMM_MODE, IP_ADDRESS, PORT, sqlToolkit));
		appProviders.add(new App_FavorProfile(gcm, COMM_MODE, IP_ADDRESS, PORT, sqlToolkit));
		f = new FavorDispatcher(sqlToolkit, gcm);
	}
	
	private void initializeSchedule()
	{
		App_Schedule scheduleApp = new App_Schedule("IOT at CMU", gcm, COMM_MODE, IP_ADDRESS, PORT);
		scheduleApp.addEvent("Breakfast",      "Newell Simon Hall (NSH)", new GregorianCalendar(2015,6,27,8,30,00).getTime(), new GregorianCalendar(2015,6,27,9,00,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=1");
		scheduleApp.addEvent("Welcome", 			               "NSH", new GregorianCalendar(2015,6,27,9,00,00).getTime(), new GregorianCalendar(2015,6,27,9,15,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=2");
		scheduleApp.addEvent("Introductions and Agenda Overview",  "NSH", new GregorianCalendar(2015,6,27,9,15,00).getTime(), new GregorianCalendar(2015,6,27,9,30,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=3");
		scheduleApp.addEvent("Google Research", 				   "NSH", new GregorianCalendar(2015,6,27,9,30,00).getTime(), new GregorianCalendar(2015,6,27,9,40,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=4");
		scheduleApp.addEvent("Stack Overview", 					   "NSH", new GregorianCalendar(2015,6,27,9,40,00).getTime(), new GregorianCalendar(2015,6,27,10,10,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=5");
		scheduleApp.addEvent("Review Scenarios", 				   "NSH", new GregorianCalendar(2015,6,27,10,10,00).getTime(), new GregorianCalendar(2015,6,27,10,40,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=6");
		scheduleApp.addEvent("Coffee Break", 					   "NSH", new GregorianCalendar(2015,6,27,10,40,00).getTime(), new GregorianCalendar(2015,6,27,11,00,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=7");
		scheduleApp.addEvent("Scenario Review (Continued)", 	   "NSH", new GregorianCalendar(2015,6,27,11,00,00).getTime(), new GregorianCalendar(2015,6,27,12,00,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=8");
		scheduleApp.addEvent("Lunch", 							   "NSH", new GregorianCalendar(2015,6,27,12,00,00).getTime(), new GregorianCalendar(2015,6,27,13,15,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=9");
		scheduleApp.addEvent("CMU Infrastructure and Privacy",     "NSH", new GregorianCalendar(2015,6,27,13,15,00).getTime(), new GregorianCalendar(2015,6,27,13,30,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=10");
		scheduleApp.addEvent("Face Recognition as an IOT Service", "NSH", new GregorianCalendar(2015,6,27,13,30,00).getTime(), new GregorianCalendar(2015,6,27,13,45,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=11");
		scheduleApp.addEvent("UIU Awardee", 					   "NSH", new GregorianCalendar(2015,6,27,13,45,00).getTime(), new GregorianCalendar(2015,6,27,14,15,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=12");
		scheduleApp.addEvent("CMU Privacy Awardee", 			   "NSH", new GregorianCalendar(2015,6,27,14,15,00).getTime(), new GregorianCalendar(2015,6,27,14,45,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=13");
		scheduleApp.addEvent("Coffee Break", 					   "NSH", new GregorianCalendar(2015,6,27,14,45,00).getTime(), new GregorianCalendar(2015,6,27,15,15,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=14");
		scheduleApp.addEvent("Cornell Tech Security Awardee",      "NSH", new GregorianCalendar(2015,6,27,15,15,00).getTime(), new GregorianCalendar(2015,6,27,15,45,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=15");
		scheduleApp.addEvent("Demos and Walkthrough", 			   "NSH", new GregorianCalendar(2015,6,27,15,45,00).getTime(), new GregorianCalendar(2015,6,27,17,15,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=16");
		scheduleApp.addEvent("Dinner at the Porch", 			   "NSH", new GregorianCalendar(2015,6,27,18,00,00).getTime(), new GregorianCalendar(2015,6,27,20,00,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=17");
		
		scheduleApp.addEvent("Breakfast", 						   "NSH", new GregorianCalendar(2015,6,28,8,30,00).getTime(), new GregorianCalendar(2015,6,28,9,00,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=18");
		scheduleApp.addEvent("Google Cloud Platform Presentation", "NSH", new GregorianCalendar(2015,6,28,9,00,00).getTime(), new GregorianCalendar(2015,6,28,9,30,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=19");
		scheduleApp.addEvent("Google Privacy Presentation",        "NSH", new GregorianCalendar(2015,6,28,9,30,00).getTime(), new GregorianCalendar(2015,6,28,10,00,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=20");
		scheduleApp.addEvent("Planning", 						   "NSH", new GregorianCalendar(2015,6,28,10,00,00).getTime(), new GregorianCalendar(2015,6,28,10,45,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=21");
		scheduleApp.addEvent("Coffee Break", 					   "NSH", new GregorianCalendar(2015,6,28,10,45,00).getTime(), new GregorianCalendar(2015,6,28,11,00,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=22");
		scheduleApp.addEvent("Wrap Up", 						   "NSH", new GregorianCalendar(2015,6,28,11,00,00).getTime(), new GregorianCalendar(2015,6,28,11,30,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=23");
		scheduleApp.addEvent("Meet with CMU Faculty", 			   "NSH", new GregorianCalendar(2015,6,28,12,00,00).getTime(), new GregorianCalendar(2015,6,28,14,00,00).getTime(), "http://gcf.cmu-tbank.com/apps/google_iot/schedule.php?event=24");
		appProviders.add(scheduleApp);
		
		appProviders.add(new App_Survey(gcm, COMM_MODE, IP_ADDRESS, PORT));
	}
	
	/**
	 * This Method gets Called when the Device Receives Context Data
	 */
	@Override
	public void onContextData(ContextData data) 
	{
		if (data.getContextType().equals("BLUEWAVE"))
		{
			String context = data.getPayload("CONTEXT");
			
			if (context != null)
			{
				JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, data.getPayload("CONTEXT"));
				JsonObject locationObject = parser.getJSONObject("location");
				
				if (locationObject != null)
				{
					parser.getJSONObject("location").addProperty("SENSOR", data.getDeviceID());
				}
				
				this.queryProvider.processContext(parser.getJSONObject("device").get("deviceID").getAsString(), parser.toString(), "BLUEWAVE [" + data.getDeviceID() + "]");
			}
		}	
	}
	
	/**
	 * This Method Runs Threads Once per Minute
	 * @author adefreit
	 *
	 */
	public class UpdateThread extends Thread
	{
		public void run()
		{
			while (true)
			{
				try
				{
					if (t != null)
					{
						t.run();
					}
					
					if (f != null)
					{
						f.run();
					}

					if (t == null && f == null)
					{
						break;
					}
					else
					{
						sleep(UPDATE_THREAD_WAIT_TIME);	
					}
				}
				catch (Exception ex)
				{
					System.out.println("Problem in Update Thread: " + ex.getMessage());
				}
			}
		}
	}

	/**
	 * This Method Starts the Whole Application
	 * @param args
	 */
	public static void main(String[] args) 
	{
		if (args.length > 0)
		{
			new Application(args[0], false);
		}
		else
		{
			new Application("", false);
		}
	}
	
}
