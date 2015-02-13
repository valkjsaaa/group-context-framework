package com.adefreitas.miscproviders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Class Used to Perform HTTP GET Operations in the Background
 * USAGE:  new HttpAsyncTask().execute("http://hmkcode.appspot.com/rest/controller/get.json");
 * Taken from http://hmkcode.com/android-parsing-json-data/
 * @author adefreit
 */
public class HTTPAsyncTask extends AsyncTask<String, Void, String>
{
	// Constants
	private static final String LOG_NAME = "HTTP_TASK";
	
	// Intent Names
	public static final String REQUEST_COMPLETE = "HTTP_REQUEST_COMPLETE";
	public static final String HTTP_RESPONSE    = "HTTP_RESPONSE";
	
	public static String POST(String url, String body)
	{
        InputStream inputStream = null;
        String      result      = "";
        
        try 
        {
            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();
            
            // create Post
            HttpPost httppost = new HttpPost(url);
            httppost.setEntity(new StringEntity(body));
 
            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(httppost);
 
            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();
 
            // convert inputstream to string
            if(inputStream != null)
            {
            	result = convertInputStreamToString(inputStream);
            }
            else
            {
            	result = "POST Did not work!";
            }
        } 
        catch (Exception e) 
        {
            Log.d(LOG_NAME, e.getLocalizedMessage());
        }
 
        return result;
	}
	
	public static String GET(String url)
	{
        InputStream inputStream = null;
        String      result      = "";
        try 
        {
            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();
 
            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));
 
            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();
 
            // convert inputstream to string
            if(inputStream != null)
            {
            	result = convertInputStreamToString(inputStream);
            }
            else
            {
            	result = "GET Did not work!";
            }
        } 
        catch (Exception e) 
        {
            Log.d(LOG_NAME, e.getLocalizedMessage());
        }
 
        return result;
    }

	public static String PUT(String url, String body)
	{
		InputStream inputStream = null;
        String      result      = "";
        
        try 
        {
            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();
            
            // create put
            HttpPut httpput = new HttpPut(url);
            httpput.setEntity(new StringEntity(body));
            
            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(httpput);
 
            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();
 
            // convert inputstream to string
            if(inputStream != null)
            {
            	result = convertInputStreamToString(inputStream);
            }
            else
            {
            	result = "PUT Did not work!";
            }
        } 
        catch (Exception e) 
        {
            Log.d(LOG_NAME, e.getLocalizedMessage());
        }
 
        return result;
	}
	
	private static String convertInputStreamToString(InputStream inputStream) throws IOException
	{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String 		   line 		  = "";
        String 		   result 		  = "";
        
        while((line = bufferedReader.readLine()) != null)
        {
        	result += line;
        }
            
        inputStream.close();
        return result;
    }
	
	@Override
    protected String doInBackground(String... args) 
	{
		// Format:  { COMMAND, URL, BODY }
		String command = args[0];
		
		if (command.equals("GET"))
		{
			return GET(args[1]);
		}
		else if (command.equals("POST"))
		{
			return POST(args[1], args[2]);
		}
		else if (command.equals("PUT"))
		{
			return PUT(args[1], args[2]);
		}
		
		return null;    
	}
	 
    @Override
    protected void onPostExecute(String result) 
    {
    	Log.d(LOG_NAME, "Received: " + result);
    }    
}
