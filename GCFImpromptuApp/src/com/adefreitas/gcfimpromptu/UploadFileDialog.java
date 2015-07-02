package com.adefreitas.gcfimpromptu;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.androidframework.toolkit.CloudStorageToolkit;
import com.adefreitas.androidframework.toolkit.ImageToolkit;

public class UploadFileDialog 
{
	public static final String LOG_NAME				  = "UPLOAD_FILE_DIALOG";
	public static final String REMOTE_UPLOAD_COMPLETE = "APP_UPLOAD";
	public static final String CLOUD_UPLOAD_URL       = "http://gcf.cmu-tbank.com/snaptoit/upload_file.php";
	
	private GCFApplication application;
	private Context		   context;
	private String 		   callbackCommand;
	
	// In an Activity
	private String			 dialogMessage;
	private String[] 	     mFileList;
	private File 	 	     mPath;
	private String 		     mChosenFile;
	private String[] 		 filetypes;   
	private static final int DIALOG_LOAD_FILE = 1000;

	public UploadFileDialog(GCFApplication application, Context context, String dialogMessage, String startingFolder, String[] filetypes, String callbackCommand)
	{
		this.application       		= application;
		this.context				= context;
		this.dialogMessage			= dialogMessage;
		this.callbackCommand		= callbackCommand;
		this.filetypes 		        = filetypes;
		mPath 		        		= new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + startingFolder);
		
		// Looks for All Files that Match the Specified Extensions
		loadFileList();
		
		// Creates the Dialog
		onCreateDialog(DIALOG_LOAD_FILE);
	}
	
	public void loadFileList() {
	    try 
	    {
	        mPath.mkdirs();
	    }
	    catch(SecurityException e) 
	    {
	        Log.e("FileDialog", "unable to write on the sd card " + e.toString());
	    }
	    if(mPath.exists()) 
	    {
	        FilenameFilter filter = new FilenameFilter() 
	        {
	            public boolean accept(File dir, String filename) 
	            {
	                File sel = new File(dir, filename);
	                
	                if (sel.isDirectory())
	                {
	                	return true;
	                }
	                else
	                {
	                	//Log.i("FILE_DIALOG", "Looking at filename: " + filename);
	                	
	                	for (String filetype : filetypes)
	                	{
	                		if (filename.contains(filetype))
	                		{
	                			//Log.i("FILE_DIALOG", "  MATCH: " + filetype);
	                			return true;
	                		}
	                	}
	                }
	                
	                return false;
	            }
	        };
	        mFileList = mPath.list(filter);
	    }
	    else 
	    {
	        mFileList= new String[] { };
	    }
	}
	
	public String getCallbackCommand()
	{
		return callbackCommand;
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
				String url = CLOUD_UPLOAD_URL 
						+ "?deviceID=" + Uri.encode(application.getGroupContextManager().getDeviceID()) 
						+ "&filename=" + Uri.encode(filename);
				
				application.getHttpToolkit().post(url, "data=" + encodedString, REMOTE_UPLOAD_COMPLETE);
			}
		}.execute(null, null, null);
	}
	
	protected Dialog onCreateDialog(int id) 
	{
	    Dialog dialog = null;
	    AlertDialog.Builder builder = new Builder(context, AlertDialog.THEME_HOLO_LIGHT);

	    switch(id) {
	        case DIALOG_LOAD_FILE:
	            builder.setTitle(dialogMessage);
	            
	            if (mFileList == null) 
	            {
	                Log.e("FileDialog", "Showing file picker before loading the file list");
	                dialog = builder.create();
	                return dialog;
	            }
	            
	            builder.setItems(mFileList, new DialogInterface.OnClickListener() 
	            {
	                public void onClick(DialogInterface dialog, int which) 
	                {
	                    mChosenFile = mPath + "/" + mFileList[which];
	                    File file = new File(mChosenFile);
	                    
	                    // Only Uploads if the File is Valid
	                    if (file.isFile())
	                    {
	                    	Toast.makeText(application, "Uploading " + file.getName() + " to " + CLOUD_UPLOAD_URL, Toast.LENGTH_SHORT).show();
	                    	
	                    	try 
	                    	{
	                    		byte[] bytes = new byte[(int)file.length()];
	                    	    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
	                    	    buf.read(bytes, 0, bytes.length);
	                    	    buf.close();
	                    	    
		                    	encodeFile(file.getName(), bytes);
	                    	} 
	                    	catch (Exception ex)
	                    	{
	                    	    // TODO Auto-generated catch block
	                    	    ex.printStackTrace();
	                    	}
	                    }
	                }
	            });
	            break;
	    }
	    
	    dialog = builder.show();
	    return dialog;
	}
}
