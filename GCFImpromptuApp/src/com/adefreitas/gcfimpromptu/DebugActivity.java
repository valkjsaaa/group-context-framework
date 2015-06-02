package com.adefreitas.gcfimpromptu;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.androidframework.ContextReceiver;
import com.adefreitas.gcfimpromptu.lists.AppCategoryInfo;
import com.adefreitas.gcfimpromptu.lists.AppInfo;
import com.adefreitas.gcfmagicapp.R;
import com.adefreitas.groupcontextframework.ContextProvider;

public class DebugActivity extends Activity 
{
	private GCFApplication application;
	
	// Controls
	private Toolbar  toolbar;
	private TextView txtAppCatalog;
	private TextView txtBluewave;
	private TextView txtContextProviders;
	private Button   btnClearCache;
	
	// Intent Filters
	private IntentFilter   filter;
	private IntentReceiver intentReceiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_debug);
		
		this.application = (GCFApplication)this.getApplication();
		
		// Gets Controls
		this.toolbar 	   		 = (Toolbar)this.findViewById(R.id.toolbar);
		this.txtAppCatalog 		 = (TextView)this.findViewById(R.id.txtAppCatalog);
		this.txtBluewave   		 = (TextView)this.findViewById(R.id.txtBluewave);
		this.txtContextProviders = (TextView)this.findViewById(R.id.txtContextProviders);
		this.btnClearCache 		 = (Button)this.findViewById(R.id.btnClearCache);
		
		// Sets Event Handler
		this.btnClearCache.setOnClickListener(onCacheClearListener);
		
		// Listens for Intents
		this.intentReceiver = new IntentReceiver();
		this.filter = new IntentFilter();
		this.filter.addAction(GCFApplication.ACTION_APP_UPDATE);
		
		// Updates the View
		updateViewContents();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.debug, menu);
		return true;
	}

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
	 * Android Method:  Used when an Activity is Resumed
	 */
	protected void onResume()
	{
		super.onResume();
			
		application.setInForeground(true);
		
		// Sets Up the Intent Listener
		this.registerReceiver(intentReceiver, filter);
	}
	
	/**
	 * Android Method:  Used when an Activity is Paused
	 */
	@Override
	protected void onPause() 
	{
	    super.onPause();
	    this.unregisterReceiver(intentReceiver);
	    application.setInForeground(false);
	}
	
	private void updateViewContents()
	{
		toolbar.setTitle(this.getString(R.string.title_activity_debug));
		
		// Updates Application Catalog String
		String appCatalogDetails = "";
		
		int numActiveApps  = 0;
		int numExpiredApps = 0;
		int numCategories  = 0;
		
		for (AppCategoryInfo category : application.getCatalog())
		{
			numCategories++;
			
			for (AppInfo app : category.getApps())
			{
				if (app.isExpired())
				{
					numExpiredApps++;
				}
				else
				{
					numActiveApps++;
				}
			}
		}
		
		appCatalogDetails += "Num Categories: " + numCategories  + "\n";
		appCatalogDetails += "Num Active: " + numActiveApps  + "\n";
		appCatalogDetails += "Num Expired: " + numExpiredApps + "\n";
		txtAppCatalog.setText(appCatalogDetails);
		
		// Updates GCF Contents
		String contextProviderDetails = "";
		
		for (ContextProvider p : this.application.getGroupContextManager().getRegisteredProviders())
		{
			contextProviderDetails += "Context Type: " + p.getContextType() + "\n";
			contextProviderDetails += "Description: " + p.getDescription() + "\n";
			contextProviderDetails += "# Subscriptions: " + p.getNumSubscriptions() + "\n\n";
		}
		
		txtContextProviders.setText(contextProviderDetails.trim());
		
		// Updates Bluewave Contents
		try
		{
			txtBluewave.setText(application.getBluewaveManager().getPersonalContextProvider().getContext().toString());
		}
		catch (Exception ex)
		{
			txtBluewave.setText("Bluewave Data Not Available");
		}
	}

	private OnClickListener onCacheClearListener = new OnClickListener()
	{
		@Override
		public void onClick(View v) 
		{
			application.clearCache();
			updateViewContents();
		}
	};

	private class IntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(GCFApplication.ACTION_APP_UPDATE))
			{
				updateViewContents();
			}
		}
	}
}
