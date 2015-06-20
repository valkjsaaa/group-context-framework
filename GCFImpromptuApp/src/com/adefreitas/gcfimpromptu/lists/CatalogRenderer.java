package com.adefreitas.gcfimpromptu.lists;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.adefreitas.gcfimpromptu.GCFApplication;
import com.adefreitas.gcfmagicapp.R;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class CatalogRenderer 
{	
	// Defines the number of apps to switch to a "minimalist" view
	private static final int APP_THRESHHOLD = 3;
	
	private static HashMap<String, Bitmap>		  logoDirectory     = new HashMap<String, Bitmap>();
	private static HashMap<View, AppCategoryInfo> categoryDirectory = new HashMap<View, AppCategoryInfo>(); 
	private static HashMap<View, AppInfo> 		  appDirectory 	    = new HashMap<View, AppInfo>();
	
	/**
	 * Generates the Views for the Application Catalog
	 * @param context
	 * @param categories
	 * @return
	 */
	public static View renderCatalog(Context context, ArrayList<AppCategoryInfo> categories)
	{
		// Creates a Scrollable Container
		ScrollView container = new ScrollView(context);
		container.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			
		// Creates a Container that Holds all of the Categories/Apps
		LinearLayout layout = new LinearLayout(context);
		layout.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		layout.setOrientation(LinearLayout.VERTICAL);
		
		// Removes All Views
		categoryDirectory.clear();
		appDirectory.clear();
		
		// Counts the Total Number of Available Apps
		int totalAvailableApps = 0;
		
		for (AppCategoryInfo category : categories)
		{
			totalAvailableApps += category.getAvailableApps().size();
		}
		
		// Renders the Categories and/or Apps (depending on the total number)
		if (totalAvailableApps > APP_THRESHHOLD)
		{
			// Case 1:  LOTS of Apps.  Render Category Headings
			for (AppCategoryInfo category : categories)
			{
				View categoryView = renderCategory(context, category);
							
				if (categoryView != null)
				{
					// Stores the View with the Category for Later Reference
					categoryDirectory.put(categoryView, category);
					categoryView.setOnClickListener(onCategoryClickListener);
					
					layout.addView(categoryView);
				}
			}
		}
		else
		{
			// Case 2:  Few Apps.  Render Apps Directly
			for (AppCategoryInfo category : categories)
			{				
				// Adds the App to the Main View
				for (AppInfo app : category.getAvailableApps())
				{
					View appView = renderApplication(context, app, true);
					appDirectory.put(appView, app);
					layout.addView(appView);
				}
			}
		}
		
		// Creates a Little Breating Room on the Bottom
		LinearLayout bottomBuffer = new LinearLayout(context);
		bottomBuffer.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 100));
		bottomBuffer.setOrientation(LinearLayout.VERTICAL);
		layout.addView(bottomBuffer);
		
		// Adds the List of Apps to the ScrollView
		container.addView(layout);
		
		// Returns the ScrollView
		return container;
	}
	
	/**
	 * Generates the View for a Category
	 * @param context
	 * @param category
	 * @return
	 */
	public static View renderCategory(Context context, AppCategoryInfo category)
	{
		LayoutInflater inflater     = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View 		   categoryView = inflater.inflate(R.layout.app_category_info_single, null);
		categoryView.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		
		// Gets List of all Available Apps
		ArrayList<AppInfo> availableApps = category.getAvailableApps();
		
		// Do Nothing if there are no Available Apps for this Category
		if (availableApps.size() == 0)
		{
			return null;
		}
		
		// Gets Controls
		LinearLayout appsView  = (LinearLayout)categoryView.findViewById(R.id.layoutApps);
		TextView     txtTitle  = (TextView)categoryView.findViewById(R.id.txtTitle);
		TextView     btnExpand = (TextView)categoryView.findViewById(R.id.btnExpand);

		// Sets Colors
		txtTitle.setTextColor(Theme.getColor(category.getName()));
		
		// Updates Title
		txtTitle.setText(category.getName().toUpperCase());
		
		// Creates a Button to Display All Apps
		if (category.shouldRenderAll())
		{
			btnExpand.setVisibility(View.VISIBLE);
			btnExpand.setText("HIDE");
			btnExpand.setTextColor(0xFF333333);
		}
		else
		{
			btnExpand.setVisibility(View.VISIBLE);
			btnExpand.setText("SHOW ALL " + availableApps.size());
			btnExpand.setTextColor(0xFF0186D5);
		}
		
		// Determines How Many Apps to Render
		int numAppsToRender = category.shouldRenderAll() ? availableApps.size() : 0;
		
		// Renders Apps
		for (int i=0; i<numAppsToRender; i++)
		{
			View appView = renderApplication(context, availableApps.get(i), false);
			appDirectory.put(appView, availableApps.get(i));
			appsView.addView(appView);
		}
		
		// Returns the Category
		return categoryView;
	}
	
	/**
	 * Generates the View for a App
	 * @param context
	 * @param app
	 * @return
	 */
	public static View renderApplication(Context context, AppInfo app, boolean showAll)
	{
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View 		   appView  = inflater.inflate(R.layout.app_info_single, null);
		
		// Gets Time to Expiration
		long   expirationInMinutes = (app.getDateExpires().getTime() - System.currentTimeMillis()) / 1000 / 60;
		String expirationMessage   = (expirationInMinutes == 0) ? "<1 min" : expirationInMinutes + " min";
		
		// Gets Controls
		TextView  txtTitle 	 	 = (TextView)appView.findViewById(R.id.txtTitle);
		TextView  txtCategory    = (TextView)appView.findViewById(R.id.txtCategory);
		TextView  txtDescription = (TextView)appView.findViewById(R.id.txtDescription);
		TextView  txtRunMessage  = (TextView)appView.findViewById(R.id.txtRunMessage);
		ImageView imgAppLogo     = (ImageView)appView.findViewById(R.id.imgAppLogo); 
					
		// Sets Contents
		txtTitle.setText(app.getName());
		txtCategory.setText(app.getCategory());
		txtDescription.setText(app.getDescription());
		txtRunMessage.setText(Theme.getRunMessage(app.getCategory()) + " (" + expirationMessage + " left)" );
					
		// Sets Colors
		txtTitle.setTextColor(Theme.getColor(app.getCategory()));
		txtCategory.setTextColor(Theme.getColor(app.getCategory()));
		
		// Hides Elements if Not in Detailed View
		if (!showAll)
		{
			txtCategory.setVisibility(View.GONE);
		}
		
		// Sets Logo
		String filename = app.getLogo().substring(app.getLogo().lastIndexOf("/")+1);
		String filepath = Environment.getExternalStorageDirectory() + GCFApplication.LOGO_DOWNLOAD_FOLDER + filename;
		File   logo     = new File(filepath);
		
		if (logo.exists())
		{
			Bitmap bmLogo = logoDirectory.containsKey(filepath) ?
					logoDirectory.get(filepath) : BitmapFactory.decodeFile(filepath);
			
			if (bmLogo != null)
			{
				imgAppLogo.setImageBitmap(bmLogo);
				imgAppLogo.setBackgroundColor(0x00FFFFFF);
				logoDirectory.put(filepath, bmLogo);
			}	
		}
		
		// Sets Event Handler
		appView.setOnClickListener(onAppClickListener);
		
		// Returns the View
		return appView;
	}
	
	/**
	 * This listener gets fired when the user clicks on a specific app card
	 */
	public static final OnClickListener onAppClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v) 
		{
			AppInfo app = appDirectory.get(v);
			
			if (app != null)
			{
				// Generates an intent with the selected app
				Intent appSelectedIntent = new Intent(GCFApplication.ACTION_APP_SELECTED);
				appSelectedIntent.putExtra(GCFApplication.EXTRA_APP_ID, app.getID());
				v.getContext().sendBroadcast(appSelectedIntent);
			}
		}	
	};
	
	/**
	 * This listener gets fired when the user clicks on a category
	 */
	public static final OnClickListener onCategoryClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v) 
		{
			AppCategoryInfo category = categoryDirectory.get(v);
			
			if (category != null)
			{
				// Toggles the Render All Flag
				category.setRenderAll(!category.shouldRenderAll());
				
				Intent updateIntent = new Intent(GCFApplication.ACTION_APP_UPDATE);
				v.getContext().sendBroadcast(updateIntent);
			}
		}
	};
}
