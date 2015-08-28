package com.adefreitas.beacon.apps;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.util.Log;

import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.CommManager.CommMode;
import com.adefreitas.gcf.android.ContextReceiver;
import com.adefreitas.gcf.android.bluewave.JSONContextParser;
import com.adefreitas.gcf.android.impromptu.AndroidApplicationProvider;
import com.adefreitas.gcf.android.toolkit.CalendarToolkit;
import com.adefreitas.gcf.android.toolkit.CalendarToolkit.CalendarInfo;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.adefreitas.gcf.messages.ContextData;
import com.adefreitas.gcf.messages.ContextRequest;
import com.adefreitas.magicappserver.GCFApplication;

public class App_ContactCard extends AndroidApplicationProvider implements ContextReceiver
{
	private CalendarToolkit calendarToolkit;
	
	private String accountName;
	
	private HashMap<String, String> nextAppointment;
	
	/**
	 * Constructor
	 * @param application
	 * @param groupContextManager
	 * @param contextType
	 * @param name
	 * @param description
	 * @param contextsRequired
	 * @param preferencesToRequest
	 * @param logoPath
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 */
	public App_ContactCard(GCFApplication application, GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(  groupContextManager, 
				"CCARD", 
				"Office App",
				//"Contact Card (" + groupContextManager.getDeviceID() + ")", 
				"Info About User: NAME GOES HERE",
				"CONTACTS",
				new String[] { "CAL" }, 
				new String[] { "PREFERENCE" }, 
				"",
				30,
				commMode, 
				ipAddress, 
				port);
		
		Account[] accounts = AccountManager.get(application).getAccountsByType("com.google");
		
		if (accounts.length > 0)
		{
			this.accountName = accounts[0].name.split("@")[0];
			this.description   = "Information about " +  name;
			this.name		   = "Contact Card (" + accountName + ")"; 
		}
		
		// Informs the Application that This Object Wants Context Data as it Arrives
		application.setContextReceiver(this);
		
		// Creates the Object that Will Get Calendar Information
		calendarToolkit = new CalendarToolkit(application.getContentResolver());
		
		nextAppointment = new HashMap<String, String>();
	}

	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String ui  = "<html>";
		ui		  += "<title>Office 2602M</title>";
		ui 		  += "<h3>" + "Welcome to Adrian's Office!" + "</h3>";
		
		// Calendar UI
		ui += "<h4>Schedule Comparison Results</h4>";
		ui += "<div>" + nextAppointment.get(subscription.getDeviceID())  + "</div>";
		
		// Communications UI
		ui		  += "<h4>Send a Quick Message</h4><div>";
		ui 		  += "<div><input type=\"text\" id=\"txtMessage\" name=\"Message\" value=\"\"></div>";
		ui		  += "<div><input value=\"Send Message\" type=\"button\" style=\"height:50px; font-size:20px\"";
		ui		  += "  onclick=\"";
		ui		  += "    var myTextField = document.getElementById('txtMessage').value;";
		ui		  += "    device.sendComputeInstruction('MESSAGE', [myTextField]);";
		ui		  += "    device.toast('Message Sent');";
		ui		  += "\"/></div>";
		ui		  += "</html>";
		
		System.out.println(ui);
		
		return new String[] { "UI=" + ui };
	}

	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		this.getGroupContextManager().sendRequest("CAL", ContextRequest.SINGLE_SOURCE, new String[] { newSubscription.getDeviceID() }, 30000, new String[] { "days=1" });
		
		// Sets Output for this Device
		nextAppointment.put(newSubscription.getDeviceID(), "Looking for Matches");
		
		//Toast.makeText(this.getApplication(), newSubscription.getDeviceID() + " has subscribed.", Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		super.onSubscriptionCancelation(subscription);
		
		this.getGroupContextManager().cancelRequest("CAL");
		
		// Sets Output for this Device
		nextAppointment.put(subscription.getDeviceID(), "Looking for Matches");
		
		//Toast.makeText(this.getApplication(), subscription.getDeviceID() + " has unsubscribed.", Toast.LENGTH_LONG).show();
	}
	
	/**
	 * Determines Whether or Not to Send Application Data
	 */
	@Override
	public boolean sendAppData(String bluewaveContext)
	{
		return true;
	}

	/**
	 * Handles Compute Instructions Received from Clients
	 */
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
		
		if (instruction.getCommand().equalsIgnoreCase("MESSAGE"))
		{
			//this.getApplication().createNotification(new Intent(this.getApplication(), App_ContactCard.class), "Message", instruction.getPayload()[0]);
		}	
	}

	@Override
	public void onContextData(ContextData data)
	{		
		if (data.getContextType().equalsIgnoreCase("CAL"))
		{
			try
			{
				// Grabs the Calendar Events On THIS Device for the Rest of the Day
				Calendar cal = Calendar.getInstance();
				cal.setTime(new Date());
				cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 23, 59);
				ArrayList<CalendarInfo> personalCalendar = calendarToolkit.getCalendarInfo(cal.getTime());
								
				Calendar currentDate = Calendar.getInstance();
				currentDate.setTime(new Date());
				currentDate.set(Calendar.MINUTE, 0);
				currentDate.set(Calendar.SECOND, 0);
				int todayDayOfMonth = currentDate.get(Calendar.DAY_OF_MONTH);
				
				// Sets Output for this Device
				nextAppointment.put(data.getDeviceID(), "No Schedule Matches Found for Today");
				
				while (currentDate.get(Calendar.DAY_OF_MONTH) == todayDayOfMonth)
				{
					boolean isClientBusy = false;
					for (int i=0; i<data.getPayload().length; i++)
					{
						// Grabs the Contents from the User
						JSONObject obj = new JSONObject(data.getPayload()[i]);

						// Grabs the Start and Stop Date of the Event
						Date startDate = new Date(obj.getLong("StartTime"));
						Date endDate   = new Date(obj.getLong("EndTime"));
						
						Log.d(LOG_NAME, "  C_Event: " + obj.getString("EventName"));
						
						if (currentDate.getTimeInMillis() >= startDate.getTime() && currentDate.getTimeInMillis() <= endDate.getTime())
						{
							isClientBusy = true;
							break;
						}
					}
					
					boolean isServerBusy = false;
					for (CalendarInfo calendarEvent : personalCalendar)
					{
						Log.d(LOG_NAME, "  S_Event: " + calendarEvent.getEventName());
						
						if (currentDate.getTimeInMillis() >= calendarEvent.getStartDate().getTime() && currentDate.getTimeInMillis() <= calendarEvent.getEndDate().getTime())
						{
							isServerBusy = true;
							break;
						}
					}
					
					Log.d(LOG_NAME, currentDate.get(Calendar.HOUR_OF_DAY) + ":" + currentDate.get(Calendar.MINUTE) + "; ClientBusy:" + isClientBusy + "; ServerBusy:" + isServerBusy);
					
					if (!isClientBusy && !isServerBusy)
					{
						// Sets Output for this Device
						nextAppointment.put(data.getDeviceID(), "Your Next Available Time to Meet is:\n" + currentDate.getTime());						
						break;
					}
					
					currentDate.add(Calendar.MINUTE, 30);
				}
				
				this.sendContext();		
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	@Override
	public void onBluewaveContext(JSONContextParser parser)
	{
		
	}
}
