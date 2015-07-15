package com.adefreitas.gcfimpromptu;

import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.androidbluewave.JSONContextParser;
import com.adefreitas.androidframework.AndroidCommManager;
import com.adefreitas.androidframework.ContextReceiver;
import com.adefreitas.androidframework.GCFService;
import com.adefreitas.gcfimpromptu.lists.AppInfo;
import com.adefreitas.gcfimpromptu.lists.CatalogRenderer;
import com.adefreitas.gcfmagicapp.R;
import com.adefreitas.messages.ContextData;
import com.adefreitas.messages.ContextRequest;

public class MainActivity extends ActionBarActivity implements ContextReceiver
{	
	// Constants
	private static final String TITLEBAR_CONTENTS = "TITLE";
	private static final String APP_ID			  = "APP_ID";
	private static final double CONFIDENT_MATCH   = 40.0;
	
	// Link to the Application
	private GCFApplication application;
	
	// Android Controls
	private Toolbar	     	   toolbar;
	private LinearLayout       layoutApps;
	private ExpandableListView lstApps;
	private LinearLayout 	   layoutInstructions;
	private ImageView    	   imgCameraSmall;
	private ImageView    	   imgIconBig;
	private TextView	 	   txtInstructionTitle;
	private TextView	 	   txtInstructionDescription;

	// Intent Filters
	private ContextReceiver contextReceiver;
	private IntentFilter    filter;
	private IntentReceiver  intentReceiver;
	
	// Temporary
	private boolean connectionProblems = false;
	private AppInfo selectedApp;
	
