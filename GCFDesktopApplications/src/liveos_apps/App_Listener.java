package liveos_apps;

import java.util.Date;

import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.desktopframework.toolkit.SQLToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_Listener extends DesktopApplicationProvider
{
	private String 		lastOutput;
	private int 	 	entriesRecorded;
	private Date		dateStarted;
		
	private SQLToolkit sqlToolkit;
	
	public App_Listener(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port, SQLToolkit sqlToolkit)
	{
		super(groupContextManager, 
				"LISTEN",
				"Listener Application",
				"This app is recording all IMPROMPTU context delivered.",
				"DEBUG",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"https://cdn1.iconfinder.com/data/icons/MetroStation-PNG/128/MB__listen.png", // LOGO
				120,
				commMode,
				ipAddress,
				port);
		
		// Initializes Objects
		this.sqlToolkit = sqlToolkit;
		dateStarted 	= new Date();
		entriesRecorded	= 0;
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String ui = "<html>";
		ui       += "<title>Listener App</title>";
		ui       += "<p><b>Started:</b> " + dateStarted + "</p>";
		ui       += "<p><b>Current Time:</b> " + new Date() + "</p>";
		ui		 += "<p><b># Records:</b> " + entriesRecorded + "</p>";
		ui		 += "<p><b>Last Entry:</b><br/>" + lastOutput + "</p>";
		ui       += "</html>";
		
		return new String[] { "UI=" + ui };
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	public void recordContext(JSONContextParser parser)
	{	
		try
		{
			String updateQuery = String.format("INSERT INTO usercontext (deviceID, timestamp, latitude, longitude, activity) VALUES ('%s','%s',%f,%f,'%s');",
					this.getDeviceName(parser), new java.sql.Timestamp(System.currentTimeMillis()).toString(), this.getLatitude(parser), this.getLongitude(parser), this.getActivity(parser));
			
			// Runs the SQL Query
			sqlToolkit.runUpdateQuery(updateQuery);
					
			// Increments the Counter
			entriesRecorded++;
			
			// Remembers the Last Thing Output
			lastOutput = updateQuery;
		}
		catch (Exception ex)
		{
			System.out.println(" *** Problem Encountered while Recording Context: " + ex.getMessage() + " ***");
		}
	}
	
	@Override
	public boolean sendAppData(String json)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		
		// Records Context
		recordContext(parser);
		
		return this.hasEmailAddress(parser, "adrian.defreitas@gmail.com");
	}
}
