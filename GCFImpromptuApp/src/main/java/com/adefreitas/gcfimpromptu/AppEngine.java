package com.adefreitas.gcfimpromptu;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.gcf.android.*;
import com.adefreitas.gcf.android.bluewave.*;
import com.adefreitas.gcf.android.toolkit.*;
import com.adefreitas.gcf.impromptu.ApplicationFunction;
import com.adefreitas.gcf.impromptu.ApplicationObject;
import com.adefreitas.gcf.messages.ContextData;
import com.adefreitas.gcf.messages.ContextRequest;
import com.adefreitas.gcfimpromptu.lists.AppInfo;
import com.adefreitas.gcfmagicapp.R;
import com.google.gson.Gson;

@SuppressLint("SetJavaScriptEnabled")
public class AppEngine extends ActionBarActivity implements ContextReceiver
{
	// Constants
	private static final String LOG_NAME 				 = "AppEngine";
	private static final String ACTION_DOWNLOADED_HTML   = "DOWNLOADED_HTML";
	private static final String ACTION_DOWNLOAD_COMPLETE = "DOWNLOAD_COMPLETE";
	public  static final String REMOTE_UPLOAD_COMPLETE   = "APP_UPLOAD";
	
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
	
	// Web Chrome Client Values
    public  static final int     FILECHOOSER_RESULTCODE = 12345;
    private static final String  SS_FILE_PATH_CALLBACK  = "FILE_PATH_CALLBACK";
    private static final String  SS_UPLOAD_MESSAGE      = "UPLOAD_MESSAGE";
    private static final String  SS_CAMERA_PHOTO_PATH   = "CAMERA_PHOTO_PATH";
    private static final String  SS_IMAGE_URI           = "IMAGE_URI";
    	
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
		this.intentFilter.addAction(REMOTE_UPLOAD_COMPLETE);
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
		webView.setWebChromeClient(new CustomChromeClient());
		webViewPlaceholder.addView(webView);
		webView.getSettings().setGeolocationDatabasePath(application.getFilesDir().getPath() );
	
		// Restores the App State so Long as the App is Running!
		if (savedInstanceState != null)
		{
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
	    this.unregisterReceiver(receiver);
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
		if (item.toString().equalsIgnoreCase(this.getString(R.string.title_activity_settings)))
	    {
	    	Intent intent = new Intent(this, SettingsActivity.class);
	    	this.startActivity(intent);
	    }
		else if (item.toString().equalsIgnoreCase(this.getString(R.string.title_activity_quit)))
		{
			finish();
			
			// Kills the Timer Thread
			application.halt();
			
			// Creates an Intent to Kill the GCF Service
			Intent intent = new Intent(this, GCFService.class);
			this.stopService(intent);
			android.os.Process.killProcess(android.os.Process.myPid());
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
		else if (item.toString().equalsIgnoreCase(this.getString(R.string.title_debug)))
		{
			Intent intent = new Intent(this, DebugActivity.class);
			this.startActivity(intent);
		}
		
	    
	    return false;
	}
	
	/**
	 * Android Method:  Used to Save the Activity's State
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		//Toast.makeText(this, "Save Instance State Called", Toast.LENGTH_SHORT).show();
		super.onSaveInstanceState(outState);
		outState.putString(SS_CAMERA_PHOTO_PATH, application.mCameraPhotoPath);
		webView.saveState(outState);
	}
	
	/**
	 * Android Method:  Used to Restore the Activity's State
	 */
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		application.mCameraPhotoPath = savedInstanceState.getString(SS_CAMERA_PHOTO_PATH);
		webView.restoreState(savedInstanceState);
		
	}
	
	/**
	 * Android Method:  Called when the User Presses the Back Button
	 * This allows us to go back in the webview, as opposed to (only) the app
	 */
    @Override
    // Detect when the back button is pressed
    public void onBackPressed() 
    { 
        if(webView.canGoBack())
        { 
            webView.goBack();    
        } 
        else 
        {
            // Let the system handle the back button
            super.onBackPressed();
        }
    }
	
