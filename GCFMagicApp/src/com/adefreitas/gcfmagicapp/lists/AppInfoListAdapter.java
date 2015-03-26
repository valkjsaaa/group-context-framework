package com.adefreitas.gcfmagicapp.lists;

import java.util.List;

import com.adefreitas.gcfmagicapp.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AppInfoListAdapter extends ArrayAdapter<AppInfo> 
{
	private Context 	  context;
	private BitmapManager bitmapManager;
	
	public AppInfoListAdapter(Context context, int textViewResourceId, List<AppInfo> items, BitmapManager bm)
	{
		super(context, textViewResourceId, items);
		this.context 	   = context;
		this.bitmapManager = bm;
	}
	
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View	view   = convertView;
		AppInfo app    = getItem(position);
		
		if (view == null)
		{
			LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.app_info_single, null);
		}
		
		if (app != null)
		{
			// Gets Controls
			TextView  txtTitle 	 	 = (TextView)view.findViewById(R.id.txtTitle);
			TextView  txtDescription = (TextView)view.findViewById(R.id.txtDescription);
			ImageView imgBanner      = (ImageView)view.findViewById(R.id.imgBanner);
			
			// Updates Contents
			txtTitle.setText(app.getAppName());
			txtDescription.setText(app.getDescription() + "\nExpires: " + app.getDateExpires());// + "\nPhoto Matches: " + app.getPhotoMatches());
			
			// Hides Banner and Begins Download
			if (app.getLogo() == null || app.getLogo().length() == 0)
			{
				imgBanner.setVisibility(View.GONE);
			}
			else
			{
				imgBanner.setVisibility(View.VISIBLE);
				bitmapManager.loadBitmap(app.getLogo(), imgBanner, imgBanner.getWidth(), imgBanner.getHeight());
			}
		}
		
		return view;
	}
	
}
