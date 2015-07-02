package impromptu_apps;


import impromptu_apps.snaptoit.SnapToItApplicationProvider;

import java.util.ArrayList;
import java.util.Date;

import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.liveos.ApplicationProvider;
import com.adefreitas.liveos.ApplicationSettings;
import com.adefreitas.messages.ComputeInstruction;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class QueryApplicationProvider extends ContextProvider
{	
	private String connectionKey;
	
	private Gson gson = new Gson();
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public QueryApplicationProvider(GroupContextManager groupContextManager, String connectionKey) 
	{
		super("QUERY", groupContextManager);
		
		this.connectionKey = connectionKey;

		// Sets it So That Any Device can Send a Command Message to this Device at Any Time!
		this.setSubscriptionDependentForCompute(false);
	}
	
	/**
	 * Matches the Apps to the User
	 */
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{		
		if (instruction.getCommand().equals("DEVICE_QUERY"))
		{			
			String deviceID  = instruction.getPayload("DEVICE_ID");
			String context   = instruction.getPayload("CONTEXT");
			
			processContext(deviceID, context, "APP_DIRECTORY");
		}
	}
	
	public void processContext(String deviceID, String context, String source)
	{
		Date startTime = new Date();
		
		// Prints a Debug Line
		System.out.println("QUERY [" + deviceID + "]: "+ new Date() + " (from " + source + ")");
		
		ArrayList<ApplicationProvider> relevantApps = new ArrayList<ApplicationProvider>();
				
		if (context != null && deviceID != null)
		{
			// Looks for All Available Application Providers
			for (ContextProvider provider : this.getGroupContextManager().getRegisteredProviders())
			{
				if (provider instanceof ApplicationProvider)
				{
					DesktopApplicationProvider application = (DesktopApplicationProvider)provider;
					
					System.out.print("  " + application.getAppID() + ": ");
					
					// Determines if Sending an App Advertisement would be Redundant
					boolean redundant       = isRedundant(deviceID, application, context);
					boolean appDecision     = application.sendAppData(context);
					boolean signed          = application.signedDisclaimer(new JSONContextParser(JSONContextParser.JSON_TEXT, context));
					boolean isDisclaimerApp = application.getAppID().equals("APP_DISCLAIMER");
					
					if (!redundant && appDecision && (signed || isDisclaimerApp))
					{
						System.out.print("YES ");
						relevantApps.add(application);
						//sendAdvertisement(deviceID, context, application);
					}	
					else
					{
						if (redundant)
						{
							System.out.print("NO (REDUNDANT) ");
						}
						else
						{
							System.out.print("NO ");
						}
					}
					
					// Prints the Amount of Processing Time
					System.out.println("(" + (new Date().getTime() - startTime.getTime()) + " ms)");
				}
			}
			
			// Transmits All Relevant Apps in ONE Burst
			if (relevantApps.size() > 0)
			{
				sendAdvertisement(deviceID, context, relevantApps);
			}
		}
		else
		{
			if (context == null)
			{
				System.out.println("PROBLEM: Context is Null");
			}
			else if (deviceID == null)
			{
				System.out.println("PROBLEM: Device ID is Null");
			}
		}
		
		// Prints a Blank Line
		System.out.println();
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
	public void sendContext() {
		
	}

	/**
	 * Sends an Advertisement for ONE App
	 * @param deviceID
	 * @param userContext
	 * @param application
	 */
	private void sendAdvertisement(String deviceID, String userContext, ApplicationProvider application)
	{
		// Creates a List of Parameters
		ArrayList<String> parameters = new ArrayList<String>();
		
		for (String s : application.getInformation(userContext))
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
			parameters.add("PHOTO_MATCHES=" + stiApp.getFitness(userContext));
		}
		
		// Directs the DNS to Send the Advertisement to the Device
		getGroupContextManager().sendComputeInstruction(connectionKey, 
				ApplicationSettings.DNS_CHANNEL, 
				"LOS_DNS", 
				new String[] { "LOS_DNS" }, 
				"SEND_ADVERTISEMENT", 
				parameters.toArray(new String[0]));
	}
	
	/**
	 * Sends an Advertisement for MULTIPLE Apps
	 * @param deviceID
	 * @param userContext
	 * @param application
	 */
	private void sendAdvertisement(String deviceID, String userContext, ArrayList<ApplicationProvider> applications)
	{	
		// Creates a List of Parameters
		ArrayList<String> parameters = new ArrayList<String>();
		
		// Generates Parameters for Each Application
		for (ApplicationProvider application : applications)
		{
			ArrayList<String> appParameters = new ArrayList<String>();
			
			// Adds the Number of Matches for a Snap To It Capable Device
			if (application instanceof SnapToItApplicationProvider)
			{
				SnapToItApplicationProvider stiApp = (SnapToItApplicationProvider)application;
				
				// Adds the Number of Matches!
				appParameters.add("PHOTO_MATCHES=" + stiApp.getFitness(userContext));
			}
			
			for (String s : application.getInformation(userContext))
			{
				appParameters.add(s);
			}
			
			parameters.add(gson.toJson(appParameters.toArray(new String[0])));
		}
		
		//System.out.println("DESTINATION=" + deviceID);
		//System.out.println("APPS=" + gson.toJson(parameters.toArray(new String[0])));
		System.out.println("  Delivering " + applications.size() + " apps total.");
		
		getGroupContextManager().sendComputeInstruction(connectionKey, 
				ApplicationSettings.DNS_CHANNEL, 
				"LOS_DNS", 
				new String[] { "LOS_DNS" }, 
				"SEND_ADVERTISEMENT", 
				new String[] { "DESTINATION=" + deviceID, "APPS=" + gson.toJson(parameters.toArray(new String[0])) } );
	}
	
	// Private Methods --------------------------------------------------------------------------
	private boolean isRedundant(String deviceID, ApplicationProvider appProvider, String context)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, context);
		
		if (parser.getJSONObject("identity") != null && parser.getJSONObject("identity").has("APPS"))
		{
			try
			{
				JsonArray apps = parser.getJSONObject("identity").getAsJsonArray("APPS");
				
				for (int i=0; i<apps.size(); i++)
				{			
					JsonObject appObject = apps.get(i).getAsJsonObject();
					
					if (appObject.get("name").getAsString().equals(appProvider.getContextType()))
					{
						if (appObject.has("expiring"))
						{
							return !appObject.get("expiring").getAsBoolean();
						}
						else
						{
							return false;
						}
					}
				}
			}
			catch (Exception ex)
			{
				System.out.println("Problem analyzing context from " + deviceID + ": " + ex.getMessage());
				ex.printStackTrace();
			}
		}
		
		return false;
	}
}
