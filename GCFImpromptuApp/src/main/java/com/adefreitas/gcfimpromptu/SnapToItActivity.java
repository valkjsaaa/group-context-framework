package com.adefreitas.gcfimpromptu;

import java.io.ByteArrayOutputStream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.gcf.android.AndroidGroupContextManager;
import com.adefreitas.gcf.android.toolkit.HttpToolkit;
import com.adefreitas.gcf.android.toolkit.ImageToolkit;
import com.adefreitas.gcf.messages.ContextData;
import com.adefreitas.gcf.messages.ContextRequest;
import com.adefreitas.gcfmagicapp.R;

public class SnapToItActivity extends ActionBarActivity implements SurfaceHolder.Callback  {

	// Constants
	private static final String LOG_NAME		 	   = "SnapToIt";
	public  static final String CLOUD_UPLOAD_URL 	   = "http://gcf.cmu-tbank.com/snaptoit/upload_image.php";
	public  static final String ACTION_PHOTO_SUBMITTED = "STI Photo Submitted";
	public  static final int    IMAGE_WIDTH			   = 640;
	public  static final int    IMAGE_HEIGHT		   = 480;
	
	// Application Link
	private GCFApplication application;
	
	// Intent Filters
	private IntentFilter   intentFilter;
	private IntentReceiver receiver;
	
	// Controls
	private Toolbar  toolbar;
	private Button	 btnCamera;
	private EditText txtObject;
	private TextView txtCompass;
	
	// Camera Orientation
	private double photoAzimuth;
	private double photoPitch;
	private double photoRoll;
	private double currentAzimuth;
	private double currentPitch;
	private double currentRoll;
	private double currentAccuracy;
	private RelativeLayout layoutCamera;
	
	// Camera Surface
	private Camera        camera;
	private int			  cameraID       = -1;
	private String		  encodedString  = "";
	private boolean		  previewRunning = false;
	private SurfaceView   cameraSurface;
	private SurfaceHolder surfaceHolder;
	
	/**
	 * Android Method:  Used when an Activity is Created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_snap_to_it);
		
		// Saves a Link to the Application
		application = (GCFApplication)this.getApplication();
		
		// Gets Controls
		toolbar    = (Toolbar)this.findViewById(R.id.toolbar);
		btnCamera  = (Button)this.findViewById(R.id.btnCamera);
		txtObject  = (EditText)this.findViewById(R.id.txtObject);
		txtCompass = (TextView)this.findViewById(R.id.txt_compass);
		layoutCamera = (RelativeLayout)this.findViewById(R.id.layoutCamera);
		
		// Sets Up the Toolbar
		this.setSupportActionBar(toolbar);
		this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Initializes Camera Surface
		cameraSurface = (SurfaceView)findViewById(R.id.surface_camera);
		surfaceHolder = cameraSurface.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		// Initializes Orientation
		photoAzimuth = Double.NaN;
		photoPitch   = Double.NaN;
		photoRoll    = Double.NaN;
		
		// Create Intent Filter and Receiver
		// Receivers are Set in onResume() and Removed on onPause()
		this.receiver     = new IntentReceiver();
		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(ACTION_PHOTO_SUBMITTED);
		this.intentFilter.addAction(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED);
		
		// Sets Up Event Handlers
		btnCamera.setOnClickListener(onCameraClickListener);
	}

	/**
	 * Android Method:  Used when an Activity is Resumed
	 */
	protected void onResume()
	{
		super.onResume();
		this.registerReceiver(receiver, intentFilter);
		
		// Notifies the Application that the Window is Active
		this.application.setInForeground(true);
		
		application.getGroupContextManager().sendRequest("COMPASS", ContextRequest.LOCAL_ONLY, new String[0], 500, new String[0]);
	}
	
