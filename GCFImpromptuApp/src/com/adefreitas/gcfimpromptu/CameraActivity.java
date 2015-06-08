package com.adefreitas.gcfimpromptu;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.adefreitas.androidframework.toolkit.ImageToolkit;
import com.adefreitas.gcfmagicapp.R;

public class CameraActivity extends Activity 
{		
	// Constants
	public  static final int    IMAGE_HEIGHT 		  = 480;
	public  static final int    IMAGE_WIDTH  		  = 640;
	public  static final String CLOUD_UPLOAD_URL	  = "http://gcf.cmu-tbank.com/snaptoit/upload_image.php";
	private static final String IMAGE_DIRECTORY_NAME  = "SnapToIt";
	private static final String IMAGE_NAME 			  = "sti_photo.jpeg";
	private static final String LOG_NAME 			  = "SnapToIt";

	// Activity request codes
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    public  static final int MEDIA_TYPE_IMAGE = 1;
	
	// Application Link
	private GCFApplication application;
	
	// Intent Filters
	private IntentFilter   intentFilter;
	private IntentReceiver receiver;	
	
	// Controls
	private LinearLayout btnQuit;
	private ImageView	 imgPreview;
	
	// Camera Variables
	private boolean cameraStarted = false;
	private boolean imageTaken    = false;
	private Uri 	fileUri; // file url to store image/video

	// Encoding
	String encodedString = "";
	String stiFilename   = "";
	String archive       = "";
	
	/**
	 * Android Method:  Called when this activity is created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		
		// Saves a Link to the Application
		application = (GCFApplication)this.getApplication();
		
		// Create Intent Filter and Receiver
		// Receivers are Set in onResume() and Removed on onPause()
		this.receiver     = new IntentReceiver();
		this.intentFilter = new IntentFilter();
		
		// Gets Controls
		btnQuit	   = (LinearLayout)this.findViewById(R.id.btnQuit);
		imgPreview = (ImageView)this.findViewById(R.id.imgPreview);
		
		// Sets Event Handlers
		btnQuit.setOnClickListener(onQuitClickListener);
		
		// Determines Whether to Launch the Camera App or Show a Picture
		if (savedInstanceState != null && savedInstanceState.containsKey("IMAGE_TAKEN") && savedInstanceState.getBoolean("IMAGE_TAKEN"))
		{
			imageTaken = savedInstanceState.getBoolean("IMAGE_TAKEN");
		}

		// Determines Whether to Launch the Camera App
		if (savedInstanceState != null && savedInstanceState.containsKey("CAMERA_STARTED"))
		{
			cameraStarted = savedInstanceState.getBoolean("CAMERA_STARTED");
		}
		
		// Displays a Quick Warning if the Device is in Capture Mode
		if (PreferenceManager.getDefaultSharedPreferences(this.getBaseContext()).getBoolean("sti_capture", false))
		{
			Toast.makeText(this, "CAPTURE MODE ENABLED", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Android Method:  Used when an Activity is Resumed
	 */
	protected void onResume()
	{
		super.onResume();
		this.registerReceiver(receiver, intentFilter);
		
		if (!cameraStarted)
		{
			captureImage();	
		}
	}
	
	/**
	 * Android Method:  Used when an Activity is Paused
	 */
	@Override
	protected void onPause() 
	{
	    super.onPause();
	    this.unregisterReceiver(receiver);
	}
	
	/**
	 * Android Method:  Called when the menu is created
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.camera, menu);
		return true;
	}

	/**
	 * Android Method:  Called when a menu item is selected
	 */
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
	 * Android Method:  Called right before the activity is able to be closed 
	 */
	@Override
	protected void onSaveInstanceState(Bundle bundle)
	{
		bundle.putBoolean("IMAGE_TAKEN", imageTaken);
		bundle.putBoolean("CAMERA_STARTED", cameraStarted);
	}
	
	// Camera Methods
	private void captureImage() 
	{
	    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	 
	    File imageFile = ImageToolkit.getOutputMediaFile(IMAGE_DIRECTORY_NAME, IMAGE_NAME);
	    fileUri        = ImageToolkit.getOutputMediaFileUri(imageFile);
	    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
	    intent.putExtra("imageUri", fileUri.toString());
	    
	    // start the image capture Intent
	    startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
	    
	    cameraStarted = true;
	}
	