	/**
	 * Android Method:  Used when the Activity is Created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Creates a Link to the Application
		this.application = (GCFApplication)this.getApplication();
		
		// Saves Controls
		this.toolbar   	   			   = (Toolbar)this.findViewById(R.id.toolbar);
		this.layoutApps  	    	   = (LinearLayout)this.findViewById(R.id.layoutApps);
		this.lstApps  	    		   = (ExpandableListView)this.findViewById(R.id.lstApps);
		this.layoutInstructions 	   = (LinearLayout)this.findViewById(R.id.layoutInstructions);
		this.imgCameraSmall 		   = (ImageView)this.findViewById(R.id.imgCameraSmall);
		this.imgIconBig   		       = (ImageView)this.findViewById(R.id.imgCameraBig);
		this.txtInstructionTitle 	   = (TextView)this.findViewById(R.id.txtInstructionTitle);
		this.txtInstructionDescription = (TextView)this.findViewById(R.id.txtInstructionDescription);
				
		// Sets Up Event Handlers
		this.lstApps.setOnChildClickListener(onItemClickListener);
		
		// Create Intent Filter and Receiver
		this.intentReceiver = new IntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(GCFApplication.ACTION_APP_UPDATE);
		this.filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.filter.addAction(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED);
		this.filter.addAction(AndroidCommManager.ACTION_CHANNEL_SUBSCRIBED);
		this.filter.addAction(GCFApplication.ACTION_APP_SELECTED);
		this.filter.addAction(AndroidCommManager.ACTION_CONNECTION_ERROR);
		
		// Creates Event Handlers
		this.imgCameraSmall.setOnClickListener(onSnapToItClickListener);
		this.imgIconBig.setOnClickListener(onRefreshClickListener);
		
		// Generates the Execution Alert for an App
		if (this.getIntent() != null && this.getIntent().hasExtra(APP_ID))
		{
			//Toast.makeText(this, "Processing Intent: " + this.getIntent().getStringExtra(APP_ID), Toast.LENGTH_SHORT).show();
			selectedApp = this.application.getApplicationFromCatalog(this.getIntent().getStringExtra(APP_ID));
			
			if (selectedApp != null)
			{
				this.promptToConnect(selectedApp, false);
				this.getIntent().removeExtra(APP_ID);
			}
			else
			{
				Toast.makeText(this, "Did not find App: " + this.getIntent().getStringExtra(APP_ID), Toast.LENGTH_SHORT).show();
			}
		}
		else if (savedInstanceState != null && savedInstanceState.containsKey(APP_ID))
		{
			selectedApp = this.application.getApplicationFromCatalog(savedInstanceState.getString(APP_ID));
			
			if (selectedApp != null)
			{
				this.promptToConnect(selectedApp, false);
				savedInstanceState.remove(APP_ID);
			}
			else
			{
				Toast.makeText(this, "Did not find App: " + savedInstanceState.getString(APP_ID), Toast.LENGTH_SHORT).show();
			}
		}
		
		// Sets Up Toolbar
		this.setSupportActionBar(toolbar);
		
		// Sets the Name of the Toolbar
		if (savedInstanceState != null && savedInstanceState.containsKey(TITLEBAR_CONTENTS))
		{
			toolbar.setTitle(savedInstanceState.getString(TITLEBAR_CONTENTS));
		}
				
		// Requests Context Information from Self
		if (application.getGroupContextManager().getRequest("LOC") == null || application.getGroupContextManager().getRequest("ACT") == null)
		{
			application.getGroupContextManager().sendRequest("LOC", ContextRequest.LOCAL_ONLY, GCFApplication.UPDATE_SECONDS * 1000, new String[0]);
			application.getGroupContextManager().sendRequest("ACT", ContextRequest.LOCAL_ONLY, GCFApplication.UPDATE_SECONDS * 1000, new String[0]);	
		}
		
		// Updates the Application List
		updateViewContents();
	}

	/**
	 * Android Method:  Used when the Menu is Created
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Android Method:  Used when a Menu Item is Selected
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
			// Kills the Timer Thread
			application.halt();
		
			application.getGCFService().setRestart(false);
			
			// Creates an Intent to Kill the GCF Service
			Intent intent = new Intent(this, GCFService.class);
			this.stopService(intent);
			android.os.Process.killProcess(android.os.Process.myPid());
		}
		else if (item.toString().equalsIgnoreCase(this.getString(R.string.action_refresh)))
		{
			onRefreshClickListener.onClick(null);
		}
		else if (item.toString().equalsIgnoreCase(this.getString(R.string.title_debug)))
		{
			Intent intent = new Intent(this, DebugActivity.class);
			this.startActivity(intent);
		}
		
		return true;
	}
	
	/**
	 * Android Method:  Used when an Activity is Resumed
	 */
	protected void onResume()
	{
		super.onResume();
			
		application.setInForeground(true);
		
		// Sets Up the Intent Listener
		this.registerReceiver(intentReceiver, filter);
		
		// Notifies the Application to Forward GCF Messages to this View
		this.application.setContextReceiver(this);
				
		// Clears All Existing UIs
		for (AppInfo app : application.getAvailableApps())
		{
			app.setUI(null);
		}
		
		// Cleans Up Running Applications
		if (application.getActiveApplications().size() > 0)
		{			
			// Terminates Active Subscriptions
//			for (ContextRequest request : application.getGroupContextManager().getRequests())
//			{
//				application.getGroupContextManager().cancelRequest(request.getContextType());
//			}
			
			// Clears Active Applications
			for (AppInfo app : application.getActiveApplications())
			{
				application.getGroupContextManager().cancelRequest(app.getAppContextType());
				
				if (application.getGroupContextManager().isConnected(app.getCommMode(), app.getIPAddress(), app.getPort()))
				{
					String connectionKey = application.getGroupContextManager().getConnectionKey(app.getCommMode(), app.getIPAddress(), app.getPort());
					
					if (app.getCommMode() == GCFApplication.COMM_MODE && app.getIPAddress().equals(GCFApplication.IP_ADDRESS) && app.getPort() == GCFApplication.PORT)
					{
						application.getGroupContextManager().unsubscribe(connectionKey, app.getChannel());
					}
					else
					{
						application.getGroupContextManager().disconnect(connectionKey);
					}
				}
			}
			
			// Removes all Active Applications
			application.getActiveApplications().clear();
		}
		
		// Updates the Application List
		updateViewContents();
	}
	
	/**
	 * Android Method:  Used when an Activity is Paused
	 */
	@Override
	protected void onPause() 
	{
	    super.onPause();
	    this.unregisterReceiver(intentReceiver);
	    this.application.setContextReceiver(null);
	    application.setInForeground(false);
	}

