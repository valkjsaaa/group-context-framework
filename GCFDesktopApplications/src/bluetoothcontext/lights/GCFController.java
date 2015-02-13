package bluetoothcontext.lights;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import bluetoothcontext.toolkit.GCFDesktopApplication;
import bluetoothcontext.toolkit.JSONContextParser;

import com.adefreitas.desktopframework.DesktopBatteryMonitor;
import com.adefreitas.desktopframework.DesktopGroupContextManager;
import com.adefreitas.desktopframework.MessageProcessor;
import com.adefreitas.desktopframework.RequestProcessor;
import com.adefreitas.desktopproviders.PhillipsHueProvider;
import com.adefreitas.desktoptoolkits.DropboxToolkit;
import com.adefreitas.desktoptoolkits.HttpToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.Settings;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ContextData;
import com.adefreitas.messages.ContextRequest;
import com.google.gson.JsonObject;

public class GCFController extends GCFDesktopApplication
{
	// Creates a Unique Computer Name (Needed for GCM to Operate, But You Can Change it to Anything Unique)
	public static final String COMPUTER_NAME = "LIGHT_CONTROLLER_" + new Date().getTime();
	
	// Application Settings
	public static final String 	  LIGHT_URL  = "http://192.168.1.25/api/gcfdeveloper/";
	public static final Integer[] LIGHT_IDs  = new Integer[] { 1, 2, 3 };
	public static final int 	  SLEEP_TIME = 60 * 1000;
	public static final int 	  ON_HOUR    = 21;
	public static final int 	  OFF_HOUR   = 6;	
		
	// Light Settings Providers
	public ContextProvider hueProvider; 
	public Date 		   lastNavRequest 		 = new Date(0);
	public final int 	   LIGHT_RETURN_DURATION = 120000;
	
	public GCFController()
	{
		super(COMPUTER_NAME);
		
		hueProvider = new PhillipsHueProvider(this.getGroupContextManager(), LIGHT_URL, LIGHT_IDs);
		this.getGroupContextManager().registerContextProvider(hueProvider);
		
		this.getGroupContextManager().sendRequest("HUE", ContextRequest.LOCAL_ONLY, 300000, new String[0]);
		
		setLightState();
		setupLightThread();
	}
	
	public void setupLightThread()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				try
				{
					while (true)
					{
						setLightState();
						Thread.sleep(SLEEP_TIME);
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}	
			}
		};
		
		t.start();
	}

	public void setLightState()
	{
		Calendar calendar = Calendar.getInstance();
		
		if (new Date().getTime() - lastNavRequest.getTime() < LIGHT_RETURN_DURATION)
		{
			this.getGroupContextManager().sendComputeInstruction("HUE", "LIGHT_RGB", new String[] { "true", "0", "0", "255" });
		}
		else if (calendar.get(Calendar.HOUR_OF_DAY) >= ON_HOUR || calendar.get(Calendar.HOUR_OF_DAY) <= OFF_HOUR)
		{
			this.getGroupContextManager().sendComputeInstruction("HUE", "LIGHT_RGB", new String[] { "true", "255", "255", "255"} );
		}
		else
		{
			this.getGroupContextManager().sendComputeInstruction("HUE", "LIGHT_RGB", new String[] { "false", "255", "255", "255"} );
		}
	}
	
	public void onContextData(ContextData data)
	{
		super.onContextData(data);
		
		// TODO:  Application Specific Stuff Goes Here
	}
	
	public void onContext(JSONContextParser parser)
	{
		System.out.println(new Date() + ":  Processing Context . . .");
		
		try
		{
			// Extracts Device Settings
			JsonObject deviceInfo  		= parser.getJSONObject("device");
			String     deviceID    		= parser.getString("deviceID", deviceInfo);
			String     commIP      		= parser.getString("commIP", deviceInfo);
			int	       commPort      	= Integer.parseInt(parser.getString("commPort", deviceInfo));
			String     commMode    		= parser.getString("commMode", deviceInfo);
			String     callbackProvider = parser.getString("callbackProvider", deviceInfo);
			
			// Extracts Context
			JsonObject contextInfo = parser.getJSONObject("context");
			String     address     = parser.getString("navigation", contextInfo);
			
			System.out.println("  Address: " + address);
			
			// Change Light Color
			if (address != null && address.equalsIgnoreCase("137 Chalet Drive, Pittsburgh, PA 15221, USA"))
			{
				System.out.println("*** FOUND SOMEONE LOOKING FOR MY ADDRESS ***");
				lastNavRequest = new Date();
				setLightState();
				
				if (deviceID != null && commMode != null)
				{
					System.out.println("  TEST ME OUT TO MAKE SURE I WORK!!!");
					this.getGroupContextManager().sendComputeInstruction(callbackProvider, new String[] { deviceID }, "toast", new String[] { "I changed my lights for you!" });
				}
			}
			
			System.out.println("Processing Complete!\n");
		}
		catch (Exception ex)
		{
			System.out.println("A problem occurred while processing context: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
}
