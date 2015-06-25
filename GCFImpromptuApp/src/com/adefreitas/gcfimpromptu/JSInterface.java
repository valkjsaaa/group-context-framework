package com.adefreitas.gcfimpromptu;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.os.Environment;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.JavascriptInterface;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.androidframework.toolkit.HttpToolkit;
import com.adefreitas.gcfimpromptu.lists.AppInfo;
import com.adefreitas.gcfmagicapp.R;

public class JSInterface
{			
	// Application Constants
	public static final String JAVASCRIPT_OBJECT_NAME = "device";
	
	private Handler 		handler;
	private GCFApplication	application;
	private Context 		context;
	private AppEngine		activity;
	private HttpToolkit		httpToolkit;
	
	// Tracks the Callback Command
	private String callbackCommand;
	
	/**
	 * Constructor
	 * @param application
	 */
	public JSInterface(GCFApplication application, AppEngine activity, Context context)
	{
		this.application = application;
		this.handler     = new Handler();
		this.context	 = context;
		this.httpToolkit = new HttpToolkit(application);
		this.activity    = activity;
		
		callbackCommand = "";
	}
	
	// GETTERS -----------------------------------------------------------------------------------------
	public String getUploadCallbackCommand()
	{
		return callbackCommand;
	}
	
	// DIALOGS -----------------------------------------------------------------------------------------
	public void createDownloadAlert(final String url)
	{	
		AlertDialog.Builder alert = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
		
		final FrameLayout frameView = new FrameLayout(context);
		alert.setView(frameView);
		
		final AlertDialog alertDialog = alert.create();
		LayoutInflater inflater = alertDialog.getLayoutInflater();
		View dialoglayout = inflater.inflate(R.layout.dialog_download_alert, frameView);

		// Grabs Controls
		LinearLayout btnDownload = (LinearLayout)dialoglayout.findViewById(R.id.btnDownload);
		TextView 	 txtFilePath = (TextView)dialoglayout.findViewById(R.id.txtFilePath);
		txtFilePath.setText(url);

		final OnClickListener onButtonDownloadPressed = new OnClickListener() 
		{
			@Override
	        public void onClick(final View v)
	        {
	    		alertDialog.dismiss();
	    		
	    		// Downloads the File
	    		httpToolkit.download(url, Environment.getExternalStorageDirectory() + GCFApplication.DOWNLOAD_FOLDER, "DOWNLOAD_COMPLETE");
	    		
	    		try
	    		{
	    			JSONObject interactionObj = new JSONObject();
		    		interactionObj.put("type", "FILE");
		    		interactionObj.put("extension", url.substring(url.lastIndexOf(".")+1));
		    		interactionObj.put("url", url);
		    		application.getBluewaveManager().getPersonalContextProvider().setContext("interaction", interactionObj);
	    		}
	    		catch (Exception ex)
	    		{
	    			ex.printStackTrace();
	    		}
	        }
	    };
	    
	    final OnDismissListener onDialogDismissed = new OnDismissListener()
	    {
			@Override
			public void onDismiss(DialogInterface dialog)
			{
				// DO NOTHING
				alertDialog.dismiss();
			}
	    };
	    
	    // Sets Events
	    btnDownload.setOnClickListener(onButtonDownloadPressed);
	    alertDialog.setOnDismissListener(onDialogDismissed);
		
	    // Shows the Finished Dialog
		alertDialog.show();
	}
	
	// JAVASCRIPT Methods (Can be Called by Impromptu Apps) --------------------------------------------
	@JavascriptInterface
	public void downloadFile(String url)
	{		
		try
		{	
			// Downloads the File
			createDownloadAlert(url);
		}
		catch (Exception ex)
		{
			toast("Error Calling downloadFile(): " + ex.getLocalizedMessage());
		}
	}
	
	@JavascriptInterface
	public void uploadFile(String callbackCommand, String dialogMessage, String[] filetypes)
	{	
		try
		{	
			// Allows User to Select a File and Upload it via SCP
			UploadFileDialog fd = new UploadFileDialog(context, dialogMessage, GCFApplication.DOWNLOAD_FOLDER, filetypes, application.getCloudToolkit(), GCFApplication.UPLOAD_SFTP_PATH, "UPLOAD_COMPLETE");
			
			this.callbackCommand = callbackCommand;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			toast("Error Calling uploadFile(): " + ex.getLocalizedMessage());
		}
	}
	
	@JavascriptInterface
	public void sendComputeInstruction(String command, String[] parameters)
	{
		try
		{
			System.out.println("Sending Compute Instruction: " + command);
			for (AppInfo app : application.getActiveApplications())
			{
				application.getGroupContextManager().sendComputeInstruction(app.getAppContextType(), command, parameters);
			}
		}
		catch (Exception ex)
		{
			toast("Error Calling sendComputeInstruction(): " + ex.getLocalizedMessage());
		}
	}
	
	@JavascriptInterface
	public String getUserContext()
	{
		return application.getGroupContextManager().getBluewaveManager().getPersonalContextProvider().getContext().toString();
	}
	
	@JavascriptInterface
	public String getTelephoneNumber()
	{
		TelephonyManager tMgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		return tMgr.getLine1Number();
	}
			
	@JavascriptInterface
	public void setPreference(String key, String value)
	{		
		try
		{
			application.setPreference(key, value);
		}
		catch (Exception ex)
		{
			toast("Error Calling setPreference(): " + ex.getLocalizedMessage());
		}
	}

	@JavascriptInterface
	public String getPreference(String key)
	{
		return application.getPreference(key);
	}
		
	@JavascriptInterface
	public void removeApplicationFromCatalog()
	{
		application.removeApplicationFromCatalog(activity.getRunningApp());
	}
	
	@JavascriptInterface
	public void finish()
	{
		activity.finish();
	}
	
	@JavascriptInterface
	public void toast(String text)
	{
		final String toastText = text;
		
		// Runs this Code on the Main UI Thread
		handler.post(new Runnable()
		{
			public void run()
			{
				Toast.makeText(application, toastText, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
}
