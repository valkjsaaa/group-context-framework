package liveos_apps;

import java.util.ArrayList;

import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.liveos.ApplicationProvider;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;

public class QueryApplicationProvider extends ContextProvider
{	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public QueryApplicationProvider(GroupContextManager groupContextManager) 
	{
		super("QUERY", groupContextManager);

		// Sets it So That Any Device can Send a Command Message to this Device at Any Time!
		this.setSubscriptionDependentForCompute(false);
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{		
		if (instruction.getCommand().equals("DEVICE_QUERY"))
		{			
			String deviceID = CommMessage.getValue(instruction.getParameters(), "DEVICE_ID");
			String context  = CommMessage.getValue(instruction.getParameters(), "CONTEXT");
			
			if (context != null && deviceID != null)
			{
				// Looks for All Available Application Providers
				for (ContextProvider provider : this.getGroupContextManager().getRegisteredProviders())
				{
					if (provider instanceof ApplicationProvider)
					{
						ApplicationProvider application = (ApplicationProvider)provider;
						
						System.out.print("Giving context to " + application.getAppID() + " . . . ");
						
						if (application.sendAppData(context))
						{
							System.out.println("MATCH " + deviceID);
							
							// Creates a List of Parameters
							ArrayList<String> parameters = new ArrayList<String>();
							
							for (String s : application.getInformation())
							{
								parameters.add(s);
							}
							
							// Adds a Custom Parameter Needed by the DNS to Know Which Device this Advertisement is For
							parameters.add("DESTINATION=" + deviceID);
							
							// Adds the Number of Matches for a Snap To It Capable Device
							if (application instanceof SnapToItApplicationProvider)
							{
								SnapToItApplicationProvider stiApp = (SnapToItApplicationProvider)application;
								
								// Adds the Number of Matches!
								parameters.add("PHOTO_MATCHES=" + stiApp.getFitness(context));
							}
							
							// Directs the DNS to Send the Advertisement to the Device
							getGroupContextManager().sendComputeInstruction("LOS_DNS", new String[] { "LOS_DNS" }, "SEND_ADVERTISEMENT", parameters.toArray(new String[0]));
						}	
						else
						{
							System.out.println("NO MATCH " + deviceID);
						}
					}
				}
			}
			else
			{
				System.out.println("CONTEXT OR DEVICE ID IS NULL");
			}
		}
	}
	
	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public double getFitness(String[] parameters) {
		// TODO Auto-generated method stub
		return 1.0;
	}
	
	@Override
	public void sendMostRecentReading() {
		
	}
}
