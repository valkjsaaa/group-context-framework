package com.adefreitas.gcfimpromptu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.androidbluewave.JSONContextParser;
import com.adefreitas.androidframework.AndroidCommManager;
import com.adefreitas.androidframework.ContextReceiver;
import com.adefreitas.androidframework.toolkit.CloudStorageToolkit;
import com.adefreitas.androidframework.toolkit.HttpToolkit;
import com.adefreitas.gcfimpromptu.lists.AppInfo;
import com.adefreitas.gcfmagicapp.R;
import com.adefreitas.liveos.ApplicationFunction;
import com.adefreitas.liveos.ApplicationObject;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ContextData;
import com.adefreitas.messages.ContextRequest;
import com.google.gson.Gson;

@SuppressLint("SetJavaScriptEnabled")
public class AppEngine extends ActionBarActivity implements ContextReceiver
{
	// Constants
	private static final String LOG_NAME 				 = "AppEngine";
	private static final String ACTION_DOWNLOADED_HTML   = "DOWNLOADED_HTML";
	private static final String ACTION_DOWNLOAD_COMPLETE = "DOWNLOAD_COMPLETE";
	
	// Application Link
	private GCFApplication application;
	
	// Intent Filters
	private IntentFilter   intentFilter;
	private IntentReceiver receiver;
	
	// Javascript Support
	private JSInterface	jsInterface;
	
	// Controls
	private Menu				 menu;
	private LinearLayout		 layoutFunctions;
	private HorizontalScrollView scrollViewFunctions;
	private Toolbar     		 toolbar;
	private FrameLayout 		 webViewPlaceholder;
	private WebView     		 webView;
	private ProgressBar			 progressSpinner;
	
	// Object Serialization
	private Gson gson;
	
	// Current App/Function
	private AppInfo 					 app;
	private ArrayList<ApplicationObject> applicationObjects;
	
	// Functions
	private HashMap<String, ApplicationFunction> functionLookup;
	
