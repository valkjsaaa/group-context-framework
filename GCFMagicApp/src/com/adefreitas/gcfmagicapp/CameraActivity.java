package com.adefreitas.gcfmagicapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class CameraActivity extends Activity 
{		
	// Constants
	public  static final int    IMAGE_HEIGHT 		 = 480;
	public  static final int    IMAGE_WIDTH  		 = 640;
	public  static final String CLOUD_FOLDER 		 = "/var/www/html/gcf/universalremote/";
	private static final String IMAGE_DIRECTORY_NAME = "SnapToIt";
	private static final String LOG_NAME 			 = "SnapToIt";
	
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
	private boolean imageTaken = false;
	private Uri 	fileUri; // file url to store image/video
	
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
		
		// Displays a Quick Warning
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
		
		captureImage();
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
	}
	
	// Camera Methods
	private void captureImage() 
	{
	    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	 
	    fileUri = getOutputMediaFileUri();
	    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
	    intent.putExtra("imageUri", fileUri.toString());
	    
	    // start the image capture Intent
	    startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
	}
	
	/**
	 * Camera Method:  Called after a picture has been taken!
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		// Processes Camera Data
		if(requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK)
		{		
			try
			{
			    ContentResolver cr = this.getContentResolver();
			    
			    try
			    {
			    	Bitmap originalBitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, getOutputMediaFileUri());
			    	
			    	// Resizes the Bitmap Image
			    	Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, IMAGE_WIDTH, IMAGE_HEIGHT, true);			    	
			    							
			    	// Determines if we are in Capture Mode
			    	boolean captureMode = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext()).getBoolean("sti_capture", false);
			    	
			    	// Determines the Filename
			    	String filename = (captureMode) ? 
			    			application.getGroupContextManager().getDeviceID() + new Date().getTime() + ".jpeg" :
			    			application.getGroupContextManager().getDeviceID() + ".jpeg";
			    	
			        // Writes a File Containing the Bitmap Image
			        ByteArrayOutputStream stream = new ByteArrayOutputStream();
					resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);	
					File dir = getPictureDirectory();
					
			    	System.out.println("Created file: " + dir.getAbsolutePath() + "/" + filename);
					
				    File 			 file = new File(dir.getAbsolutePath() + "/" + filename);
				    FileOutputStream fos  = new FileOutputStream(file);
				    fos.write(stream.toByteArray());
				    fos.close();
					
				    // Uploads or Saves the File, Depending on the Mode
				    if (!captureMode)
				    {
				    	application.setPhotoTaken();
				    	application.getCloudToolkit().uploadFile(CLOUD_FOLDER, file);
				    }
				    else
				    {
				    	Toast.makeText(this, "Created : " + filename, Toast.LENGTH_LONG).show();
				    }
				    
				    // Makes the File Visible!
				    MediaScannerConnection.scanFile(this, new String[] { file.getAbsolutePath() }, null, null);
			    }
			    catch (Exception e)
			    {
			        Toast.makeText(this, "Failed to process picture: " + e.getMessage(), Toast.LENGTH_LONG).show();
			        e.printStackTrace();
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
	
    /**
     * Display image from a path to ImageView
     */
    private void previewCapturedImage() 
    {
        try 
        {
            imgPreview.setVisibility(View.VISIBLE);
 
            // bimatp factory
            BitmapFactory.Options options = new BitmapFactory.Options();
 
            // downsizing image as it throws OutOfMemory Exception for larger
            // images
            options.inSampleSize = 8;
 
            final Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath(), options);
 
            imgPreview.setImageBitmap(bitmap);
        } 
        catch (NullPointerException e) 
        {
            e.printStackTrace();
        }
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

	// Helper Methods
	/**
	 * Creating file uri to store image/video
	 */
	public Uri getOutputMediaFileUri() 
	{
	    return Uri.fromFile(getOutputMediaFile());
	}
	 
	/**
	 * returning image
	 */
	private static File getOutputMediaFile() 
	{
	    // External sdcard location
	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), IMAGE_DIRECTORY_NAME);
	 
	    // Create the storage directory if it does not exist
	    if (!mediaStorageDir.exists()) 
	    {
	        if (!mediaStorageDir.mkdirs()) 
	        {
	            Log.d(LOG_NAME, "Oops! Failed create " + IMAGE_DIRECTORY_NAME + " directory");
	            return null;
	        }
	    }
	 
	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
	    File   mediaFile = new File(mediaStorageDir.getPath() + File.separator + "sti_photo.jpg");
	 
	    return mediaFile;
	}

	private File getPictureDirectory()
	{
		File   storageDir = null;
		String dirPath    = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), IMAGE_DIRECTORY_NAME).getAbsolutePath() + "/";
	      
	      if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
	            storageDir = new File(dirPath);
	            if (storageDir != null) {
	                  if (!storageDir.mkdirs()) {
	                        if (!storageDir.exists()){
	                              Log.e("DropboxToolkit", "failed to create directory: " + dirPath);
	                              return null;
	                        }
	                  }
	            }    
	      } 
	      else 
	      {
	            Log.e("DropboxToolkit", "External storage is not mounted READ/WRITE.");
	      }
	      return storageDir;
	}
}
