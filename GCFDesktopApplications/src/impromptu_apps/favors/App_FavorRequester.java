package impromptu_apps.favors;

import impromptu_apps.DesktopApplicationProvider;

import java.sql.ResultSet;
import java.util.Calendar;


import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.desktop.toolkit.JSONContextParser;
import com.adefreitas.gcf.desktop.toolkit.SQLToolkit;
import com.adefreitas.gcf.messages.ComputeInstruction;

public class App_FavorRequester extends DesktopApplicationProvider
{
	private SQLToolkit toolkit;
	
	public App_FavorRequester(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port, SQLToolkit toolkit)
	{
		// Creates App with Default Settings
		super(groupContextManager, 
				"FAVOR_REQUEST",
				"Favor Requester",
				"This app lets you request a favor.",
				"FAVORS",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://cdn5.fedobe.com/wp-content/uploads/2012/09/service-icon-concept.png", // LOGO
				3600,
				commMode,
				ipAddress,
				port);
		
		this.toolkit = toolkit;
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		return new String[] { "WEBSITE=http://gcf.cmu-tbank.com/apps/favors/submitFavor.php?deviceID=" + subscription.getDeviceID()};
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

	public int getLifetime(String userContextJSON)
	{
		return this.lifetime;
	}
	
	@Override
	public boolean sendAppData(String json)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		
		try
		{
			String    query   = String.format("SELECT device_id FROM favors_profile WHERE device_id='%s' AND status=1;", this.getDeviceID(parser)); 
			ResultSet results = toolkit.runQuery(query);
			return results.next();
		}
		catch (Exception ex)
		{
			System.out.println("PRoblem Occurred Checking DeviceID Against Database: " + ex.getMessage());
		}
		
		return false;
	}
}