	/**
	 * Android Method:  Used when an Activity is Paused
	 */
	@Override
	protected void onPause() 
	{
	    super.onPause();
	    this.application.setInForeground(false);
	    this.unregisterReceiver(receiver);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.snap_to_it, menu);
		return true;
	}

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

	// CAMERA METHODS -----------------------------------------------------------------------------	
	@SuppressWarnings("deprecation")
	Camera.PictureCallback pictureCallback = new Camera.PictureCallback() 
	{
		public void onPictureTaken(byte[] imageData, Camera c) 
		{
			photoAzimuth = currentAzimuth;
			photoPitch   = currentPitch;
			photoRoll    = currentRoll;
			encodeImageToString(imageData);
		}
	};
	
	private int findBackFacingCamera() 
	{
	    int cameraId = -1;
	    
	    // Search for the front facing camera
	    int numberOfCameras = Camera.getNumberOfCameras();
	    
	    for (int i = 0; i < numberOfCameras; i++) 
	    {
	      CameraInfo info = new CameraInfo();
	      Camera.getCameraInfo(i, info);
	      if (info.facing == CameraInfo.CAMERA_FACING_BACK) 
	      {
	        cameraId = i;
	        break;
	      }
	    }
	    
	    return cameraId;
	  }
	
	// SURFACE METHODS ----------------------------------------------------------------------------
	@SuppressWarnings("deprecation")
	@Override
	public void surfaceCreated(SurfaceHolder holder) 
	{
		//Toast.makeText(this, "Surface Created", Toast.LENGTH_SHORT).show();
		
		try
		{
			cameraID = findBackFacingCamera();
			camera = Camera.open(cameraID);
		}
		catch (Exception ex)
		{
			Toast.makeText(this, "Camera Failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) 
	{				
		if (camera != null)
		{
			if (previewRunning)
			{
				camera.stopPreview();
			}
			
			try
			{
				Camera.Parameters p = camera.getParameters();
				
				android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
			    android.hardware.Camera.getCameraInfo(cameraID, info);
			    int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
			    int degrees = 0;
			    switch (rotation) 
			    {
			    	case Surface.ROTATION_0:   degrees = 0;   break;
			        case Surface.ROTATION_90:  degrees = 90;  break;
			        case Surface.ROTATION_180: degrees = 180; break;
			        case Surface.ROTATION_270: degrees = 270; break;
			    }
		        
			    int result;
			    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) 
			    {
			    	result = (info.orientation + degrees) % 360;
			        result = (360 - result) % 360;  // compensate the mirror
			    }
			    else 
			    {  // back-facing
			         result = (info.orientation - degrees + 360) % 360;
			    }
			    camera.setDisplayOrientation(result);
			    
		        p.setPreviewSize(640, 480);
				camera.setParameters(p);
				camera.setPreviewDisplay(surfaceHolder);
				camera.startPreview();
				previewRunning = true;
			}
			catch (Exception ex)
			{
				Toast.makeText(this, "Problem Setting Preview Display: " + ex.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) 
	{
		//Toast.makeText(this, "Surface Destroyed", Toast.LENGTH_SHORT).show();
		
		if (camera != null)
		{
			camera.stopPreview();
			previewRunning = false;
			camera.release();	
		}
	}
	
	// EVENT HANDLERS -----------------------------------------------------------------------------
	final OnClickListener onCameraClickListener = new OnClickListener() 
    {
		@Override
		public void onClick(View v) 
		{			
			if (camera != null && encodedString.length() == 0)
		  	{
				try
				{
					camera.takePicture(null, null, pictureCallback);
				}
		  		catch (Exception ex)
		  		{
		  			Toast.makeText(SnapToItActivity.this, "Picture Failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
		  		}
		  	}
			else
			{
				Toast.makeText(SnapToItActivity.this, "Picture Failed!", Toast.LENGTH_SHORT).show();
			}
		}
    };
    	
	/**
	 * Asynchronous Task to Encode an Image to a String
	 * @param byteArray
	 */
	private void encodeImageToString(final byte[] byteArray)
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
				
				Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray , 0, byteArray.length);
				Bitmap resizedBitmap = ImageToolkit.resizeImage(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT);
				
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				resizedBitmap.compress(CompressFormat.JPEG, 100, stream);
				
				encodedString = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);
				
				encodedString = Uri.encode(encodedString);
				
				long timeElapsed = System.currentTimeMillis() - startTime;
				
				Log.d(LOG_NAME, "Encoded " + encodedString.length() + " Bytes in " + timeElapsed + "ms");
				return "";
			}
			
			protected void onPostExecute(String msg)
			{
				String url = CLOUD_UPLOAD_URL 
						+ "?deviceID=" + Uri.encode(application.getGroupContextManager().getDeviceID()) 
						+ "&timestamp=" + System.currentTimeMillis() 
						+ "&azimuth=" + currentAzimuth
						+ "&pitch=" + currentPitch
						+ "&roll=" + currentRoll
						+ "&object=" + Uri.encode(txtObject.getText().toString());
				
				application.getHttpToolkit().post(url, "jpeg=" + encodedString, ACTION_PHOTO_SUBMITTED);
			}
		}.execute(null, null, null);
	}
	
	// Processes Intents
	private class IntentReceiver extends BroadcastReceiver
	{
		/**
		 * This is the method that is called when an intent is received
		 */
		@Override
		public void onReceive(Context context, Intent intent) 
		{				
			if (intent.getAction().equals(ACTION_PHOTO_SUBMITTED))
			{
				onPhotoSubmitted(context, intent);
			}
			else if (intent.getAction().equals(AndroidGroupContextManager.ACTION_GCF_DATA_RECEIVED))
			{
				onContextDataReceived(context, intent);
			}
			else
			{
				Log.e(LOG_NAME, "Unexpected Intent (Action: " + intent.getAction() + ")");	
			}
		}
		
		private void onPhotoSubmitted(Context context, Intent intent)
		{		
			String photoURL = intent.getStringExtra(HttpToolkit.EXTRA_HTTP_RESPONSE).replace(" ", "%20");
			Log.d("ASDF", "Response: " + photoURL);
			
			double azimuth  = (photoAzimuth != Double.NaN) ? photoAzimuth : currentAzimuth;
			double pitch    = (photoPitch   != Double.NaN) ? photoPitch   : currentAzimuth;
			double roll     = (photoRoll    != Double.NaN) ? photoRoll    : currentAzimuth;
			
			Intent i = new Intent(GCFApplication.ACTION_IMAGE_UPLOADED);
			i.putExtra("UPLOAD_PATH", photoURL);
			i.putExtra("AZIMUTH", azimuth);
			i.putExtra("PITCH", pitch);
			i.putExtra("ROLL", roll);
			i.putExtra("APPLIANCE_NAME", txtObject.getText().toString().trim());
			
			photoAzimuth = Double.NaN;
			photoPitch   = Double.NaN;
			photoRoll    = Double.NaN;			
			
			if (txtObject.getText().length() == 0)
			{
				finish();
			}
			else
			{
				//i.putExtra("UPLOAD_PATH", photoURL);//"http://gcf.cmu-tbank.com/snaptoit/photos/" + Uri.encode(application.getGroupContextManager().getDeviceID()) + ".jpeg");
		  		previewRunning = false;
		  		encodedString = "";
		  		camera.stopPreview();
		  		camera.startPreview();
			}
			
			// Sends the Broadcast
			Log.d("IMPROMPTU", "Sending " + GCFApplication.ACTION_IMAGE_UPLOADED);
			application.sendBroadcast(i);
		}
		
		private void onContextDataReceived(Context context, Intent intent)
		{
			// Extracts the values from the intent
			String   contextType = intent.getStringExtra(ContextData.CONTEXT_TYPE);
			String   deviceID    = intent.getStringExtra(ContextData.DEVICE_ID);
			String[] values      = intent.getStringArrayExtra(ContextData.PAYLOAD);
						
			ContextData data = new ContextData(contextType, deviceID, values);
			
			// Forwards Values to the ContextReceiver for Processing
			if (contextType.equals("COMPASS"))
			{
				currentAzimuth  = (Double.valueOf(data.getPayload("AZIMUTH")));
				currentPitch    = (Double.valueOf(data.getPayload("PITCH")));
				currentRoll     = (Double.valueOf(data.getPayload("ROLL")));
				currentAccuracy = (Double.valueOf(data.getPayload("ACCURACY")));
				txtCompass.setText(String.format("Azimuth: %1.0f, Pitch: %1.0f, Accuracy=%1.1f", currentAzimuth, currentPitch, currentAccuracy));
				
				if (currentAccuracy < 3.0)
				{
					btnCamera.setEnabled(false);
					btnCamera.setBackgroundColor(0xFF333333);
					btnCamera.setText("Compass Needs Calibration");
					txtCompass.setBackgroundColor(0xFF993333);
				}
				else
				{
					btnCamera.setEnabled(true);
					btnCamera.setBackgroundColor(0xFFFF8000);
					btnCamera.setText("Take Photo");
					txtCompass.setBackgroundColor(0xFFFF8000);
				}
			}
		}
	}

	
}
