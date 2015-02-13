package com.adefreitas.androidframework.toolkit;

import java.io.File;
import java.io.FileOutputStream;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class FileHelper 
{

	public static void writeToFile(Context context, String foldername, String filename, String text)
	{	
		if (canWrite())
		{
			FileOutputStream outputStream;
			
			File sdCard    = Environment.getExternalStorageDirectory();
			File directory = new File (sdCard.getAbsolutePath() + "/" + foldername);
			directory.mkdirs();
			
			try 
			{
				String textToWrite = text + "\n";
				File   file 	   = new File(directory, filename);
				
				if (!file.exists())
				{
					file.createNewFile();
				}
				
				outputStream = new FileOutputStream(file, true);
				//outputStream = context.openFileOutput(filename, Context.MODE_APPEND | Context.MODE_WORLD_READABLE);
				outputStream.write(textToWrite.getBytes());
				outputStream.close();
				
				Log.i("GCM-FileIO", "Writing to " + file.getAbsolutePath() + ": " + text);
			} 
			catch (Exception e) 
			{
				Log.e("GCM-FileIO", "A Problem Occurred while Writing");
				e.printStackTrace();
			}	
		}
		else
		{
			Log.e("GCM-FileIO", "Cannot Write to External Storage");
		}
	}

	public static boolean canWrite()
	{
		//boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) 
		{
		    // We can read and write the media
		    //mExternalStorageAvailable = true;
		    mExternalStorageWriteable = true;
		} 
		else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) 
		{	
		    // We can only read the media
		    //mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} 
		else 
		{
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    //mExternalStorageAvailable false;
			mExternalStorageWriteable = false;
		}
		
		return mExternalStorageWriteable;
	}

}
