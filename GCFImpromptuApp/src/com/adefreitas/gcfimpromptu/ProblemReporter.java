package com.adefreitas.gcfimpromptu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import com.adefreitas.androidframework.toolkit.ImageToolkit;
import com.adefreitas.androidframework.toolkit.MediaUtility;
import com.adefreitas.gcfmagicapp.R;
 
/**
 * EXPERIMENTAL FEATURE
 * http://codecanyon.net/item/universal-android-webview-app/8431507/faqs/21475
 */
public class ProblemReporter extends Activity 
{
 
    //private Button button;
    private WebView  webView;
    final   Activity activity = this;
     
    private static final int FILECHOOSER_RESULTCODE = 12345;
    
    public void onCreate(Bundle savedInstanceState) {
         
        super.onCreate(savedInstanceState);
         
        setContentView(R.layout.activity_show_web_view);
         
        //Get webview 
        webView = (WebView) findViewById(R.id.webView1);    
		webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		webView.getSettings().setLoadsImagesAutomatically(true);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setDomStorageEnabled(true);
		webView.getSettings().setAllowFileAccess(true);
		//webView.addJavascriptInterface(jsInterface, JSInterface.JAVASCRIPT_OBJECT_NAME);
		//webView.setWebViewClient(new CustomBrowser());	
   
        webView.setWebChromeClient(new CustomChromeClient());
        
        webView.loadUrl("http://www.script-tutorials.com/demos/199/index.html");
        
		// Restores the App State so Long as the App is Running!
		if (savedInstanceState != null)
		{
			webView.restoreState(savedInstanceState);	
		}
    }
 
    ValueCallback<Uri>   mUploadMessage;
    ValueCallback<Uri[]> mFilePathCallback;
    String 				 mCameraPhotoPath;
    Uri      			 imageUri;
    
