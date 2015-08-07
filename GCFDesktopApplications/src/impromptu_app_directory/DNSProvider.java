package impromptu_app_directory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.impromptu.ApplicationSettings;
import com.adefreitas.gcf.messages.CommMessage;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class DNSProvider extends ContextProvider
{	
	// Constants
	private static String CONTEXT_TYPE 	  = "LOS_DNS";
	private static int    MIN_REPEAT_TIME = 5000;
	
	// Parameters
	private String connectionKey;
	
	// Archives
	private HashMap<String, Date> archives = new HashMap<String, Date>();
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public DNSProvider(GroupContextManager groupContextManager, String connectionKey) 
	{
		super(CONTEXT_TYPE, groupContextManager);
		
		this.connectionKey = connectionKey;
				
		// Sets it So That Any Device can Send a Command Message to this Device at Any Time!
		this.setSubscriptionRequiredForCompute(false);
	}

	/**
	 * This Method 
	 */
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		if (instruction.getCommand().equalsIgnoreCase("QUERY"))
		{
			// Analyzes the Query Message
			runQuery(instruction.getDeviceID(), instruction.getPayload());
			
			// Archives the Date Since the Last Query
			archives.put(instruction.getDeviceID(), new Date());
		}
		else if (instruction.getCommand().equalsIgnoreCase("SEND_ADVERTISEMENT"))
		{
//			String appID    = instruction.getPayload("APP_ID");
			String deviceID = instruction.getPayload("DESTINATION");
						
			// Sends a Message about an Incoming Application
			System.out.println(instruction.getDeviceID() + " -> " + deviceID);
			
			this.getGroupContextManager().sendComputeInstruction(connectionKey, "dev/" + deviceID, "PCP", new String[] { deviceID }, "APPLICATION", instruction.getPayload());
		}
		else
		{
			System.out.println("UNKNOWN INSTRUCTION: " + instruction.getCommand());
		}
	}
	
	@Override
	public void start() 
	{
		// Do Nothing
	}

	@Override
	public void stop() 
	{
		// Do Nothing		
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendContext() 
	{
		// Do Nothing
	}

	/**
	 * INSTRUCTION (QUERY): Searches for Relevant Applications
	 * @param parameters
	 */
	public void runQuery(String deviceID, String[] parameters)
	{
		System.out.println("\n--- DNS QUERY " + new Date() + " -----");
		System.out.println("Device:  " + deviceID);
		
		if (!archives.containsKey(deviceID) || (new Date().getTime() - archives.get(deviceID).getTime() >= MIN_REPEAT_TIME))
		{
			//Creates Query Parameters
			ArrayList<String> queryParameters = new ArrayList<String>();
			
			// Creates a Copy of Parameters
			for (String s : parameters)
			{
				queryParameters.add(s);
			}
			
			// Creates Custom Query Parameters
			queryParameters.add("DEVICE_ID=" + deviceID);
			
			// Sends the Query to All Application Providers
			this.getGroupContextManager().sendComputeInstruction(connectionKey, ApplicationSettings.DNS_APP_CHANNEL, "QUERY", new String[] { }, "DEVICE_QUERY", queryParameters.toArray(new String[0]));
		}
		else
		{
			System.out.println("Ignoring due to multiple repeated messages in last " + MIN_REPEAT_TIME + "ms.");
		}
	}
}
