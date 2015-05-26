package com.adefreitas.gcfimpromptu.lists;

import java.util.ArrayList;
import java.util.List;

import com.adefreitas.gcfmagicapp.R;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AppExpandableListAdapter extends BaseExpandableListAdapter
{
	// Private Variables
	private Context 			  context;
	private List<AppCategoryInfo> categories;
	
	public AppExpandableListAdapter(Context context, List<AppCategoryInfo> categories)
	{
		this.context    = context;
		this.categories = categories;
	}
	
	@Override
	public int getGroupCount() 
	{
		return categories.size();
	}

	@Override
	public int getChildrenCount(int groupPosition) 
	{
		return categories.get(groupPosition).getApps().size();
	}

	@Override
	public Object getGroup(int groupPosition) 
	{
		return categories.get(groupPosition);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) 
	{
		return categories.get(groupPosition).getApps().get(childPosition);
	}

	@Override
	public long getGroupId(int groupPosition) 
	{
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) 
	{
		return childPosition;
	}

	@Override
	public boolean hasStableIds() 
	{
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) 
	{
		return true;
	}
	
	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) 
	{
		View			view     = convertView;
		AppCategoryInfo category = categories.get(groupPosition);
				
		if (view == null)
		{
			LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.app_category_info_single, null);
		
			if (category != null)
			{
				// Gets Available Apps (Non-Expired)
				ArrayList<AppInfo> availableApps = category.getAvailableApps();
				
				// Gets Controls
				LinearLayout layoutCategory = (LinearLayout)view.findViewById(R.id.layoutCategory);
				TextView     txtTitle       = (TextView)view.findViewById(R.id.txtTitle);

				// Sets Colors
				txtTitle.setBackgroundColor(Color.WHITE);
				txtTitle.setTextColor(Theme.getColor(category.getName()));
				
				// Updates Title
				txtTitle.setText(category.getName().toUpperCase() + " (" + availableApps.size() + ")");
					
//				if (availableApps.size() == 0)
//				{
//					layoutCategory.setLayoutParams(new LayoutParams(0, 0));				
//				}
			}
		}
				
		return view;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) 
	{
		View	view = convertView;
		AppInfo app  = categories.get(groupPosition).getApps().get(childPosition);
				
		if (view == null)
		{
			LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.app_info_single, null);
		
			// Gets Controls
			TextView txtTitle 		= (TextView)view.findViewById(R.id.txtTitle);
			TextView txtDescription = (TextView)view.findViewById(R.id.txtDescription);
			
			// Sets Contents
			txtTitle.setText(app.getAppName());
			txtDescription.setText(app.getDescription());
			
			// Sets Colors
			txtTitle.setTextColor(Theme.getColor(app.getCategory()));
		}
				
		return view;
	}
}