	/**
	 * Android Method:  Used to Save the Activity's State
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		webView.saveState(outState);
	}
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
    	if (Build.VERSION.SDK_INT >= 21)
    	{
            if(requestCode != FILECHOOSER_RESULTCODE || mFilePathCallback == null) 
            {
                super.onActivityResult(requestCode, resultCode, intent);
                return;
            }

            Uri[] results = null;

            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) 
            {
                if(intent == null) {
                    // If there is not data, then we may have taken a photo
                    if(mCameraPhotoPath != null) 
                    {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                } 
                else 
                {
                    String dataString = intent.getDataString();
                    if (dataString != null) 
                    {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
            return;
        }
    	else
    	{
              if(requestCode==FILECHOOSER_RESULTCODE)
              {  
                  if (null == this.mUploadMessage) 
                  {
                	  return;
                  }

                    Uri result;
                    if (resultCode != RESULT_OK) {
                        result = null;
                    } else {
                        result = intent == null ? this.imageUri : intent.getData(); // retrieve from the private variable if the intent is null
                    }

                    this.mUploadMessage.onReceiveValue(result);
                    this.mUploadMessage = null;
              } 
        }
        
    }
 
    @Override
    // Detect when the back button is pressed
    public void onBackPressed() 
    { 
        if(webView.canGoBack()) {
         
            webView.goBack();
             
        } else {
            // Let the system handle the back button
            super.onBackPressed();
        }
    }
     
    public class CustomChromeClient extends WebChromeClient
    {
     //The undocumented magic method override  
     //Eclipse will swear at you if you try to put @Override here  
     // For Android 3.0+
     @SuppressWarnings("unused")
     public void openFileChooser(ValueCallback<Uri> uploadMsg) 
     {  
    	 Toast.makeText(ProblemReporter.this, "openFileChooser (Android 3+)", Toast.LENGTH_SHORT).show();
    	 
         mUploadMessage = uploadMsg;  
         File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Impromptu");
         // Create the storage directory if it does not exist
         if (!imageStorageDir.exists())
         {
             imageStorageDir.mkdirs();                  
         }
         File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");  
         imageUri = Uri.fromFile(file); 

         final List<Intent> cameraIntents = new ArrayList<Intent>();
         final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
         final PackageManager packageManager = getPackageManager();
         final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
         for(ResolveInfo res : listCam) {
             final String packageName = res.activityInfo.packageName;
             final Intent i = new Intent(captureIntent);
             i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
             i.setPackage(packageName);
             i.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
             cameraIntents.add(i);
         }

         mUploadMessage = uploadMsg; 
         Intent i = new Intent(Intent.ACTION_GET_CONTENT);  
         i.addCategory(Intent.CATEGORY_OPENABLE);  
         i.setType("image/*"); 
         Intent chooserIntent = Intent.createChooser(i,"Image Chooser");
         chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));
         ProblemReporter.this.startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);

    }

     //For Android 3.0+
    @SuppressWarnings("unused")
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture)
    {
    	Toast.makeText(ProblemReporter.this, "openFile (Android 3.0+)", Toast.LENGTH_SHORT).show();
    	
        mUploadMessage = uploadMsg;  
        File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Impromptu");
        // Create the storage directory if it does not exist
        if (! imageStorageDir.exists()){
            imageStorageDir.mkdirs();                  
        }
        File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");  
        imageUri = Uri.fromFile(file); 

        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for(ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent i = new Intent(captureIntent);
            i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            i.setPackage(packageName);
            i.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            cameraIntents.add(i);
        }


        mUploadMessage = uploadMsg; 
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);  
        i.addCategory(Intent.CATEGORY_OPENABLE);  
        i.setType("image/*"); 
        Intent chooserIntent = Intent.createChooser(i,"Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));
        ProblemReporter.this.startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);

    }

    //For Android 4.1
    @SuppressWarnings("unused")
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType)
    {
    	Toast.makeText(ProblemReporter.this, "onShowFileChoser (Android 4.1)", Toast.LENGTH_SHORT).show();
    	
    	mUploadMessage = uploadMsg;  
        File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Impromptu");
        // Create the storage directory if it does not exist
        if (! imageStorageDir.exists()){
            imageStorageDir.mkdirs();                  
        }
        File file = ImageToolkit.getOutputMediaFile("Impromptu", "photoToUpload.jpeg");
        imageUri = Uri.fromFile(file); 

        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for(ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent i = new Intent(captureIntent);
            i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            i.setPackage(packageName);
            i.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            cameraIntents.add(i);
        }

        mUploadMessage = uploadMsg; 
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);  
        i.addCategory(Intent.CATEGORY_OPENABLE);  
        i.setType("image/*"); 
        Intent chooserIntent = Intent.createChooser(i,"Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));
        ProblemReporter.this.startActivityForResult(chooserIntent,  FILECHOOSER_RESULTCODE);

    }
    
     //For Android 5.0+
     public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) 
     {
    	 Toast.makeText(ProblemReporter.this, "onShowFileChoser (Android 5+)", Toast.LENGTH_SHORT).show();
    	 
        // Double check that we don't have any existing callbacks
        if(mFilePathCallback != null) 
        {
            mFilePathCallback.onReceiveValue(null);
        }
        mFilePathCallback = filePathCallback;

        // Set up the take picture intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(ProblemReporter.this.getPackageManager()) != null) 
        {        	
            // Create the File where the photo should go
            File photoFile = ImageToolkit.getOutputMediaFile("Impromptu", "photoToUpload.jpeg");

            // Continue only if the File was successfully created
            if (photoFile != null) 
            {
                mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            } 
            else 
            {
                takePictureIntent = null;
            }
        }

        // Set up the intent to get an existing image
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        // Set up the intents for the Intent chooser
        Intent[] intentArray;
        if(takePictureIntent != null) {
            intentArray = new Intent[]{takePictureIntent};
        } 
        else 
        {
            intentArray = new Intent[0];
        }

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

        startActivityForResult(chooserIntent, ProblemReporter.FILECHOOSER_RESULTCODE);

        return true;
    }
    	
    }
}