	/**
	 * Constructor
	 */
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_app_engine);
		
		// Saves a Link to the Application
		application = (GCFApplication)this.getApplication();
		
		// Creates a Lookup of Buttons Mapped to Functions (Dynamically)
		functionLookup = new HashMap<String, ApplicationFunction>();
		
		// Creates Serialization Helper
		gson = new Gson();
		
		// Create Intent Filter and Receiver
		// Receivers are Set in onResume() and Removed on onPause()
		this.receiver     = new IntentReceiver();
		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(ACTION_DOWNLOADED_HTML);
		this.intentFilter.addAction(ACTION_DOWNLOAD_COMPLETE);
		this.intentFilter.addAction(UploadFileDialog.REMOTE_UPLOAD_COMPLETE);
		this.intentFilter.addAction(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED);
		this.intentFilter.addAction(AndroidCommManager.ACTION_CHANNEL_SUBSCRIBED);
		
		// Creates the Javascript Interface
		jsInterface = new JSInterface(application, this, this);
				
		// Gets Controls
		toolbar  			= (Toolbar)this.findViewById(R.id.toolbar);
		layoutFunctions     = (LinearLayout)this.findViewById(R.id.layoutFunctions);
		scrollViewFunctions = (HorizontalScrollView)this.findViewById(R.id.scrollViewFunctions);
		webViewPlaceholder  = (FrameLayout)this.findViewById(R.id.webViewPlaceholder);
		progressSpinner     = (ProgressBar)this.findViewById(R.id.progressBar);
		progressSpinner.getIndeterminateDrawable().setColorFilter(0xFF996633, android.graphics.PorterDuff.Mode.MULTIPLY);
		
		// Sets Up the Toolbar
		this.setSupportActionBar(toolbar);
		this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Initializes the Webview
		webView = new WebView(this);
		webView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		webView.getSettings().setLoadsImagesAutomatically(true);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setDomStorageEnabled(true);
		webView.getSettings().setAllowFileAccess(true);
		webView.addJavascriptInterface(jsInterface, JSInterface.JAVASCRIPT_OBJECT_NAME);
		webView.setWebViewClient(new CustomBrowser());	

		//setupWebView();
		
		webViewPlaceholder.addView(webView);
		
		// Restores the App State so Long as the App is Running!
		if (savedInstanceState != null)
		{
			webView.restoreState(savedInstanceState);	
			progressSpinner.setVisibility(View.GONE);
		}
	}

	/**
	 * Android Method:  Used when an Activity is Resumed
	 */
	protected void onResume()
	{
		super.onResume();
		this.registerReceiver(receiver, intentFilter);
		
		// Notifies the Application to Forward GCF Message to this View
		this.application.setContextReceiver(this);
		this.application.setInForeground(true);
		
		// Gets the App from the Intent
		Intent intent = this.getIntent();
		app 		  = application.getApplicationFromCatalog(intent.getStringExtra("APP_ID"));
		
		if (app != null)
		{
			application.addActiveApplication(app);
			
			if (app.getUI() == null)
			{				
				// Sets Up Communications Channel
				//if (!application.getGroupContextManager().isConnected(app.getCommMode(), app.getIPAddress(), app.getPort()))
				{
					String connectionKey = application.getGroupContextManager().connect(app.getCommMode(), app.getIPAddress(), app.getPort());
					application.getGroupContextManager().subscribe(connectionKey, app.getChannel());
					
					// Remembers the Communications Channel Associated with this Key!
					if (!app.getConnections().contains(connectionKey))
					{
						app.getConnections().add(connectionKey);				
					}
				}
			}
		}
		else
		{
			this.finish();
		}
	}
	
	/**
	 * Android Method:  Used when an Activity is Paused
	 */
	@Override
	protected void onPause() 
	{
	    super.onPause();
	    this.unregisterReceiver(receiver);
	    this.application.setContextReceiver(null);
	    this.application.setInForeground(false);
	}
	
	/** 
	 * Android Method:  Used when an Activity is Destroyed
	 */
	protected void onDestroy()
	{
		super.onDestroy();
		this.application.setContextReceiver(null);
		this.application.clearSnapToItHistory();
		webView.destroy();
	}
	
	/**
	 * Android Method:  Used when the Options Menu is Created
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Saves the Menu
		this.menu = menu;
		
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.app_engine, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	/**
	 * Android Method:  Used when an Options Menu Item is Selected
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		if (item.toString().equalsIgnoreCase("settings"))
	    {
	    	Intent intent = new Intent(this, SettingsActivity.class);
	    	this.startActivity(intent);
	    }
		else if (item.toString().equalsIgnoreCase("refresh"))
		{
			if (app != null && app.getUI() != null)
			{
				this.render(app.getUI(), true);	
			}
			else
			{
				Toast.makeText(application, "Cannot refresh app.", Toast.LENGTH_SHORT).show();
			}
		}
	    
	    return false;
	}
	
	/**
	 * Android Method:  Used to Save the Activity's State
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		webView.saveState(outState);
	}

	/**
	 * Android Method:  Used to Restore the Activity's State
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	// GCF Methods --------------------------------------------------------------------------
	public void onContextData(ContextData data)
	{		
		if (data.getContextType().equals(app.getAppContextType()))
		{			
			// Renders the WEBSITE
			render(data, false);
		}
	}
	
	public void onGCFOutput(String output)
	{

	}
	
	@Override
	public void onBluewaveContext(JSONContextParser parser)
	{

	}
	
	public AppInfo getRunningApp()
	{
		return app;
	}
	
	// EXPERIMENTAL METHODS -----------------------------------------------------------------
	private void callJavascriptFunction(String call)
	{
		webView.loadUrl("javascript:" + call);
	}
	
	private void processObjects(String objectJSON)
	{
		// Cleans Up All Existing Functions
		layoutFunctions.removeAllViews();
		functionLookup.clear();
		
		// Stores all Application Objects for this Application ONLY!
		this.applicationObjects = new ArrayList<ApplicationObject>();
				
		// Converts JSON Into Application Objects
		try
		{
			JSONArray objects = new JSONArray(objectJSON);
			
			for (int i=0; i<objects.length(); i++)
			{
				// Gets Each Object Element and Converts It to an 
				JSONObject 	  	  objectElement = (JSONObject)objects.get(i);
				ApplicationObject obj 			= gson.fromJson(objectElement.toString(), ApplicationObject.class);
				
				if (obj != null)
				{
					applicationObjects.add(obj);
				}
			}
		}
		catch (Exception ex)
		{
			Toast.makeText(application, "Error Parsing Objects: " + ex.getMessage(), Toast.LENGTH_LONG).show();
			ex.printStackTrace();
		}
		
		// Looks for Compatible Functions
		for (AppInfo app : application.getAvailableApps())
		{
			for (ApplicationFunction f : app.getFunctions())
			{
				if (f.isCompatible(applicationObjects.toArray(new ApplicationObject[0])))
				{
					// Creates and Adds a Button
					Button bt = new Button(this);
					bt.setText(f.getName());
					bt.setTextSize(10.0f);
					bt.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
					bt.setOnClickListener(onFunctionClickListener);
					layoutFunctions.addView(bt);
					
					// Adds the Button to the Lookup
					functionLookup.put(bt.getText().toString(), f);
				}
			}
		}
		
		// Only Shows the Functions Layout if at least One function is Visible! 
		if (layoutFunctions.getChildCount() > 0)
		{
			scrollViewFunctions.setVisibility(View.VISIBLE);
		}
		else
		{
			scrollViewFunctions.setVisibility(View.GONE);
		}
	}
	
	private String readHtml(String remoteUrl) {
	    String out = "";
	    BufferedReader in = null;
	    try {
	        URL url = new URL(remoteUrl);
	        in = new BufferedReader(new InputStreamReader(url.openStream()));
	        String str;
	        while ((str = in.readLine()) != null) {
	            out += str;
	        }
	    } catch (MalformedURLException e) 
	    {
	    	System.out.println("Malformed URL Exception");
	    } 
	    catch (IOException e) 
	    { 
	    	System.out.println("IO Exception");
	    } 
	    finally {
	        if (in != null) {
	            try {
	                in.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }
	    
	    // Step 2:  Add Local References
	    // http://stackoverflow.com/questions/9414312/android-webview-javascript-from-assets
	    //out = out.replace("phaser.min.js", "file:///android_asset/phaser.min.js");
	    
	    Log.d("HTML", out);
	    
	    return out;
	}

	private boolean appInstalled(String uri) 
	{
        PackageManager pm = getPackageManager();
        boolean app_installed = false;
        try 
        {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e) 
        {
            app_installed = false;
        }
        return app_installed ;
    }
	
	 public Uri imageUri;
     
	    private static final int FILECHOOSER_RESULTCODE   = 2888;
	    private ValueCallback<Uri> mUploadMessage;
	    private Uri mCapturedImageURI = null;
	
	/**
	 * Experimental:  Enabling Camera Code!
	 */
	private void setupWebView()
	{
		webView.setWebViewClient(new WebViewClient() {      
            ProgressDialog progressDialog;
          
            //If you will not use this method url links are open in new brower not in webview
            public boolean shouldOverrideUrlLoading(WebView view, String url) {              
                 
                // Check if Url contains ExternalLinks string in url 
                // then open url in new browser
                // else all webview links will open in webview browser
                if(url.contains("google")){ 
                     
                    // Could be cleverer and use a regex
                    //Open links in new browser
                    view.getContext().startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                     
                    // Here we can open new activity
                     
                    return true;
                     
                } else {
                     
                    // Stay within this webview and load url
                    view.loadUrl(url); 
                    return true;
                }
                   
            }
             
            //Show loader on url load
            public void onLoadResource (WebView view, String url) {
             
                // if url contains string androidexample
                // Then show progress  Dialog
                if (progressDialog == null && url.contains("androidexample") 
                        ) {
                     
                    // in standard case YourActivity.this
                    progressDialog = new ProgressDialog(AppEngine.this);
                    progressDialog.setMessage("Loading...");
                    progressDialog.show();
                }
            }
             
            // Called when all page resources loaded
            public void onPageFinished(WebView view, String url) {
                 
                try{
                    // Close progressDialog
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                }catch(Exception exception){
                    exception.printStackTrace();
                }
            }
            
        }); 
		
		webView.setWebChromeClient(new WebChromeClient() {
            
           // openFileChooser for Android 3.0+
           public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType){  
               
               // Update message
               mUploadMessage = uploadMsg;
                
               try{    
                
                   // Create AndroidExampleFolder at sdcard
                    
                   File imageStorageDir = new File(
                                          Environment.getExternalStoragePublicDirectory(
                                          Environment.DIRECTORY_PICTURES)
                                          , "Impromptu");
                                           
                   if (!imageStorageDir.exists()) {
                       // Create AndroidExampleFolder at sdcard
                       imageStorageDir.mkdirs();
                   }
                    
                   // Create camera captured image file path and name 
                   File file = new File(
                                   imageStorageDir + File.separator + "IMG_"
                                   + String.valueOf(System.currentTimeMillis()) 
                                   + ".jpg");
                                    
                   mCapturedImageURI = Uri.fromFile(file); 
                    
                   // Camera capture image intent
                   final Intent captureIntent = new Intent(
                                                 android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                                                  
                   captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
                   
                   Intent i = new Intent(Intent.ACTION_GET_CONTENT); 
                   i.addCategory(Intent.CATEGORY_OPENABLE);
                   i.setType("image/*");
                    
                   // Create file chooser intent
                   Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
                    
                   // Set camera intent to file chooser 
                   chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                                          , new Parcelable[] { captureIntent });
                    
                   // On select image call onActivityResult method of activity
                   startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
                    
                 }
                catch(Exception e){
                    Toast.makeText(getBaseContext(), "Exception:"+e, 
                               Toast.LENGTH_LONG).show();
                }
                
           }
            
           // openFileChooser for Android < 3.0
           public void openFileChooser(ValueCallback<Uri> uploadMsg){
               openFileChooser(uploadMsg, "");
           }
            
           //openFileChooser for other Android versions
           public void openFileChooser(ValueCallback<Uri> uploadMsg, 
                                      String acceptType, 
                                      String capture) {
                                       
               openFileChooser(uploadMsg, acceptType);
           }



           // The webPage has 2 filechoosers and will send a 
           // console message informing what action to perform, 
           // taking a photo or updating the file
            
           public boolean onConsoleMessage(ConsoleMessage cm) {  
                  
               onConsoleMessage(cm.message(), cm.lineNumber(), cm.sourceId());
               return true;
           }
            
           public void onConsoleMessage(String message, int lineNumber, String sourceID) {
               //Log.d("androidruntime", "Show console messages, Used for debugging: " + message);
                
           }
       });   // End setWebChromeClient
	}
		
	public void verifyFunctionExecution(final ApplicationFunction function, final ArrayList<ApplicationObject> applicationObjects)
	{	
		// Creates a Confirmation Dialog if there Are Contexts
		AlertDialog.Builder alert = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
		
		final FrameLayout frameView = new FrameLayout(this);
		alert.setView(frameView);
		
		final AlertDialog alertDialog = alert.create();
		LayoutInflater inflater = alertDialog.getLayoutInflater();
		View dialoglayout = inflater.inflate(R.layout.dialog_function_alert, frameView);

		// Grabs Controls
		TextView 	 txtFunctionName 	    = (TextView)dialoglayout.findViewById(R.id.txtFunctionName);
		TextView 	 txtFunctionDescription = (TextView)dialoglayout.findViewById(R.id.txtFunctionDescription);
		LinearLayout btnRun			   		= (LinearLayout)dialoglayout.findViewById(R.id.btnRun);
		
		txtFunctionName.setText(function.getName());
		txtFunctionDescription.setText(function.getDescription());
				
		final OnClickListener onButtonRunPressed = new OnClickListener() 
		{
			@Override
	        public void onClick(final View v)
	        {
	    		alertDialog.dismiss(); 
	    		
	    		for (AppInfo app : application.getAvailableApps())
	    		{
	    			for (ApplicationFunction f : app.getFunctions())
	    			{
	    				if (f.getName().equals(function.getName()))
	    				{
	    					// Opens a Temporary Connection to this Device
		    				String socketID = application.getGroupContextManager().connect(app.getCommMode(), app.getIPAddress(), app.getPort());
		    				
		    				// Determine what Object to send
		    				
		    				
		    				// Sends the Command
		    				System.out.println("Sending Information to ");
		    				application.getGroupContextManager().sendComputeInstruction(socketID, app.getChannel(), app.getAppContextType(), new String[] { app.getDeviceID() }, function.getCallbackCommand(), new String[0]);
		    				break;
	    				}
	    			}
	    		}
	    		
	    		// Remembers the Function
	    		//AppEngine.this.function = function;
	    		
	    		// Tries to Connect to it
	    		//String channelID = application.getGroupContextManager().connect(function.getCommMode(), function.getIpAddress(), function.getPort());
	    		//application.getGroupContextManager().sendComputeInstruction(channelIDcontextType, function.get, command, instructions)
	    		//application.getGroupContextManager().subscribe(channelID, function.getChannel());
	        }
	    };
	    
	    final OnDismissListener onDialogDismissed = new OnDismissListener()
	    {
			@Override
			public void onDismiss(DialogInterface dialog)
			{

			}
	    };
	    
	    // Sets Events
	    btnRun.setOnClickListener(onButtonRunPressed);
	    alertDialog.setOnDismissListener(onDialogDismissed);
		
	    // Shows the Finished Dialog
		alertDialog.show();	
	}
	
	// Event Handlers -----------------------------------------------------------------------
	final OnClickListener onFunctionClickListener = new OnClickListener() 
    {
		@Override
		public void onClick(View v) 
		{
		  	Button button = (Button)v;
		  	
		  	ApplicationFunction function = functionLookup.get(button.getText().toString());
		  	
		  	if (function != null)
		  	{
		  		//Toast.makeText(application, "Activated Function: " + function.getName(), Toast.LENGTH_SHORT).show();
		  		verifyFunctionExecution(function, applicationObjects);
		  	}
		  	else
		  	{
		  		Toast.makeText(application, "ERROR:  Try Again", Toast.LENGTH_SHORT).show();
		  	}
		}
    };
	
	// Renders User Interface ---------------------------------------------------------------
	private void render(ContextData data, boolean force)
	{		
		// Erases the Cache
		webView.clearCache(true);
		
		// Extracts the NEW Interface Values
		final String userInterface  = data.getPayload("UI");
		final String websitePath    = data.getPayload("WEBSITE");
		final String packageName    = data.getPayload("PACKAGE");
		
		// Extracts EXTRA Values 
		final String objectJSON     = data.getPayload("OBJECTS");
		final String javascriptCall = data.getPayload("JAVASCRIPT");
		final String forceRender    = data.getPayload("FORCE");
		
		// Updates the Force Update Button
		if (forceRender != null)
		{
			force = (forceRender.equalsIgnoreCase("TRUE") || forceRender.equalsIgnoreCase("T") || forceRender.equalsIgnoreCase("1"));
		}
		
		// Determines the OLD Interface Values
		String previousUI = "";
		if (app.getUI() != null)
		{
			String previousHtml    = app.getUI().getPayload("UI");
			String previousWebsite = app.getUI().getPayload("WEBSITE");
			String previousPackage = app.getUI().getPayload("PACKAGE");
			
			if (previousHtml != null)
			{
				previousUI = previousHtml;
			}
			else if (previousWebsite != null)
			{
				previousUI = previousWebsite;
			}
			else if (previousPackage != null)
			{
				previousUI = previousPackage;
			}
		}
		
		//Log.d(LOG_NAME, "PREVIOUS: " + previousUI);
		//Log.d(LOG_NAME, "NEW:      " + userInterface);
		
		// Only Touches the UI if the UI has Actually Changed
		if (userInterface != null && (!userInterface.equals(previousUI) || force))
		{
			webView.loadData(userInterface, "text/html", "UTF-8");
			app.setUI(data);
			
			Log.d(LOG_NAME, "Rendering HTML (" + userInterface.length() + " bytes)");
		}
		else if (websitePath != null &&  (!websitePath.equals(previousUI) || force))
		{
			webView.loadUrl(websitePath);
			app.setUI(data);
				
			Log.d(LOG_NAME, "Rendering URL");
		}
		else if (packageName != null && (!packageName.equals(previousUI) || force))
		{
			app.setUI(data);
			
			// Creates an Empty Intent
			Intent intent;
			
			if (appInstalled(packageName))
			{
				intent = getPackageManager().getLaunchIntentForPackage(packageName);
				Log.d(LOG_NAME, "Running Installed Package: " + packageName);
			}
			else
			{
				intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("market://details?id=" + packageName));
				Log.d(LOG_NAME, "Linking to Store: " + packageName);
			}
			
			// Starting the Intent
			startActivity(intent);
			
			// Kills this Activity Since we Don't Need it Anymore!
			finish();
		}
		
		// EXPERIMENTAL Processes Objects
		if (objectJSON != null)
		{
			processObjects(objectJSON);
		}
		
		// EXPERIMENTAL Processes JavaScript Calls
		if (javascriptCall != null)
		{
			callJavascriptFunction(javascriptCall);
		}
		
		// Removes the Progress Bar
		this.progressSpinner.setVisibility(View.GONE);
	}
		
	// Intent Receiver ----------------------------------------------------------------------
	private class IntentReceiver extends BroadcastReceiver
	{
		/**
		 * This is the method that is called when an intent is received
		 */
		@Override
		public void onReceive(Context context, Intent intent) 
		{				
			if (intent.getAction().equals(ACTION_DOWNLOAD_COMPLETE))
			{
				onDownloadComplete(context, intent);
			}
			else if (intent.getAction().equals(UploadFileDialog.REMOTE_UPLOAD_COMPLETE))
			{	
				onUploadComplete(context, intent);
			}
			else if (intent.getAction().equals(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED))
			{
				onConnected(context, intent);
			}
			else if (intent.getAction().equals(AndroidCommManager.ACTION_CHANNEL_SUBSCRIBED))
			{
				onSubscribed(context, intent);
			}
			else if (intent.getAction().equals(ACTION_DOWNLOADED_HTML))
			{
				onHTMLDownloadComplete(context, intent);
			}
			else
			{
				Log.e(LOG_NAME, "Unexpected Intent (Action: " + intent.getAction() + ")");	
			}
		}
		
		private void onConnected(Context context, Intent intent)
		{
			progressSpinner.getIndeterminateDrawable().setColorFilter(0xFF339966, android.graphics.PorterDuff.Mode.MULTIPLY);
		}
		
		private void onSubscribed(Context context, Intent intent)
		{
			progressSpinner.getIndeterminateDrawable().setColorFilter(0xFF0186D5, android.graphics.PorterDuff.Mode.MULTIPLY);
			
			String channel = intent.getStringExtra("CHANNEL");
			
			// Determines What Channel We Have Subscribed To
			if (channel != null && app != null && channel.equals(app.getChannel()))
			{			
				// Grabs All Preferences Requested by the Application
				String preferences = "";
				
				// Adds Application Specific Preferences
				for (String preferenceKey : app.getPreferences())
				{
					String preference = application.getPreference(preferenceKey);
					
					if (preference != null)
					{
						preferences += preferenceKey + "=" + preference + ",";
					}
				}
				
				// Sends the Request for Contextual Information
				application.getGroupContextManager().sendRequest(app.getAppContextType(), ContextRequest.SINGLE_SOURCE, new String[] { app.getDeviceID() }, 60000, 
					new String[] { 
						"credentials=" + application.getGroupContextManager().getDeviceID(),
						"preferences=" + ((preferences.length() > 0) ? preferences.substring(0, preferences.length()-1) : ""),
						"context="     + application.getBluewaveManager().getPersonalContextProvider().getContext().toString()
					});	
			}
		}
		
		private void onDownloadComplete(Context context, Intent intent)
		{
			String downloadPath = intent.getStringExtra(HttpToolkit.HTTP_RESPONSE);
			
			if (downloadPath != null)
			{
				Toast.makeText(application, "Download Complete (" + downloadPath + ")", Toast.LENGTH_SHORT).show();	
			}
		}
		
		private void onUploadComplete(Context context, Intent intent)
		{
			// This is the FULL link to the file being uploaded
			String sourcePath = intent.getStringExtra(CloudStorageToolkit.CLOUD_UPLOAD_SOURCE);
			String filename   = sourcePath.substring(sourcePath.lastIndexOf("/") + 1);
			
			// This is the Link to the CLOUD LOCATION where the above file was uploaded
			String destinationFolder = intent.getStringExtra(CloudStorageToolkit.CLOUD_UPLOAD_PATH);
						
			if (application != null)
			{
				// This retrieves any commands associated with the uploaded file folder
				String callbackCommand = jsInterface.getUploadCallbackCommand();
				
				Toast.makeText(application, "File Uploaded: " + destinationFolder + "; UPLOAD COMMAND = " + callbackCommand, Toast.LENGTH_SHORT).show();
				
				// Sends a Callback (if one is specified)
				if (callbackCommand != null && callbackCommand.length() > 0)
				{
					application.getGroupContextManager().sendComputeInstruction(app.getAppContextType(), callbackCommand, 
							new String[] { "uploadPath=" + GCFApplication.UPLOAD_WEB_PATH + filename});
				}
			}
		}
		
		private void onHTMLDownloadComplete(Context context, Intent intent)
		{
			String html = intent.getStringExtra("HTML");
			webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", "");
		}
	}
	
	// Custom Browser Class -----------------------------------------------------------------
	private class CustomBrowser extends WebViewClient 
	{
	   @Override
	   public boolean shouldOverrideUrlLoading(WebView view, String url) 
	   {
		  view.loadUrl(url);
	      return true;
	   }
	
	   @Override
       public void onPageFinished(WebView view, String url) 
	   {
           toolbar.setTitle(view.getTitle());
       }
	}
	
}
