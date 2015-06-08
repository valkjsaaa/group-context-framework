package impromptu_apps;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;


import com.adefreitas.desktopframework.toolkit.HttpToolkit;
import com.adefreitas.desktopframework.toolkit.JSONContextParser;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class App_Weather extends DesktopApplicationProvider
{	
	// Stores Coordinates
	private HashMap<String, Point2D.Double> coordinates     = new HashMap<String, Point2D.Double>();
	private HashMap<String, String>         history		    = new HashMap<String, String>();
	private ArrayList<String>	  	        devicesAccessed = new ArrayList<String>();
	
	// Stores the Current Calendar Day
	private int day;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 * @param commMode
	 * @param ipAddress
	 * @param port
	 */
	public App_Weather(GroupContextManager groupContextManager, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"PNP_WEATHER",
				"Weather App",
				"You don't care about the temperature.",
				"WEATHER",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"http://www.iconpng.com/png/stylish_weather/sun.small.cloud.dark.png",				   // LOGO
				120,
				commMode,
				ipAddress,
				port);
		
		// Saves the Current Day
		Calendar calendar = Calendar.getInstance();
		day = calendar.get(Calendar.DAY_OF_MONTH);
		
		// Populates coordinates
		// NOTE:  I am using X, Y for Longitude, Latitude (the reverse of what you normally think)
		coordinates.put("15221", new Point2D.Double(40.4338964, -79.8537098));
	}
	
	@Override
	public void onSubscription(ContextSubscriptionInfo newSubscription)
	{
		super.onSubscription(newSubscription);
		
		if (!devicesAccessed.contains(newSubscription.getDeviceID()))
		{
			devicesAccessed.add(newSubscription.getDeviceID());
		}
	}
	
	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{
		String  		  context 		  = CommMessage.getValue(subscription.getParameters(), "context");
		JSONContextParser parser  		  = new JSONContextParser(JSONContextParser.JSON_TEXT, context);
		
		String html = "<html><title>Simple Weather App</title><img src=\"http://l.yimg.com/a/i/us/we/52/33.gif\"/>" +
				"<br /> " +
				"<b>Current Conditions:</b><br /> " +
				"Fair, 71 F<BR /> <BR /><b>Forecast:</b><BR /> Wed - Thunderstorms. High: 85 Low: 62<br /> " +
				"Thu - Partly Cloudy. High: 85 Low: 61<br /> Fri - PM Thunderstorms. High: 89 Low: 66<br /> " +
				"Sat - Scattered Thunderstorms. High: 88 Low: 59<br /> Sun - Showers. High: 67 Low: 56<br /> <br /> " +
				"<a href=\"http://us.rd.yahoo.com/dailynews/rss/weather/Pittsburgh__PA/*http://weather.yahoo.com/forecast/USPA1290_f.html\">Full Forecast at Yahoo! Weather</a><BR/><BR/> (provided by <a href=\"http://www.weather.com\" >The Weather Channel</a>)<br/></html>";
				
		return new String[] { "WEBSITE=" + "http://weather.yahoo.com/forecast/USPA1290_f.html" };
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}
	
	@Override
	public boolean sendAppData(String json)
	{
		String bestZipcode  = "";
		double bestDistance = Double.MAX_VALUE;
		
		JSONContextParser parser   = new JSONContextParser(JSONContextParser.JSON_TEXT, json);
		String 			  deviceID = this.getDeviceID(parser);
		
		// Calculates the Best WOEID (Used by Yahoo)
		for (String locationName : coordinates.keySet())
		{
			Point2D.Double location = coordinates.get(locationName);
			double 		   distance = this.getDistance(new JSONContextParser(JSONContextParser.JSON_TEXT, json), location.x, location.y);
			
			if (distance < bestDistance)
			{
				bestZipcode  = locationName;
				bestDistance = distance;
			}
		}
		
		getForecastAdvice(bestZipcode);

		// Gets the Current Date
		Calendar calendar = Calendar.getInstance();
		
		// Performs Maintenance Tasks ONCE Per Day!
		if (day != calendar.get(Calendar.DAY_OF_MONTH))
		{
			day = calendar.get(Calendar.DAY_OF_MONTH);
			devicesAccessed.clear();
			history.clear();
		}
		
		// Only Lets you See the App ONCE per Day!
		//f (this.hasEmailAddress(parser, "adrian.defreitas@gmail.com"))
		if (calendar.get(Calendar.HOUR_OF_DAY) >= 6 && calendar.get(Calendar.HOUR_OF_DAY) <= 9 && !devicesAccessed.contains(deviceID))
		{
			return true;
		}

		return false;
	}

	public String getDescription(String userContextJSON)
	{
		JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, userContextJSON);
		return getForecastAdvice(getZipcode(parser));
	}
	
	// YAHOO API METHODS ------------------------------------------------------------------------------------------
	
	// A Woeid is Yahoo's CRAZY way of associating weather
	private String getZipcode(JSONContextParser parser)
	{
		String bestName     = "";
		double bestDistance = Double.MAX_VALUE;
		
		for (String locationName : coordinates.keySet())
		{
			Point2D.Double location = coordinates.get(locationName);
			double 		   distance = this.getDistance(parser, location.y, location.x);
			
			if (distance < bestDistance)
			{
				bestName     = locationName;
				bestDistance = distance;
			}
		}
		
		return bestName;
	}

	private String getForecastAdvice(String zipcode)
	{
		if (!history.containsKey(zipcode))
		{
			String weatherJSON = HttpToolkit.get("http://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20location%3D%22" + 15221 + "%22&format=json");
			
			JSONContextParser parser = new JSONContextParser(JSONContextParser.JSON_TEXT, weatherJSON);
			
			System.out.print("DOWNLOADING ");
			
			String forecast = "";
			
			try
			{
				JsonObject forecastJSON = parser.getJSONObject("query").getAsJsonObject("results").getAsJsonObject("channel").getAsJsonObject("item").getAsJsonArray("forecast").get(0).getAsJsonObject();
				
				int    high = forecastJSON.get("high").getAsInt();
				int    low  = forecastJSON.get("low").getAsInt();
				String text = forecastJSON.get("text").getAsString();
				
				String recommendedTop    = "";
				String recommendedBottom = "";
				
				if (low < 60)
				{
					recommendedBottom = "wear pants";
				}
				else
				{
					recommendedBottom = "wear shorts";
				}
				
				if (low < 70)
				{
					if (high > 80)
					{
						recommendedTop = "bring a jacket";
					}
					else
					{
						recommendedTop = "wear a long sleeve shirt";
					}
				}
				else
				{
					recommendedTop = "wear a short sleeve shirt";
				}
				
				forecast = (text + " [" + low + "F -> " + high + "F]" + "\n\nSuggest that you " + recommendedTop + " and " + recommendedBottom);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				forecast = ex.getMessage();
			}
			
			// Downloads the Forecast
			history.put(zipcode, forecast);
		}
		else
		{
			System.out.print("USING CACHED VALUE ");
		}
		
		// TODO:  Process the Forecast
		return history.get(zipcode);
	}
 
}
