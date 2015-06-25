package com.adefreitas.gcfimpromptu;

import java.io.ByteArrayOutputStream;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.adefreitas.androidframework.AndroidCommManager;
import com.adefreitas.androidframework.toolkit.HttpToolkit;
import com.adefreitas.androidframework.toolkit.ImageToolkit;
import com.adefreitas.androidproviders.LocationContextProvider;
import com.adefreitas.gcfmagicapp.R;

public class ProblemReport extends ActionBarActivity implements SurfaceHolder.Callback 
{
	// Constants
	private static final String LOG_NAME		 	     = "ProblemReport";
	public  static final String CLOUD_UPLOAD_URL 	     = "http://gcf.cmu-tbank.com/apps/creationfest/upload_image.php";
	public  static final String ACTION_PROBLEM_SUBMITTED = "Problem Submitted";
	
	// Application Link
	private GCFApplication application;
	
	// Intent Filters
	private IntentFilter   intentFilter;
	private IntentReceiver receiver;
	
	// Controls
	private Toolbar  toolbar;
	private Button	 btnCamera;
	private Button	 btnSubmit;
	private EditText txtDescription;
	private EditText txtPhone;
	private TextView txtCamera;
	private TextView txtLocation;
	private Spinner  spinner;
	
	// Camera Surface
	private int			  cameraID       = -1;
	private String		  encodedString  = "";
	private boolean		  previewRunning = false;
	private Camera        camera;
	private SurfaceView   cameraSurface;
	private SurfaceHolder surfaceHolder;

	/**
	 * Android Method:  Used when the Activity is Created
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_problem_report);
		
		// Saves a Link to the Application
		application = (GCFApplication)this.getApplication();
		
		// Create Intent Filter and Receiver
		// Receivers are Set in onResume() and Removed on onPause()
		this.receiver     = new IntentReceiver();
		this.intentFilter = new IntentFilter();
		this.intentFilter.addAction(ACTION_PROBLEM_SUBMITTED);
				
		// Gets Controls
		toolbar  	   = (Toolbar)this.findViewById(R.id.toolbar);
		btnCamera 	   = (Button)this.findViewById(R.id.btnCamera);
		btnSubmit 	   = (Button)this.findViewById(R.id.btnSubmit);
		txtDescription = (EditText)this.findViewById(R.id.txtDescription);
		txtPhone       = (EditText)this.findViewById(R.id.txtPhone);
		txtCamera      = (TextView)this.findViewById(R.id.txt_camera);
		txtLocation    = (TextView)this.findViewById(R.id.txtLocation);
		spinner		   = (Spinner)this.findViewById(R.id.spinner);
		
		// Sets Up Event Handlers
		txtPhone.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
		btnCamera.setOnClickListener(onCameraClickListener);
		btnSubmit.setOnClickListener(onSubmitClickListener);
		
		// Initializes Camera Surface
		cameraSurface = (SurfaceView)findViewById(R.id.surface_camera);
		surfaceHolder = cameraSurface.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		// Initializes the Spinner
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.departments, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		
		// Initializes Telephone Number
		TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
		if (tMgr.getLine1Number() != null)
		{
			txtPhone.setText(tMgr.getLine1Number());
		}
		
		// Sets Up the Toolbar
		this.setSupportActionBar(toolbar);
		this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	/**
	 * Android Method:  Used when an Activity is Resumed
	 */
	protected void onResume()
	{
		super.onResume();
		this.registerReceiver(receiver, intentFilter);
		
		// Notifies the Application to Forward GCF Message to this View
		this.application.setInForeground(true);
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

	/**
	 * Android Method:  Used when the Menu is Created
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.problem_report, menu);
		return true;
	}

	/**
	 * Android Method:  Used when a Menu Item is Selected
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		if (item.toString().equalsIgnoreCase(this.getString(R.string.title_activity_settings)))
	    {
	    	Intent intent = new Intent(this, SettingsActivity.class);
	    	this.startActivity(intent);
	    }
		
		return false;
	}
	
	// CAMERA METHODS -----------------------------------------------------------------------------	
	@SuppressWarnings("deprecation")
	Camera.PictureCallback pictureCallback = new Camera.PictureCallback() 
	{
		public void onPictureTaken(byte[] imageData, Camera c) 
		{
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
					btnCamera.setText("Retake");
				}
		  		catch (Exception ex)
		  		{
		  			Toast.makeText(ProblemReport.this, "Picture Failed!", Toast.LENGTH_SHORT).show();
		  		}
		  	}
		  	else if (camera != null)
		  	{
		  		previewRunning = false;
		  		encodedString = "";
		  		camera.stopPreview();
		  		camera.startPreview();
		  		btnCamera.setText("Take Picture");
		  	}
		}
    };
    
    final OnClickListener onSubmitClickListener = new OnClickListener() 
    {
		@Override
		public void onClick(View v) 
		{			
		  	if (txtDescription.getText().toString().trim().length() == 0)
		  	{
		  		Toast.makeText(ProblemReport.this, "Cannot Submit Without a Description", Toast.LENGTH_LONG).show();
		  	}
		  	else if (txtLocation.getText().toString().trim().length() == 0)
		  	{
		  		Toast.makeText(ProblemReport.this, "Must Provide a Location", Toast.LENGTH_LONG).show();
		  	}
		  	else
		  	{
		  		LocationContextProvider locationProvider = (LocationContextProvider)application.getGroupContextManager().getContextProvider("LOC");
		  		
		  		String descriptionParam = "description=" + Uri.encode(txtDescription.getText().toString());
		  		String latitudeParam    = "latitude=" + locationProvider.getLatitude();
		  		String longitudeParam   = "longitude=" + locationProvider.getLongitude();
		  		String telephoneParam   = "telephone=" + Uri.encode(txtPhone.getText().toString());
		  		String departmentParam  = "department=" + Uri.encode(spinner.getSelectedItem().toString());
		  		String locationParam    = "location=" + Uri.encode(txtLocation.getText().toString());
		  		
				String url = String.format("%s?%s&%s&%s&%s&%s&%s", CLOUD_UPLOAD_URL, descriptionParam, latitudeParam, longitudeParam, telephoneParam, departmentParam, locationParam);
				System.out.println(url);
				application.getHttpToolkit().post(url, "jpeg=" + encodedString, ACTION_PROBLEM_SUBMITTED);
		  		
				Toast.makeText(ProblemReport.this, "Sending . . . Please Wait!", Toast.LENGTH_LONG).show();
		  	}
		}
    };
	
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
			cameraSurface.setVisibility(View.GONE);
			btnCamera.setVisibility(View.GONE);
			txtCamera.setVisibility(View.GONE);
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
	
	// Async Task to Encode JPEG Image to a String
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
				Bitmap resizedBitmap = ImageToolkit.resizeImage(bitmap, 640, 480);
				
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
//				String url = CLOUD_UPLOAD_URL + "?timestamp=" + System.currentTimeMillis();
//				application.getHttpToolkit().post(url, "jpeg=" + encodedString, GCFApplication.ACTION_IMAGE_UPLOADED);
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
			if (intent.getAction().equals(ACTION_PROBLEM_SUBMITTED))
			{
				Toast.makeText(context, intent.getStringExtra(HttpToolkit.HTTP_RESPONSE), Toast.LENGTH_SHORT).show();
				finish();
			}
			else
			{
				Log.e(LOG_NAME, "Unexpected Intent (Action: " + intent.getAction() + ")");	
			}
		}
	}

}
