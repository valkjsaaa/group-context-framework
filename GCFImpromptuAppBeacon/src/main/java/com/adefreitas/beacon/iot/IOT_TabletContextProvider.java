package com.adefreitas.beacon.iot;

import java.util.Locale;

import org.json.JSONObject;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.adefreitas.gcf.ContextProvider;
import com.adefreitas.gcf.ContextSubscriptionInfo;
import com.adefreitas.gcf.GroupContextManager;
import com.adefreitas.gcf.android.*;
import com.adefreitas.gcf.android.providers.legacy.HTTPAsyncTask;
import com.adefreitas.gcf.messages.ComputeInstruction;
import com.adefreitas.gcf.messages.ContextRequest;

public class IOT_TabletContextProvider extends IOTContextProvider
{
	// CONFIGURATION VARIABLES
	private static final String CONTEXT_TYPE = "IOT_TABLET_LIGHT";
	private static final String LOG_NAME     = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	private static final String DESCRIPTION  = "This is a simple context provider used to test IOT remote control applications.";
	
	private TextToSpeech tts;
	
	/**
	 * Constructor.  Creates a New Instance of this Context Provider.  Note that this method does not register the context
	 * provider with the GCM.  You still need to do this manually.
	 * @param groupContextManager - a link to the Group Context Manager
	 */
	public IOT_TabletContextProvider(Context context, GroupContextManager groupContextManager, String category) 
	{
		super(CONTEXT_TYPE, DESCRIPTION, context, groupContextManager, category);
		
		tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() 
		{
	         @Override
	         public void onInit(int status) 
	         {
	            if(status != TextToSpeech.ERROR) 
	            {
	               tts.setLanguage(Locale.US);
	            }
	         }
	     });
	}

	/**
	 * This method is used to Start a context provider (initialize sensors).  
	 * It is automatically called by the GCM when the FIRST device subscribes to it
	 * (i.e., numSubscriptions goes from 0->1)
	 */
	@Override
	public void start() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started");
	}

	/**
	 * This method is used to Stop a context provider (turn off sensors).  
	 * It is automatically called by the GCM when the LAST device unsubscribes from it
	 * (i.e., numSubscriptions goes from 1->0) 
	 */
	@Override
	public void stop() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
	}

	/**
	 * Returns a numeric value representing the "goodness" of this context provider for a specific Context Request.
	 * This value is used by the GCM to compare context providers of the same type.
	 */
	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	/**
	 * Sends context to all subscribed devices.  This method is automatically called by the framework as needed.
	 */
	@Override
	public void sendContext() 
	{
		AndroidGroupContextManager gcm = (AndroidGroupContextManager)this.getGroupContextManager();
		
		this.getGroupContextManager().sendContext(this.getContextType(), 
				this.getSubscriptionDeviceIDs(), 
				new String[] {"CONTEXT=" + gcm.getBluewaveManager().getPersonalContextProvider().getContext().toString()});
	}
	
	/**
	 * Creates a JSON Object that Contains All of the IOT Information
	 * IMPORTANT:  We will need to fix this in the future so that it is standardized and easy to use
	 */
	@Override
	public JSONObject getJSON() {
		try
		{
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("type", "lights");
			JSONObject capabilityObj = new JSONObject();
						
			JSONObject speakInput = new JSONObject().put("TEXT", "String");
			JSONObject lightsInput = new JSONObject().put("ON", "Boolean")
					.put("R", "Integer")
					.put("G", "Integer")
					.put("B", "Integer");
			
			
			capabilityObj.put("identify",  defineMethod("IDENTIFY", new JSONObject(), new JSONObject()));
			capabilityObj.put("notify",    defineMethod("NOTIFY", new JSONObject(), new JSONObject()));
			//capabilityObj.put("speak",     defineMethod("SPEAK", speakInput, new JSONObject()));
			capabilityObj.put("setLights", defineMethod("SETLIGHTS", lightsInput, new JSONObject()));
			
			jsonObj.put("capability", capabilityObj);
			return jsonObj;
		}
		catch (Exception ex)
		{
			return null;
		}
	}	
	
	public void speak(String textToSpeak)
	{		
		if (textToSpeak != null)
		{
			tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null);
		}
		else
		{
			tts.speak("Hello World", TextToSpeech.QUEUE_FLUSH, null);
		}
	}
	
	private JSONObject defineMethod(String command, JSONObject input, JSONObject output)
	{
		try
		{
			JSONObject parameterObj = new JSONObject();
			parameterObj.put("contextProvider", this.getContextType());
			parameterObj.put("command", command);
			parameterObj.put("channel", "cmu/gcf_framework");
			parameterObj.put("messageType", "I");
			parameterObj.put("input", input);
			parameterObj.put("output", output);
			
			return parameterObj;
		}
		catch (Exception ex)
		{
			Log.e(LOG_NAME, "Problem Defining " + command + ": " + ex.getMessage());
		}
		
		return null;
	}
	
	// -----------------------------------------------------------------------------------------------------------------------
	// OPTIONAL METHODS
	// Check ContextProvider.java for a more complete list of optional methods.  The ones below are the ones that are 
	// most commonly modified by custom context providers
	// -----------------------------------------------------------------------------------------------------------------------
	boolean tmp = false;
	
	/**
	 * Event that fires when the context provider receives a Compute Instruction from another device
	 * @param instruction The Compute Instruction from another device
	 */
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		if (instruction.getCommand().equalsIgnoreCase("identify"))
		{			
			Vibrator v = (Vibrator) this.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(500);
		}
		else if (instruction.getCommand().equalsIgnoreCase("notify"))
		{	
			//this.speak("You have a Notification.");
			
			if (tmp)
			{
				this.getGroupContextManager().sendComputeInstruction("HUE", new String[] { "IMPROMPTU_LIGHTS" }, "LIGHT_RGB", new String[] { "ON=true", "R=255", "G=0", "B=0" });	
			}
			else
			{
				this.getGroupContextManager().sendComputeInstruction("HUE", new String[] { "IMPROMPTU_LIGHTS" }, "LIGHT_RGB", new String[] { "ON=true", "R=0", "G=0", "B=255" });
			}
			
			tmp = !tmp;
		}
		else if (instruction.getCommand().equalsIgnoreCase("speak"))
		{	
			String text = instruction.getPayload("TEXT");
			
			if (text != null)
			{
				this.speak(text);
			}
			else
			{
				this.speak("No Text Provided");
			}
		}
		else if (instruction.getCommand().equalsIgnoreCase("setlights"))
		{	
			String on = instruction.getPayload("ON");
			String r  = instruction.getPayload("R");
			String g  = instruction.getPayload("G");
			String b  = instruction.getPayload("B");
			
			this.getGroupContextManager().sendComputeInstruction("HUE", new String[] { "IMPROMPTU_LIGHTS" }, "LIGHT_RGB", new String[] { "ON=" + on, "R=" + r, "G=" + g, "B=" + b });
		}	
			
	}
	
	/**
	 * Specifies the Rate at which devices should resend their request (before getting automatically unsubscribed)
	 * @param request - The Context Request of the device
	 * @return - the Heartbeat Rate (in ms)
	 */
	public int getHeartbeatRate(ContextRequest request)
	{
		return super.getHeartbeatRate(request);
	}

	/**
	 * Event that fires when a device subscribes
	 * @param newSubscription The subscription details for the requesting device
	 */
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
	}
	
	/**
	 * Event that fires when a device unsubscribes (intentionally, or automatically by the system)
	 * @param subscription The subscription details for the unsubscribing device
	 */
	public void onSubscriptionCancelation(ContextSubscriptionInfo subscription)
	{
		super.onSubscriptionCancelation(subscription);
	}

	
}
