package com.adefreitas.androidproviders.legacy;

import java.util.ArrayList;

import org.json.JSONObject;

import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.ComputeInstruction;
import com.adefreitas.messages.ContextRequest;

/**
 * Controls Phillips Hue Lights
 * @author adefreit
 * COMMAND: "LIGHT_STATE"; PARAMETERS: {TRUE/FALSE, BRIGHTNESS, SATURATION, HUE };
 */
public class PhillipsHueProvider extends ContextProvider
{	
	private static final String LIGHT_STATE_COMMAND    = "LIGHT_STATE";
	private static final String LIGHT_RGB_COMMAND      = "LIGHT_RGB";
	private static final String LIGHT_SPECTRUM_COMMAND = "LIGHT_SPECTRUM";
	
	private String            apiURL;
	private ArrayList<String> capabilities;
	private Integer[]         lightIDs;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public PhillipsHueProvider(GroupContextManager groupContextManager, String url, Integer[] lightIDs) 
	{
		super("HUE", groupContextManager);
		
		// Saves the Link to the API URL
		apiURL = (url.endsWith("/")) ? url : url + "/";
		
		// This array stores the capabilities that it is willing to provide
		capabilities = new ArrayList<String>();
		
		// Initializes Its Own Capabilities
		capabilities.add(LIGHT_STATE_COMMAND);
		
		// Initializes Its Light IDs
		this.lightIDs = lightIDs;
	}

	// CONTEXT PROVIDER METHODS ---------------------------------------------------------------------
	@Override
	public void start() 
	{		
		System.out.println("Hue Light Provider is Running");
	}

	@Override
	public void stop() 
	{
		System.out.println("Hue Light Provider is Stopping");
		
		// TODO:  Maybe Change This?
		ComputeInstruction resetInstruction = new ComputeInstruction(this.getContextType(), this.getGroupContextManager().getDeviceID(), new String[] { this.getGroupContextManager().getDeviceID() }, LIGHT_RGB_COMMAND, new String[] {"true", "255", "255", "255"});
		onComputeInstruction(resetInstruction);
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		if (lightIDs.length > 0)
		{
			return 1.0;
		}
		else
		{
			return 0.0;
		}
	}

	@Override
	public void sendContext() 
	{		
		String strCapabilities = "";
		String lgtIds          = "";
		
		// Gets a List of all Capabilities
		for (String c : capabilities)
		{
			strCapabilities += c;
		}
		
		// Gets a List of all Lights
		for (Integer lightID : lightIDs)
		{
			lgtIds += lightID + ",";
		}
		
		// Trims the Strings to Remove the Last Comma
		strCapabilities = strCapabilities.substring(0, strCapabilities.length() - 1);
		lgtIds          = lgtIds.substring(0, lgtIds.length() - 1);
		
		// Sends the Capabilities of this Device
		this.getGroupContextManager().sendContext(this.getContextType(), new String[0],
				new String[] { "capability=" + strCapabilities, "lightIDs=" + lgtIds } );	
	}
	
	private String[] rgbToHueXY(int r, int g, int b)
	{	
		// For the hue bulb the corners of the triangle are:
	    // -Red: 0.675, 0.322
	    // -Green: 0.4091, 0.518
	    // -Blue: 0.167, 0.04
	    double[] normalizedToOne = new double[3];
	    normalizedToOne[0] = ((double)r / 255.0);
	    normalizedToOne[1] = ((double)g / 255.0);
	    normalizedToOne[2] = ((double)b / 255.0);
	    float red, green, blue;

	    // Make red more vivid
	    if (normalizedToOne[0] > 0.04045) {
	        red = (float) Math.pow((normalizedToOne[0] + 0.055) / (1.0 + 0.055), 2.4);
	    } else {
	        red = (float) (normalizedToOne[0] / 12.92);
	    }

	    // Make green more vivid
	    if (normalizedToOne[1] > 0.04045) {
	        green = (float) Math.pow((normalizedToOne[1] + 0.055) / (1.0 + 0.055), 2.4);
	    } else {
	        green = (float) (normalizedToOne[1] / 12.92);
	    }

	    // Make blue more vivid
	    if (normalizedToOne[2] > 0.04045) {
	        blue = (float) Math.pow((normalizedToOne[2] + 0.055) / (1.0 + 0.055), 2.4);
	    } else {
	        blue = (float) (normalizedToOne[2] / 12.92);
	    }

	    float X = (float) (red * 0.649926 + green * 0.103455 + blue * 0.197109);
	    float Y = (float) (red * 0.234327 + green * 0.743075 + blue * 0.022598);
	    float Z = (float) (red * 0.000000 + green * 0.053077 + blue * 1.035763);

	    String[] xy = new String[2];
	    
	    xy[0] = Float.toString(X / (X + Y + Z));
	    xy[1] = Float.toString(Y / (X + Y + Z));

//	    System.out.print("RGB [" + r + ", " + g + ", " + b + "] = ");
//	    System.out.println("XY " + xy[0] + ", " + xy[1]);
	    
	    return xy;
	}
	
