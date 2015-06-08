package impromptu_apps;

import impromptu_apps.creationfest.TaskDispatcher;
import impromptu_apps.favors.FavorDispatcher;

import java.util.ArrayList;


import com.adefreitas.desktopframework.DesktopBatteryMonitor;
import com.adefreitas.desktopframework.DesktopGroupContextManager;
import com.adefreitas.desktopframework.EventReceiver;
import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.desktopframework.toolkit.SQLToolkit;
import com.adefreitas.groupcontextframework.CommManager;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.liveos.ApplicationSettings;
import com.adefreitas.messages.ContextData;
import com.google.gson.JsonObject;

public class Application implements EventReceiver
{
	// Creates a Unique Computer Name (Needed for GCM to Operate, But You Can Change it to Anything Unique)
	public String COMPUTER_NAME = "";
	
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
		sqlToolkit = new SQLToolkit("epiwork.hcii.cs.cmu.edu", "adrian", "@dr1@n1234", "GCF");
		
		// Initializes Apps
		initializeApps();
		
		// Initializes Communications Channel
		for (DesktopApplicationProvider app : appProviders)
		{
			gcm.registerContextProvider(app);
			gcm.subscribe(connectionKey, app.getContextType());
			System.out.println("Application [" + app.toString() + "] Ready.");
		}
		
		System.out.println(appProviders.size() + " App(s) Initialized!\n");
	}

	/**
	 * This Creates all of the Apps that Impromptu Users will See
	 */
	private void initializeApps()
	{
		// Standard Apps
//		appProviders.add(new App_Disclaimer(gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new App_Michaels(gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new App_HalfPriceBooks(gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new App_Target(gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new App_Starbucks(gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new App_BestBuy(gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new App_Weather(gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new App_Bus(gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new App_Feedback(gcm, sqlToolkit, COMM_MODE, IP_ADDRESS, PORT));
// 	    appProviders.add(new App_BluewaveDebug(gcm, COMM_MODE, IP_ADDRESS, PORT));
		
 	    appProviders.add(new App_Listener(gcm, COMM_MODE, IP_ADDRESS, PORT, sqlToolkit));
		appProviders.add(new App_HomeLights(gcm, COMM_MODE, IP_ADDRESS, PORT));
		appProviders.add(new App_HomeNest(gcm, COMM_MODE, IP_ADDRESS, PORT));
		
		// CreationFest
		//appProviders.add(new App_CreationFestAlert(gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new App_CreationFestReporter(gcm, COMM_MODE, IP_ADDRESS, PORT));
//		appProviders.add(new App_CreationFestProfile(gcm, COMM_MODE, IP_ADDRESS, PORT));
//		t = new TaskDispatcher(sqlToolkit, gcm);
		
		// Favors
		//f = new FavorDispatcher(sqlToolkit, gcm);
		
		// Snap-To-It Apps
		//appProviders.add(new Sti_Diagnostics(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new Sti_Printer(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new Sti_Map(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new Sti_Game(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new Sti_PowerPoint(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new Sti_PowerPointDemo(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new Sti_DoorPlate(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new Sti_Paint(gcm, COMM_MODE, IP_ADDRESS, PORT));
				
		// Misc Impromptu Apps
		//appProviders.add(new App_Diagnostics(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_Hershey(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_CHI2014(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_PowerPoint(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_Printer(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_PlayFeedly(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_Away(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_Game_PiratePig(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_Flickr(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_Game_Simon(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_PlayGmail(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_PlayAngryBirds(gcm, COMM_MODE, IP_ADDRESS, PORT));
		//appProviders.add(new App_QuickTask(gcm, COMM_MODE, IP_ADDRESS, PORT));
		
		updateThread = new UpdateThread();
		updateThread.start();
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
						sleep(60000);	
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