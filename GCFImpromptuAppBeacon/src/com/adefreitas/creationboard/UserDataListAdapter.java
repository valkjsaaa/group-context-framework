package com.adefreitas.creationboard;

import java.util.Date;
import java.util.List;

import com.adefreitas.magicappserver.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class UserDataListAdapter extends ArrayAdapter<UserData> 
{
	private Context context;
	
	public UserDataListAdapter(Context context, int textViewResourceId, List<UserData> items)
	{
		super(context, textViewResourceId, items);
		this.context 	   = context;
	}
	
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View	 view = convertView;
		UserData data = getItem(position);
		
		if (view == null)
		{
			LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.user_info_single, null);
		}
		
		if (data != null)
		{
			// Gets Controls
			TextView  txtName 	 	 = (TextView)view.findViewById(R.id.txtName);
			TextView  txtDescription = (TextView)view.findViewById(R.id.txtDescription);
		
			long timeElapsed = (new Date().getTime() - data.getLastEncounteredDate().getTime());
			
			if (timeElapsed < 60000)
			{
				txtName.setTextColor(0xFF339966);
			}
			else if (timeElapsed < 120000)
			{
				txtName.setTextColor(0xFF666633);
			}
			else
			{
				txtName.setTextColor(0xFF666666);
			}
			
			txtName.setText(data.getName());
			txtDescription.setText(data.getPhone() + "\nLast Seen: " + data.getLastEncounteredDate() + " by " + data.getSensingDevice());
		}
		
		return view;
	}
	
}