	/**
	 * Android Method:  Used when an Activity is About to Close to Save Important Information
	 */
	@Override
	protected void onSaveInstanceState(Bundle bundle)
	{
		if (selectedApp != null)
		{
			bundle.putString(APP_ID, selectedApp.getID());
			bundle.putString(TITLEBAR_CONTENTS, toolbar.getTitle().toString());
		}
	}
	
	// GCF Logic Goes Here ----------------------------------------------------------------------
	public void onContextData(ContextData data)
	{
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onBluewaveContext(JSONContextParser parser)
	{
		// Do Something?
	}

	// Custom Application Logic Goes Here -------------------------------------------------------
	/**
	 * Creates an Alert to Run an Application
	 * @param app
	 */
	public void promptToConnect(final AppInfo app, final boolean forceConfirmation)
	{	
		// CHEATER CHEATER!!!
		if (app.getAppContextType().equals("CF_REPORT"))
		{
		  	// Immediately Connects if No Context/Preferences are Needed
			Intent i = new Intent(application, ProblemReport.class);
    		startActivity(i); 
    		application.addActiveApplication(app);
    		selectedApp = null;
			return;
		}

		if (app.getPreferences().size() == 0 && app.getContextsRequired().size() == 0 && !forceConfirmation)
		{
			selectedApp = null;
			
		  	// Immediately Connects if No Context/Preferences are Needed
			Intent i = new Intent(application, AppEngine.class);
			i.putExtra("APP_ID", app.getID());
    		startActivity(i); 
		}
		else
		{
			// Creates a Confirmation Dialog if there Are Contexts
			AlertDialog.Builder alert = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
			
			final FrameLayout frameView = new FrameLayout(this);
			alert.setView(frameView);
			
			final AlertDialog alertDialog = alert.create();
			LayoutInflater inflater = alertDialog.getLayoutInflater();
			View dialoglayout = inflater.inflate(R.layout.dialog_app_alert, frameView);

			// Grabs Controls
			TextView 	 txtAppName 	   = (TextView)dialoglayout.findViewById(R.id.txtAppName);
			TextView 	 txtAppDescription = (TextView)dialoglayout.findViewById(R.id.txtAppDescription);
			TextView 	 txtAppContexts    = (TextView)dialoglayout.findViewById(R.id.txtAppContexts);
			TextView 	 txtAppPreferences = (TextView)dialoglayout.findViewById(R.id.txtAppPreferences);
			LinearLayout btnRun			   = (LinearLayout)dialoglayout.findViewById(R.id.btnRun);
			
			txtAppName.setText(app.getName());
			txtAppDescription.setText(app.getDescription());
			//txtAppDescription.setText("ID: " + app.getAppID() + "\n" + app.getDescription());
			
			// Shows Contexts Required
			if (app.getContextsRequired().size() > 0)
			{
				LinearLayout layoutContexts = (LinearLayout)dialoglayout.findViewById(R.id.layoutContexts);
				layoutContexts.setVisibility(View.VISIBLE);
				txtAppContexts.setText(app.getContextsRequired().toString());
			}
			
			// Shows Preferences
			if (app.getPreferences().size() > 0)
			{
				LinearLayout layoutPreferences = (LinearLayout)dialoglayout.findViewById(R.id.layoutPreferences);
				layoutPreferences.setVisibility(View.VISIBLE);
				txtAppPreferences.setText(app.getPreferences().toString());
			}
			
			final OnClickListener onButtonRunPressed = new OnClickListener() 
			{
				@Override
		        public void onClick(final View v)
		        {
		    		alertDialog.dismiss();
		    		
		    		selectedApp = null;
		    		
				  	// Opens Up the App Engine
					Intent i = new Intent(application, AppEngine.class);
					i.putExtra("APP_ID", app.getID());
		    		startActivity(i); 
		        }
		    };
		    
		    final OnDismissListener onDialogDismissed = new OnDismissListener()
		    {
				@Override
				public void onDismiss(DialogInterface dialog)
				{
					selectedApp = null;
				}
		    };
		    
		    // Sets Events
		    btnRun.setOnClickListener(onButtonRunPressed);
		    alertDialog.setOnDismissListener(onDialogDismissed);
			
		    // Shows the Finished Dialog
			alertDialog.show();	
		}	
	}
	
	/**
	 * Updates the Visual Look of the Entire Activity
	 */
	public void updateViewContents()
	{					
		//Toast.makeText(this, "Updating View", Toast.LENGTH_SHORT).show();
		
		// Determines What Apps are Currently Available
		List<AppInfo> availableApps = application.getAvailableApps();
		
		// Removes All Apps Currently Visible
		layoutApps.removeAllViews();
		
		// Determines What Icon to Display
		if (connectionProblems)
		{
			imgCameraSmall.setVisibility(View.GONE);
			lstApps.setVisibility(View.GONE);
			layoutInstructions.setVisibility(View.VISIBLE);
		}
		else
		{			
			// Displays the Snap-To-It Control (Assuming that the Feature is Enabled in Settings)
			if (PreferenceManager.getDefaultSharedPreferences(this.getBaseContext()).getBoolean("sti_enabled", false))
			{
				imgCameraSmall.setVisibility(View.VISIBLE);
			}
			else
			{
				imgCameraSmall.setVisibility(View.GONE);
			}
		
			// Determines Whether to Show the App Tray, or a Friendly Message
			if (availableApps.size() == 0)
			{
				lstApps.setVisibility(View.GONE);
				layoutInstructions.setVisibility(View.VISIBLE);
			}
			else
			{
				// Runs a Process to Auto Launch the Best App
				runAutoLaunch();
				
				// Rendering Custom UI
				layoutApps.addView(CatalogRenderer.renderCatalog(this, application.getCatalog()));
				
				lstApps.setVisibility(View.VISIBLE);
				layoutInstructions.setVisibility(View.GONE);	
			}
		}
		
		if (!connectionProblems)
		{
			// Determines What Message to Display
			if (application.getGCFService() != null && 
				application.getGCFService().isReady() && 
				application.getBluewaveManager().getPersonalContextProvider().getContext() != null && 
				application.getBluewaveManager().getPersonalContextProvider().getContext().getJSONObject(GCFApplication.SNAP_TO_IT_TAG) != null && 
				!application.getBluewaveManager().getPersonalContextProvider().getContext().getJSONObject(GCFApplication.SNAP_TO_IT_TAG).isNull("PHOTO"))
			{
				txtInstructionTitle.setText("Searching For Your Device");
				txtInstructionDescription.setText("Snap-To-It is looking for your device.  This may take a few seconds.");
				imgIconBig.setBackground(this.getResources().getDrawable(R.drawable.camera_searching));
			}
			else
			{
				txtInstructionTitle.setText("It's All Good");
				txtInstructionDescription.setText("Impromptu is analyzing your surroundings and looking for relevant apps.  Check to make sure your network connections are good.  If they are, then sit back and wait!");
				//txtInstructionDescription.setText("Welcome to Snap-To-It!  To get started, press the above button and take a photograph of the appliance you want to control!");
				imgIconBig.setBackground(this.getResources().getDrawable(R.drawable.ic_launcher));
			}
			
			// Determines if the Snap To It Button is Visible
			imgCameraSmall.setBackground(this.getResources().getDrawable(R.drawable.camera_focused));
		}
		else
		{
			txtInstructionTitle.setText("Connection Problems");
			txtInstructionDescription.setText("Impromptu is having problems connecting to the Internet.  Stand by . . .");
			imgIconBig.setBackground(this.getResources().getDrawable(R.drawable.connection));
		}
	}
	
	public void runAutoLaunch()
	{
		// Determines if AutoLaunch is Enabled
		final boolean autoMode = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("sti_auto", true);
		
		if (autoMode)
		{
			Thread t = new Thread()
			{
				public void run()
				{
					try
					{
						sleep(1000);
						
						// Runs the First App with Over X Matches
						int     count    = 0;
						AppInfo appToRun = null;

						// Looks for App
						for (AppInfo app : application.getAvailableApps())
						{
							if (app.getPhotoMatches() >= CONFIDENT_MATCH)
							{
								count++;
								appToRun = app;
							}
						}
						
						if (appToRun != null && count == 1 && application.shouldAutoLaunch())
						{
							application.setAutoLaunch();
							promptToConnect(appToRun, false);
						}
					}
					catch (Exception ex)
					{
						Toast.makeText(application, "Auto Launch Failure: " + ex.getMessage(), Toast.LENGTH_LONG).show();
						ex.printStackTrace();
					}
				}
			};
			
			t.start();
		}

//		// Runs the First App with Over X Matches
//		int     count    = 0;
//		AppInfo appToRun = null;
//		
//		if (autoMode)
//		{
//			// Looks for App
//			for (AppInfo app : application.getApplicationCatalog())
//			{
//				if (app.getPhotoMatches() >= CONFIDENT_MATCH && application.shouldAutoLaunch())
//				{
//					count++;
//					appToRun = app;
//				}
//			}
//			
//			if (appToRun != null && count == 1)
//			{
//				application.setAutoLaunch();
//				connectToApp(appToRun, false);
//			}
//		}
	}
	
	// Event Handlers Go Here -------------------------------------------------------------------
	final OnChildClickListener onItemClickListener = new OnChildClickListener() 
    {
		public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id)
		{
			try
			{
				selectedApp = application.getCatalog().get(groupPosition).getApps().get(childPosition);
				
				if (selectedApp != null)
				{ 
					promptToConnect(selectedApp, false);	
				} 
				
				return true;
			}
			catch (Exception ex)
			{
				Toast.makeText(application, "Problem Occurred While Selecting App: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
				ex.printStackTrace();
			}
			
			return false;
		}
    };

    final OnClickListener onSnapToItClickListener = new OnClickListener() 
    {
		@Override
		public void onClick(View v) 
		{
		  	// Opens Up the Camera Application
			Intent i = new Intent(application, SnapToItActivity.class);
			i.putExtra("SnapToIt", true);
    		startActivity(i); 
		}
    };

    final OnClickListener onRefreshClickListener = new OnClickListener() 
    {
		@Override
		public void onClick(View v) 
		{
			GCFApplication.sendQuery(application, null, null, 0.0, 0.0, 0.0, true);
		}
    };
	
	// Intent Receiver --------------------------------------------------------------------------
    private class IntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{	
			if (intent.getAction().equals(GCFApplication.ACTION_APP_UPDATE))
			{
				onAppUpdate(context, intent);
			}
			else if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
			{
				onDiscoveryFinished(context, intent);
			}
			else if (intent.getAction().equals(AndroidCommManager.ACTION_COMMTHREAD_CONNECTED))
			{
				onConnected(context, intent);
			}
			else if (intent.getAction().equals(AndroidCommManager.ACTION_CHANNEL_SUBSCRIBED))
			{
				onSubscribed(context, intent);
			}
			else if (intent.getAction().equals(AndroidCommManager.ACTION_CONNECTION_ERROR))
			{
				onConnectionError(context, intent);
			}
			else if (intent.getAction().equals(GCFApplication.ACTION_APP_SELECTED))
			{
				onAppSelected(context, intent);
			}
			else
			{
				Log.e("", "Unknown Action: " + intent.getAction());
			}
		}
		
		private void onAppUpdate(Context context, Intent intent) 
		{
			updateViewContents();
		}
		
		private void onDiscoveryFinished(Context context, Intent intent) 
		{
			updateViewContents();
		}
		
		private void onConnected(Context context, Intent intent)
		{
			if (connectionProblems)
			{
				connectionProblems = false;
				updateViewContents();
			}
		}
		
		private void onSubscribed(Context context, Intent intent)
		{
			if (connectionProblems)
			{
				connectionProblems = false;
				updateViewContents();
			}
		}
		
		private void onConnectionError(Context context, Intent intent)
		{
			if (!connectionProblems)
			{
				connectionProblems = true;
				updateViewContents();
			}
		}
		
		private void onAppSelected(Context context, Intent intent)
		{
			String appID = intent.getStringExtra(GCFApplication.EXTRA_APP_ID);
			
			if (appID != null)
			{
				selectedApp = application.getApplicationFromCatalog(appID);
			
				if (selectedApp != null)
				{
					promptToConnect(selectedApp, false);
				}
			}
		}
		
		
	}
}
