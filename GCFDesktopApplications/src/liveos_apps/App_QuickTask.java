package liveos_apps;

import java.util.Date;

import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;

public class App_QuickTask extends DesktopApplicationProvider
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
	public App_QuickTask(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(	groupContextManager, 
				"QTASK_" + groupContextManager.getDeviceID(), 
				"Request from John Doe", 
				"Return Printed Document", 
				"TASK",
				new String[] { }, 
				new String[] { }, 
				"http://25.media.tumblr.com/tumblr_kvewyajk5c1qzxzwwo1_500.jpg",
				30,
				commMode, 
				ipAddress, 
				port);
		
		taskTitle 		= "Return Printed Document";
		taskDescription = "I printed a document.  Can you bring it to my office?";
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
							sendContext();
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
				ui += "<br/><h4>Tasks to Perform</h4>";
				ui += "<div>1. Pick up the paper from the printer.</div>";
				ui += "<div>2. Go to the second floor.</div>";
				ui += "<div>2. Go to room 2602M.</div>";
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
		
		System.out.println(newSubscription.getDeviceID() + " has subscribed.");
	}
	
	@Override
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		super.onSubscriptionCancelation(subscription);
		
		System.out.println(subscription.getDeviceID() + " has unsubscribed.");
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
			this.sendContext();
		}
	}

}
