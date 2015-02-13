package com.adefreitas.magicappserver;

import java.util.Date;

import android.widget.Toast;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_QuickTask extends AndroidApplicationProvider
{
	private String taskTitle;
	private String taskDescription;
	
	private String acceptedBy;
	private Date   acceptedDate;
	
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
	public App_QuickTask(GCFApplication application, GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(application, 
				groupContextManager, 
				"QTASK_" + groupContextManager.getDeviceID(), 
				"Quick Task (" + groupContextManager.getDeviceID() + ")", 
				"Someone printed a document, and is offering $0.25 to anybody who will take it to their office.", 
				"TASK",
				new String[] { }, 
				new String[] { }, 
				"http://74.111.161.33/gcf/universalremote/magic/dots.png",
				30,
				commMode, 
				ipAddress, 
				port);
		
		taskTitle 		= "Return the Printed Document.";
		taskDescription = "Adrian has just printed a document on PEWTER.  Will you take it to his office?";
		acceptedBy      = "";
		acceptedDate    = new Date();
		
		setupTimer();
	}

	private void setupTimer()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				while (true)
				{
					try
					{
						if (acceptedBy.length() > 0 && (new Date().getTime() - acceptedDate.getTime()) > 120000)
						{
							acceptedBy = "";
							setLifetime(30);
							sendMostRecentReading();
						}
						sleep(1000);
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			}
		};
		
		t.start();
	}
		
	/**
	 * Generates a Custom Interface for this Subscription
	 */
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String ui  = "<html>";
		ui		  += "<title>Quick Task</title>";
		ui 		  += "<h3>Task:  " + taskTitle + "</h3>";
		
		// Calendar UI
		ui += "<h4>Description</h4>";
		ui += "<div>" + taskDescription  + "</div>";
				
		// Button
		if (acceptedBy.length() == 0)
		{
			ui += "<br/><div><input value=\"ACCEPT TASK\" type=\"button\" style=\"height:50px; font-size:20px\"";
			ui += "  onclick=\"";
			ui += "    device.sendComputeInstruction('ACCEPT', []);";
			ui += "    device.toast('Accepting Task');";
			ui += "\"/></div>";			
		}
		else
		{
			if (subscription.getDeviceID().equals(acceptedBy))
			{
				ui += "<br/><h4>Tasks to Perform</h4><div>1. Get the printed document on PEWTER.</div>";
				ui += "<div>2. Go to NSH 2602M.</div>";
			}
			else
			{
				ui += "<br/><h4>Task Already Taken</h4><div>Someone else has already agreed to perform this task.</div>";	
			}
		}

		System.out.println(ui);
		
		return new String[] { "UI=" + ui };
	}

	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		Toast.makeText(this.getApplication(), newSubscription.getDeviceID() + " has subscribed.", Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		super.onSubscriptionCancelation(subscription);
		
		Toast.makeText(this.getApplication(), subscription.getDeviceID() + " has unsubscribed.", Toast.LENGTH_LONG).show();
	}
	
	/**
	 * Determines Whether or Not to Send Application Data
	 */
	@Override
	public boolean sendAppData(String bluewaveContext)
	{
		return acceptedBy.length() == 0;
	}

	/**
	 * Handles Compute Instructions Received from Clients
	 */
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
		
		if (instruction.getCommand().equalsIgnoreCase("accept"))
		{
			acceptedBy   = instruction.getDeviceID();
			acceptedDate = new Date();
			setLifetime(0);
			this.sendMostRecentReading();
		}
	}

}