    /**
     * Android Method:  Called when an External Process (File Selector, Camera) Comes Back
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
    	if (Build.VERSION.SDK_INT >= 21)
    	{
    		if (requestCode == FILECHOOSER_RESULTCODE && application.mFilePathCallback != null)
    		{
    			Uri[] results = null;

                // Check that the response is a good one
                if (resultCode == Activity.RESULT_OK) 
                {
                	results = new Uri[] { Uri.parse(application.mCameraPhotoPath) };
                    application.mFilePathCallback.onReceiveValue(results);
                    application.mFilePathCallback = null;
                }
                
                return;
    		}
    		else if (requestCode == FILECHOOSER_RESULTCODE)
    		{
    			// Check that the response is a good one
                if (resultCode == Activity.RESULT_OK) 
                {
                	String filePath = FileHelper.getPath(this, intent.getData());
                	
                	if (filePath != null)
                	{
                		File file = new File(filePath);
                		
                		if (file.exists())
                    	{
                    		ContentResolver cr   = application.getContentResolver();
                        	String 			mime = cr.getType(intent.getData());
                        	                    	
                        	try 
                        	{
                        		Toast.makeText(application, "Uploading " + file.getName(), Toast.LENGTH_SHORT).show();
                        		
                        		byte[] bytes = new byte[(int)file.length()];
                        	    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                        	    buf.read(bytes, 0, bytes.length);
                        	    buf.close();
    	                    	encodeFile(file.getName(), bytes);
                        	} 
                        	catch (Exception ex)
                        	{
                        		Toast.makeText(application, "Upload Failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                        	}
                    	}
                	}
                	else
                	{
                		Toast.makeText(application, "File Not Downloaded: " + intent.getData(), Toast.LENGTH_LONG).show();
                	}
                	
//                	// Only Uploads if the File is Valid
//                    if (file.exists())
//                    {
//                    	Toast.makeText(application, intent.getData().getPath(), Toast.LENGTH_SHORT).show();
//                    	
//                    	ContentResolver cr   = application.getContentResolver();
//                    	String 			mime = cr.getType(intent.getData());
//                    	
//                    	Toast.makeText(application, "Uploading " + file.getName(), Toast.LENGTH_SHORT).show();
//                    	
//                    	try 
//                    	{
//                    		byte[] bytes = new byte[(int)file.length()];
//                    	    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
//                    	    buf.read(bytes, 0, bytes.length);
//                    	    buf.close();
//	                    	encodeFile(file.getName(), bytes);
//                    	} 
//                    	catch (Exception ex)
//                    	{
//                    	    // TODO Auto-generated catch block
//                    	    ex.printStackTrace();
//                    	}
//                    }
//                    else
//                    {
//                    	Toast.makeText(application, intent.getData().getPath() + " is not downloaded on the phone.", Toast.LENGTH_LONG).show();
//                    }
                }

                return;
    		}
    		else
            {
            	Toast.makeText(this, "Unexpected Activity Result: " + requestCode, Toast.LENGTH_SHORT).show();
                super.onActivityResult(requestCode, resultCode, intent);
                return;
            }
        }
    	else
    	{
              if(requestCode==FILECHOOSER_RESULTCODE)
              {  
                  if (application.mUploadMessage == null) 
                  {
                	  return;
                  }

                    Uri result;
                    if (resultCode != RESULT_OK) 
                    {
                        result = null;
                    } 
                    else 
                    {
                        result = (intent == null) ? application.imageUri : intent.getData(); // retrieve from the private variable if the intent is null
                    }

                    application.mUploadMessage.onReceiveValue(result);
                    application.mUploadMessage = null;
              } 
        }
        
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
				Toast.makeText(application, "Looking at function: " + f.getName() + " from " + app.getName(), Toast.LENGTH_SHORT).show();
				
				if (f.isCompatible(applicationObjects.toArray(new ApplicationObject[0])))
				{
					// Creates and Adds a Button
					Button bt = new Button(this);
					bt.setText(f.getName());
					bt.setTextSize(10.0f);
					bt.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
					bt.setOnClickListener(onFunctionClickListener);
					bt.setBackgroundColor(0xFF333333);
					bt.setTextColor(0xFFFFFFFF);
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
	    		
	    		boolean found = false;
	    		
	    		for (AppInfo app : application.getAvailableApps())
	    		{
					//TODO: is it right?
//	    			if (function.getParentAppID().equals(app.getID()))
					if (function.getName().equals(app.getID()))
	    			{
	    				// Opens a Temporary Connection to this Device
	    				String socketID = application.getGroupContextManager().connect(app.getCommMode(), app.getIPAddress(), app.getPort());
	    				
	    				// TODO:  REPLACE ALL OF THIS SOMEDAY!
	    				// Determine what Object to send to this function
	    				ArrayList<String> objects = new ArrayList<String>();
	    				for (ApplicationObject o : applicationObjects)
	    				{
	    					if (function.isCompatible(new ApplicationObject[] { o } ))
	    					{
	    						objects.add(o.toJSON());
	    					}
	    				}
	    				
	    				// Sends the Command
	    				application.getGroupContextManager().sendComputeInstruction(socketID, app.getChannel(), app.getAppContextType(), new String[] { app.getDeviceID() }, function.getCallbackCommand(), objects.toArray(new String[0]));
	    				found = true;
    					break;
	    			}
	    			
//	    			for (ApplicationFunction f : app.getFunctions())
//	    			{
//	    				if (f == function)
//	    				{
//	    					// Opens a Temporary Connection to this Device
//		    				String socketID = application.getGroupContextManager().connect(app.getCommMode(), app.getIPAddress(), app.getPort());
//		    				
//		    				// TODO:  REPLACE ALL OF THIS SOMEDAY!
//		    				// Determine what Object to send to this function
//		    				ArrayList<String> objects = new ArrayList<String>();
//		    				for (ApplicationObject o : applicationObjects)
//		    				{
//		    					if (f.isCompatible(new ApplicationObject[] { o } ))
//		    					{
//		    						objects.add(o.toJSON());
//		    					}
//		    				}
//		    				
//		    				// Sends the Command
//		    				application.getGroupContextManager().sendComputeInstruction(socketID, app.getChannel(), app.getAppContextType(), new String[] { app.getDeviceID() }, function.getCallbackCommand(), objects.toArray(new String[0]));
//		    				found = true;
//	    					break;
//	    				}
//	    			}
	    			
	    			if (found)
	    			{
	    				break;
	    			}
	    		}
	    		
	    		if (!found)
	    		{
	    			Toast.makeText(application, "No Function Found: " + function.getName(), Toast.LENGTH_SHORT).show();
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
	
	/**
	 * Asynchronous Task to Encode a File to a String
	 * @param byteArray
	 */
	private void encodeFile(final String filename, final byte[] byteArray)
	{
		new AsyncTask<Void, Void, String>()
		{
			String encodedString = "";
			
			protected void onPreExecute()
			{
				
			}

			@Override
			protected String doInBackground(Void... params) 
			{
				long startTime = System.currentTimeMillis();
				
				encodedString = Base64.encodeToString(byteArray, Base64.DEFAULT);
				
				encodedString = Uri.encode(encodedString);
				
				long timeElapsed = System.currentTimeMillis() - startTime;
				
				Log.d(LOG_NAME, "Encoded " + encodedString.length() + " Bytes in " + timeElapsed + "ms");
				return "";
			}
			
			protected void onPostExecute(String msg)
			{
				String url = "http://gcf.cmu-tbank.com/snaptoit/upload_file.php" 
						+ "?deviceID=" + Uri.encode(application.getGroupContextManager().getDeviceID()) 
						+ "&filename=" + Uri.encode(filename);
				
				application.getHttpToolkit().post(url, "data=" + encodedString, REMOTE_UPLOAD_COMPLETE);
			}
		}.execute(null, null, null);
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
		final String uninstallName  = data.getPayload("UNINSTALL");
		
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
			String previousHtml      = app.getUI().getPayload("UI");
			String previousWebsite   = app.getUI().getPayload("WEBSITE");
			String previousPackage   = app.getUI().getPayload("PACKAGE");
			String previousUninstall = app.getUI().getPayload("UNINSTALL");
			
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
			else if (previousUninstall != null)
			{
				previousUI = previousUninstall;
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
		else if (uninstallName != null && (!uninstallName.equals(previousUI) || force))
		{
			app.setUI(data);
			
			if (appInstalled(uninstallName))
			{
				Uri packageUri = Uri.parse("package:" + uninstallName);
	            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
	            startActivity(uninstallIntent);
				
				// Kills this Activity Since we Don't Need it Anymore!
				finish();
			}
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
			else if (intent.getAction().equals(REMOTE_UPLOAD_COMPLETE))
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
						"CHANNEL=dev/" + application.getGroupContextManager().getDeviceID(),
						"credentials=" + application.getGroupContextManager().getDeviceID(),
						"preferences=" + ((preferences.length() > 0) ? preferences.substring(0, preferences.length()-1) : ""),
						"context="     + application.getBluewaveManager().getPersonalContextProvider().getContext().toString()
					});	
			}
		}
		
		private void onDownloadComplete(Context context, Intent intent)
		{
			String downloadPath = intent.getStringExtra(HttpToolkit.EXTRA_HTTP_RESPONSE);
			
			if (downloadPath != null)
			{
				Toast.makeText(application, "Download Complete (" + downloadPath + ")", Toast.LENGTH_SHORT).show();	
			}
		}
		
		private void onUploadComplete(Context context, Intent intent)
		{	
			if (application != null)
				{
					// This retrieves any commands associated with the uploaded file folder
					String callbackCommand = jsInterface.getUploadCallbackCommand();
					String uploadPath      = intent.getStringExtra(HttpToolkit.EXTRA_HTTP_RESPONSE);
					
					Toast.makeText(application, "File Uploaded: " + uploadPath + "; UPLOAD COMMAND = " + callbackCommand, Toast.LENGTH_SHORT).show();
					
					// Sends a Callback (if one is specified)
					if (callbackCommand != null && callbackCommand.length() > 0)
					{
						application.getGroupContextManager().sendComputeInstruction(app.getAppContextType(), 
								callbackCommand, 
								new String[] { "uploadPath=" + uploadPath });
					}
				}
			
//			// This is the FULL link to the file being uploaded
//			String sourcePath = intent.getStringExtra(CloudStorageToolkit.CLOUD_UPLOAD_SOURCE);
//			String filename   = sourcePath.substring(sourcePath.lastIndexOf("/") + 1);
//			
//			// This is the Link to the CLOUD LOCATION where the above file was uploaded
//			String destinationFolder = intent.getStringExtra(CloudStorageToolkit.CLOUD_UPLOAD_PATH);
//						
//			if (application != null)
//			{
//				// This retrieves any commands associated with the uploaded file folder
//				String callbackCommand = jsInterface.getUploadCallbackCommand();
//				
//				Toast.makeText(application, "File Uploaded: " + destinationFolder + "; UPLOAD COMMAND = " + callbackCommand, Toast.LENGTH_SHORT).show();
//				
//				// Sends a Callback (if one is specified)
//				if (callbackCommand != null && callbackCommand.length() > 0)
//				{
//					application.getGroupContextManager().sendComputeInstruction(app.getAppContextType(), 
//							callbackCommand, 
//							new String[] { "uploadPath=" + GCFApplication.UPLOAD_WEB_PATH + filename});
//				}
//			}
		}
		
		private void onHTMLDownloadComplete(Context context, Intent intent)
		{
			String html = intent.getStringExtra("HTML");
			webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", "");
		}
	}
	
	// Custom Browser Classes ---------------------------------------------------------------
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
	
    public class CustomChromeClient extends WebChromeClient
    {
     // For Android 4.1
    @SuppressWarnings("unused")
     public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType)
     {
    	Toast.makeText(AppEngine.this, "onShowFileChoser (Android 4.1+)", Toast.LENGTH_SHORT).show();
    	
    	application.mUploadMessage = uploadMsg;  
        
    	// Create the File where the photo should go
        File photoFile       = ImageToolkit.getOutputMediaFile("Impromptu", "photoToUpload.jpeg");
        application.imageUri = Uri.fromFile(photoFile); 

        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for(ResolveInfo res : listCam) 
        {
            final String packageName = res.activityInfo.packageName;
            final Intent i = new Intent(captureIntent);
            i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            i.setPackage(packageName);
            i.putExtra(MediaStore.EXTRA_OUTPUT, application.imageUri);
            cameraIntents.add(i);
        }

        application.mUploadMessage = uploadMsg; 
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);  
        i.addCategory(Intent.CATEGORY_OPENABLE);  
        i.setType("image/*"); 
        Intent chooserIntent = Intent.createChooser(i,"Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[0]));
        AppEngine.this.startActivityForResult(chooserIntent,  FILECHOOSER_RESULTCODE);
    }
    
     // For Android 5.0+
     public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) 
     {
    	//Toast.makeText(AppEngine.this, "onShowFileChoser (Android 5+)", Toast.LENGTH_SHORT).show();
    	 
        // Double check that we don't have any existing callbacks
        if(application.mFilePathCallback != null) 
        {
        	application.mFilePathCallback.onReceiveValue(null);
        }
        application.mFilePathCallback = filePathCallback;

        // Set up the take picture intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(AppEngine.this.getPackageManager()) != null) 
        {        	
            // Create the File where the photo should go
            File photoFile = ImageToolkit.getOutputMediaFile("Impromptu", "photoToUpload.jpeg");

            // Continue only if the File was successfully created
            if (photoFile != null) 
            {
            	application.mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                Toast.makeText(AppEngine.this, "Photo Path: " + application.mCameraPhotoPath, Toast.LENGTH_SHORT).show();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            } 
            else 
            {
                takePictureIntent = null;
            }
        }

        // Set up the intent to get an existing image
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        // Set up the intents for the Intent chooser
        Intent[] intentArray;
        if(takePictureIntent != null) 
        {
            intentArray = new Intent[]{ takePictureIntent };
        } 
        else 
        {
            intentArray = new Intent[0];
        }

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

        startActivityForResult(chooserIntent, AppEngine.FILECHOOSER_RESULTCODE);

        return true;
    }
    
     // Enabled Location Services
     public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) 
     {
    	 callback.invoke(origin, true, false);
     }
    }
}
