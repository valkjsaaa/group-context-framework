package bluetoothcontext.template;

import java.util.Date;

import bluetoothcontext.toolkit.GCFDesktopApplication;
import bluetoothcontext.toolkit.JSONContextParser;

import com.adefreitas.messages.ContextData;
import com.google.gson.JsonObject;

public class GCFController extends GCFDesktopApplication
{
	// Creates a Unique Computer Name (Needed for GCM to Operate, But You Can Change it to Anything Unique)
	public static final String COMPUTER_NAME = "CONTROLLER_" + new Date().getTime();
	
	// Application Settings
	
	public GCFController()
	{
		super(COMPUTER_NAME);
	}
	
	public void onContextData(ContextData data)
	{
		super.onContextData(data);
		
		// TODO:  Application Specific Stuff Goes Here
	}
	
	public void onContext(JSONContextParser parser)
	{
		System.out.println("Processing Context . . .");
		
		// Extracts Device Settings
		JsonObject deviceInfo  		= parser.getJSONObject("device");
		String     deviceID    		= parser.getString("deviceID", deviceInfo);
		String     commIP      		= parser.getString("commIP", deviceInfo);
		int	       commPort      	= Integer.parseInt(parser.getString("commPort", deviceInfo));
		String     commMode    		= parser.getString("commMode", deviceInfo);
		String     emailAddress    	= parser.getString("email", deviceInfo);
		String     callbackProvider = parser.getString("callbackProvider", deviceInfo);
		
		System.out.println("Processing Complete!\n");
	}
	
}
