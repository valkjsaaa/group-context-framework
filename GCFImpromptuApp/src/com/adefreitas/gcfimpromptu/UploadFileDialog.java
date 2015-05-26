package com.adefreitas.gcfimpromptu;

import java.io.File;
import java.io.FilenameFilter;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.adefreitas.androidframework.toolkit.CloudStorageToolkit;

public class UploadFileDialog 
{
	public static final String REMOTE_UPLOAD_COMPLETE = "APP_UPLOAD";
	
	private Context 	   		context;
	private CloudStorageToolkit cloudToolkit;
	private String 		   		cloudDestinationFolder;
	private String 				callbackCommand;
	
	// In an Activity
	private String			 dialogMessage;
	private String[] 	     mFileList;
	private File 	 	     mPath;
	private String 		     mChosenFile;
	private String[] 		 filetypes;   
	private static final int DIALOG_LOAD_FILE = 1000;

	public UploadFileDialog(Context context, String dialogMessage, String startingFolder, String[] filetypes, CloudStorageToolkit cloudToolkit, String cloudDestinationFolder, String callbackCommand)
	{
		this.context       		  	= context;
		this.dialogMessage			= dialogMessage;
		this.callbackCommand		= callbackCommand;
		this.cloudToolkit 		  	= cloudToolkit;
		this.cloudDestinationFolder = cloudDestinationFolder;
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
	                    	Toast.makeText(context, "Uploading " + file.getName() + " to " + cloudDestinationFolder, Toast.LENGTH_SHORT).show();
	                    	cloudToolkit.uploadFile(cloudDestinationFolder, file, REMOTE_UPLOAD_COMPLETE);
	                    }
	                }
	            });
	            break;
	    }
	    
	    dialog = builder.show();
	    return dialog;
	}
}
