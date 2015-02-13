package liveos_dns;

import java.util.ArrayList;
import java.util.Date;

import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;

public class DNSProvider extends ContextProvider
{	
	// Constants
	private static String CONTEXT_TYPE = "LOS_DNS";
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public DNSProvider(GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
				
		// Sets it So That Any Device can Send a Command Message to this Device at Any Time!
		this.setSubscriptionDependentForCompute(false);
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		if (instruction.getCommand().equalsIgnoreCase("QUERY"))
		{
			runQuery(instruction.getDeviceID(), instruction.getParameters());
		}
		else if (instruction.getCommand().equalsIgnoreCase("SEND_ADVERTISEMENT"))
		{
			String appID    = CommMessage.getValue(instruction.getParameters(), "APP_ID");
			String deviceID = CommMessage.getValue(instruction.getParameters(), "DESTINATION");
						
			// Sends a Message about an Incoming Application
			System.out.println("Sending Application " + appID + " to " + deviceID + "\n");
			this.getGroupContextManager().sendComputeInstruction("PCP", new String[] { deviceID }, "APPLICATION", instruction.getParameters());
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
	public void sendMostRecentReading() 
	{
		// Do Nothing
	}

	/**
	 * INSTRUCTION (QUERY): Searches for Relevant Applications
	 * @param parameters
	 */
	public void runQuery(String deviceID, String[] parameters)
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
		
		System.out.println("\n--- DNS QUERY " + new Date() + " -----");
		System.out.println("Device:  " + deviceID + "\n");
		
		// Sends the Query
		this.getGroupContextManager().sendComputeInstruction("QUERY", new String[] { }, "DEVICE_QUERY", queryParameters.toArray(new String[0]));
	}
}
