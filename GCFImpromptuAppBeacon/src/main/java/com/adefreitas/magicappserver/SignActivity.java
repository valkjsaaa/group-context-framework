package com.adefreitas.magicappserver;

import java.util.ArrayList;
import java.util.Date;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.gcf.android.ContextReceiver;
import com.adefreitas.gcf.android.bluewave.JSONContextParser;
import com.adefreitas.gcf.messages.ContextData;

public class SignActivity extends ActionBarActivity implements ContextReceiver
{
	// This is a Link to a the Main Application ( contains some custom methods :)
	private GCFApplication application;
	
	// Places all of Your Android Views (contained in res/layout/<ACTIVITY NAME>.xml) here
	private WebView  webView;
	
	// Thread that Auto Updates
	private AutoUpdateHandler signContentHandler;
	
	/**
	 * Android Method:  Used when the Activity is Created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sign);
		
		// Gets a Reference to the Main Application
		this.application = (GCFApplication)this.getApplication();
		
		// Gets the "Views" from the Android Activity (activity_sign.xml)
		webView = (WebView)this.findViewById(R.id.webView);
		
		signContentHandler = new AutoUpdateHandler(webView);			
	}

	/**
	 * Android Method:  Used when an Activity is Resumed
	 */
	protected void onResume()
	{
		super.onResume();
		
		// Sets Up Context Listening
		application.setContextReceiver(this);
		
		signContentHandler.start(60000);
		signContentHandler.setWebViewContent("http://gcf.cmu-tbank.com/apps/signs/default.php?deviceID=" + application.getGroupContextManager().getDeviceID().replace(" ", "%20"));
	}
	
	/**
	 * Android Method:  Used when an Activity is Paused
	 */
	@Override
	protected void onPause() 
	{
	    super.onPause();

	    application.removeContextReceiver(this);
	    
	    signContentHandler.stop();
	}
	
	/**
	 * Android Method:  Used when the Menu is Created
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sign, menu);
		return true;
	}

	/**
	 * Android Method:  Used when a Menu Item is Selected
	 * Go to res/menu to see the XML file that generates the menu for this sign
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * GCF Method:  Used when Context is Delivered to the App
	 */
	@Override
	public void onContextData(ContextData data) 
	{
		// TODO Auto-generated method stub
	}
	
	/**
	 * Bluewave Method:  Used when Bluewave Context is Detected by the App
	 */
	@Override
	public void onBluewaveContext(JSONContextParser parser) 
	{	
		Log.d("SIGN", "Sign Analyzing Bluewave Content: " + parser.getDeviceID() + " [" + parser.toString().length() + " bytes]");
		
		try
		{
			adjustSignContents(parser);
		}
		catch (Exception ex)
		{
			// Displays a Toast Notification on the Phone to Show You the Problem
			Toast.makeText(this, "Problem Analyzing Bluewave Context: " + ex.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	// CUSTOM VARIABLES
	private Date lastGooglerSeen = new Date(0);
	
	// Custom Sign Behaviors --------------------------------------------------------------------
	private void adjustSignContents(JSONContextParser parser)
	{	
		String extras = "deviceID=" + application.getGroupContextManager().getDeviceID().replace(" ", "%20") + "&";
		
		try
		{
			// Snap To It Check
			if (parser.getJSONRoot().has("snap-to-it"))
			{
				Log.d("SIGN", "Changing Contents Due to Snap-To-It");
				extras += "sti=true&";
			}	
			
			// Google IOT Check
			lastGooglerSeen = (parser.hasEmailAddress(getGoogleEmail())) ? new Date() : lastGooglerSeen;
			if (new Date().getTime() - lastGooglerSeen.getTime() < 60000)
			{
				extras += "google=true&";
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	
		String url = "http://www.gcf.cmu-tbank.com/apps/signs/iot/index.php?" + extras;
		
		System.out.println(url);
		signContentHandler.setWebViewContent(url);
	}
	
	private String[] getGoogleEmail()
	{
		return new String[] { 
				"adrian.defreitas@gmail.com", 
				"anind@cs.cmu.edu",
				"roywant@google.com",
				"maxsenges@google.com",
				"ptone@google.com",
				"ninataft@google.com",
				"walz@google.com",
				"rhk@illinois.edu",
				"shmatikov@cornell.edu",
				"lujo.bauer@gmail.com",
				"lorrie@cs.cmu.edu",
				"sadeh@cs.cmu.edu",
				"yuvraj.agarwal@gmail.com",
				"jasonhong666@gmail.com",
				"cmusatya@gmail.com",
				"anthony.rowe2@gmail.com",
				"harrisonhci@gmail.com",
				"aninddey@gmail.com",
				"gdarakos@cs.cmu.edu",
				"edge.hayashi@gmail.com",
				"awmcmu@gmail.com",
				"lyou@google.com",
				"youngjoo@google.com"
				};
	}
	
	// Timed Event ------------------------------------------------------------------------------
	/**
	 * This Class Allows the App To Update Its Context Once per Interval
	 * @author adefreit
	 */
	private static class AutoUpdateHandler extends Handler
	{
		private final   WebView webView;
		private boolean 		running;
		private int     		interval;
		private String			url;
		
		public AutoUpdateHandler(final WebView webView)
		{			
			running      = false;
			this.webView = webView;
			url	         = "";
		}
		
		private Runnable scheduledTask = new Runnable() 
		{	
			public void run() 
			{ 	
				if (isRunning())
				{
					webView.loadUrl(url);
					
					// Sleep Time Depends on Whether or Not the Application is in the Foreground
					postDelayed(this, interval);
				}
			}
		};
		
		public void start(int interval)
		{
			this.interval = interval;
			
			// Stops Any Existing Delays
			stop();
			
			// Creates the Next Task Instance
			postDelayed(scheduledTask, 100);
			
			running = true;
		}
		
		public void stop()
		{
			removeCallbacks(scheduledTask);	
			
			running = false;
		}
		
		public void setWebViewContent(String url)
		{			
			if (!this.url.equals(url) && webView != null)
			{
				webView.loadUrl(url);
			}
			
			this.url = url;
		}
		
		public boolean isRunning()
		{
			return running;
		}
	}
}