	// OVERRIDDING METHODS --------------------------------------------------------------------------
	@Override
	public int getHeartbeatRate(ContextRequest request)
	{
		return 10000;
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{		
		System.out.println("Received Compute Instruction: " + instruction);
		
		if (instruction.getCommand().equals(LIGHT_STATE_COMMAND))
		{
			System.out.println("Received LIGHT STATE command");
			
			for (Integer lightID : lightIDs)
			{
				String lightCommand = "PUT";
				String lightURL     = apiURL + "lights/" + lightID + "/state";
				
				try
				{
					JSONObject jsonFactory = new JSONObject();
					
					if (instruction.getPayload().length == 4)
					{
						//Log.d("GCF-ContextProvider [HUE]", "Issuing HSB Light Command");
						jsonFactory.put("on", Boolean.parseBoolean(instruction.getPayload()[0]));
						jsonFactory.put("bri", Integer.parseInt(instruction.getPayload()[1]));
						jsonFactory.put("sat", Integer.parseInt(instruction.getPayload()[2]));
						jsonFactory.put("hue", Integer.parseInt(instruction.getPayload()[3]));
						
						// Performs the Command
						new HTTPAsyncTask().execute(lightCommand, lightURL, jsonFactory.toString());
					}
					else if (instruction.getPayload().length == 3)
					{
						//Log.d("GCF-ContextProvider [HUE]", "Issuing XY Light Command (Light " + lightID + ") [" + instruction.getParameters()[1] + ", " + instruction.getParameters()[2] + "]");						
						String json = "{\"on\":true,\"xy\":[" + instruction.getPayload()[1] + ", " + instruction.getPayload()[2] + "]}";
						//System.out.println("Sending Command: " + json);
						
						// Performs the Command
						new HTTPAsyncTask().execute(lightCommand, lightURL, json);
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		}
		else if (instruction.getCommand().equals(LIGHT_RGB_COMMAND))
		{
			System.out.println("Received LIGHT RGB command");
			
			for (Integer lightID : lightIDs)
			{
				String lightCommand = "PUT";
				String lightURL     = apiURL + "lights/" + lightID + "/state";
				
				try
				{
					if (instruction.getPayload().length == 4)
					{
						int r = Integer.parseInt(instruction.getPayload()[1]);
						int g = Integer.parseInt(instruction.getPayload()[2]);
						int b = Integer.parseInt(instruction.getPayload()[3]);
						
						// Uses a Converstion Function to Translate XY into RGB
						String[] xy = rgbToHueXY(r, g, b);
						
						//Log.d("GCF-ContextProvider [HUE]", "Issuing XY Light Command (Light " + lightID + ") [" + instruction.getParameters()[1] + ", " + instruction.getParameters()[2] + "]");						
						String json = "{\"on\":" + instruction.getPayload()[0] + ",\"xy\":[" + xy[0] + ", " + xy[1] + "]}";
						//System.out.println("Sending Command: " + json);
						
						// Performs the Command
						new HTTPAsyncTask().execute(lightCommand, lightURL, json);
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		}
		else if (instruction.getCommand().equals(LIGHT_SPECTRUM_COMMAND))
		{
			for (Integer lightID : lightIDs)
			{
				String lightCommand = "PUT";
				String lightURL     = apiURL + "lights/" + lightID + "/state";
				
				try
				{
					JSONObject jsonFactory = new JSONObject();
					jsonFactory.put("on", Boolean.parseBoolean(instruction.getPayload()[0]));
					jsonFactory.put("bri", (int)Double.parseDouble(instruction.getPayload()[1]));
					jsonFactory.put("sat", (int)Double.parseDouble(instruction.getPayload()[2]));
					jsonFactory.put("hue", (int)((1.0 - Math.random() * 0.2) * Double.parseDouble(instruction.getPayload()[3])));
					
					// Performs the Command
					new HTTPAsyncTask().execute(lightCommand, lightURL, jsonFactory.toString());
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		}
	}
}