	/**
	 * Camera Method:  Called after a picture has been taken!
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		// Processes Camera Data
		if(requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK)
		{		
			try
			{
			    ContentResolver cr = this.getContentResolver();
			    
			    try
			    {
			    	File imageFile = ImageToolkit.getOutputMediaFile(IMAGE_DIRECTORY_NAME, IMAGE_NAME);
			    	
			    	// Resizes the Bitmap Image
			    	Bitmap resizedBitmap = ImageToolkit.resizeImage(cr, imageFile, IMAGE_WIDTH, IMAGE_HEIGHT);		    	
			    							
			    	// Determines if we are in Capture Mode
			    	boolean captureMode = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext()).getBoolean("sti_capture", false);
			    	
			    	// Determines the Filename
			    	String filename = (captureMode) ? 
			    			application.getGroupContextManager().getDeviceID() + new Date().getTime() + ".jpeg" :
			    			application.getGroupContextManager().getDeviceID() + ".jpeg";
									    
				    // Uploads or Saves the File, Depending on the Mode
				    if (!captureMode)
				    {
				    	String encodedString = "";
				    	String stiFilename   = "";
				    	String archive       = "";
				    	
				    	application.setPhotoTaken();
				    	//application.getCloudToolkit().uploadFile(CLOUD_FOLDER, file, GCFApplication.ACTION_IMAGE_UPLOADED);
				    	encodeImageToString(resizedBitmap, application.getGroupContextManager().getDeviceID().replace(" ", "%20"), false);
				    	
				    }
				    else
				    {
				    	Toast.makeText(this, "Created : " + filename, Toast.LENGTH_SHORT).show();
				    	
				        // Writes a File Containing the Bitmap Image
				        //File file = ImageToolkit.writeBitmapToJPEG(resizedBitmap, IMAGE_DIRECTORY_NAME, filename);
				    	encodeImageToString(resizedBitmap, application.getGroupContextManager().getDeviceID().replace(" ", "%20"), true);
				    }
			    }
			    catch (Exception ex)
			    {
			        Toast.makeText(this, "Failed to process picture: " + ex.getMessage(), Toast.LENGTH_LONG).show();
			        ex.printStackTrace();
			    }
			}
			catch (Exception ex)
			{
				Toast.makeText(this, "Error Occurred While Taking Picture", Toast.LENGTH_SHORT).show();
				ex.printStackTrace();
			}
		}
		else
		{
			System.out.println("Unknown Request Code: " + requestCode);
		}
		
	    this.finish();
	}

	// Event Handler
	final OnClickListener onQuitClickListener = new OnClickListener() 
	{
        public void onClick(final View v)
        {
        	try
    		{	
	    		finish();
    		}
    		catch (Exception ex)
    		{
    			ex.printStackTrace();
    		}
        }
    };
	
	// Intent Receiver
	private class IntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{				
			Log.e(LOG_NAME, "Unexpected Intent (Action: " + intent.getAction() + ")");	
		}
	}
	
	private void encodeImageToString(final Bitmap bitmap, final String filename, final boolean archive)
	{
		new AsyncTask<Void, Void, String>()
		{
			protected void onPreExecute()
			{
				
			}

			@Override
			protected String doInBackground(Void... params) 
			{
				long startTime = System.currentTimeMillis();
				
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
				byte[] byteArray = stream.toByteArray();
				encodedString = Base64.encodeToString(byteArray, Base64.DEFAULT);
				
				long timeElapsed = System.currentTimeMillis() - startTime;
				
				Log.d(LOG_NAME, "Encoded " + encodedString.length() + " Bytes in " + timeElapsed + "ms");
				return "";
			}
			
			protected void onPostExecute(String msg)
			{
				String url = CLOUD_UPLOAD_URL + "?deviceID=" + filename + "&archive=" + archive;
				application.getHttpToolkit().post(url, "jpeg=" + encodedString, GCFApplication.ACTION_IMAGE_UPLOADED);
			}
		}.execute(null, null, null);
	}
}
