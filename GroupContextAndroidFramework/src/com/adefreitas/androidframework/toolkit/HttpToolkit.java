package com.adefreitas.androidframework.toolkit;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.content.ContextWrapper;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Class Used to Perform HTTP Operations in the Background
 * @author adefreit
 */
public class HttpToolkit
{
	// Constants
	private static final boolean DEBUG			   = false;
	private static final String  LOG_NAME     	   = "HTTP_TOOLKIT";
	private static final String  GET_COMMAND  	   = "GET";
	private static final String  GET_BYTES_COMMAND = "GET_BYTES";
	private static final String  POST_COMMAND 	   = "POST";
	private static final String  PUT_COMMAND	   = "PUT";
	private static final String  DOWNLOAD_COMMAND  = "DOWNLOAD";
	
	// Intent Extra Values
	public static final String HTTP_REQUEST_COMPLETE = "HTTP_REQUEST_COMPLETE";
	public static final String HTTP_RESPONSE 		 = "HTTP_RESPONSE";
	public static final String HTTP_RESPONSE_BYTES   = "HTTP_RESPONSE_BYTES";
	
	// A Broadcaster for Intents
	private ContextWrapper cw;
	
	// A Handler
	private HttpHandler httpHandler;
	
	public HttpToolkit(ContextWrapper cw)
	{
		this.cw 		 = cw;
		this.httpHandler = new HttpHandler(cw);
	}
	
	public void get(final String url, final String callbackIntent)
	{		
		Log.d(LOG_NAME, "Getting: " + url);
		HttpJob job = new HttpJob(GET_COMMAND, url, null, callbackIntent);
		
		Thread httpThread = new HttpRequestThread(job, httpHandler);
		httpThread.start();
	}
	
	public void getBytes(final String url, final String callbackIntent)
	{
		Log.d(LOG_NAME, "Getting Bytes: " + url);
		HttpJob job = new HttpJob(GET_BYTES_COMMAND, url, null, callbackIntent);
		
		Thread httpThread = new HttpRequestThread(job, httpHandler);
		httpThread.start();
	}
	
	public void post(final String url, final String body, final String callbackIntent)
	{
		Log.d(LOG_NAME, "Posting " + url);
		HttpJob job = new HttpJob(POST_COMMAND, url, body, callbackIntent);

		Thread httpThread = new HttpRequestThread(job, httpHandler);
		httpThread.start();
	}
	
	public void put(final String url, final String body, final String callbackIntent)
	{
		Log.d(LOG_NAME, "Putting " + url);
		HttpJob job = new HttpJob(PUT_COMMAND, url, body, callbackIntent);

		Thread httpThread = new HttpRequestThread(job, httpHandler);
		httpThread.start();
	}
	
	public void download(final String url, final String destination, final String callbackIntent)
	{
		Log.d(LOG_NAME, "Downloading " + url);
		HttpJob job = new HttpJob(DOWNLOAD_COMMAND, url, destination, callbackIntent);
		
		Thread httpThread = new HttpRequestThread(job, httpHandler);
		httpThread.start();
	}
	
	private static void log(String tag, String text)
	{
		if (DEBUG)
		{
			Log.d(tag, text);
		}
	}
	
	private static String convertInputStreamToString(InputStream inputStream) throws IOException
	{        
        StringBuilder sb   = new StringBuilder();
        int 		  next = -1;
        
        while ((next = inputStream.read()) != -1)
        {
        	char c = (char)next;
        	sb.append(c);
        }
            
        inputStream.close();

        return sb.toString();
    }
	
	private static Byte[] convertInputStreamToBytes(InputStream inputStream, long size) throws IOException
	{        
		// TODO
		return new Byte[0];
    }
	
	private static class HttpHandler extends Handler
	{	
		private final WeakReference<ContextWrapper> wrapper; 

		public HttpHandler(ContextWrapper wrapper) 
		{
			this.wrapper = new WeakReference<ContextWrapper>(wrapper);
	    }
		
		public void handleMessage(Message msg)
		{			
			if (msg.obj instanceof HttpJob)
			{
				HttpJob job = (HttpJob)msg.obj;
				
				log(LOG_NAME, "HTTP " + job.getCommand() + ":  " + job.getURL());
			    log(LOG_NAME, "BODY: " + job.getArgument());
			    log(LOG_NAME, "HTTP RESPONSE: " + job.getResponse());	
		    	
		    	if (job.getResponse() != null)
		    	{
	            	// Creates the Intent
	        	 	Intent dataDeliveryIntent = new Intent(job.getCallback());
	        	 	log(LOG_NAME, "PREPARING CALLBACK: " + job.getCallback());
	        	 	
	        	 	// Includes the HTTP Response
	        	 	dataDeliveryIntent.putExtra(HTTP_RESPONSE, job.getResponse());
	        	 	
	        	 	// Includes the Bytes (if Available)
	        	 	if (job.getBytes() != null)
	        	 	{
		        	 	dataDeliveryIntent.putExtra(HTTP_RESPONSE_BYTES, job.getBytes());	
	        	 	}
	        	 	
	        	 	// Delivers the HTTP Response
	        	 	wrapper.get().sendBroadcast(dataDeliveryIntent);
		    	}
		    	
		    	log(LOG_NAME, "TIME ELAPSED: [" + (new Date().getTime() - job.getCreationDate().getTime()) + " ms]");	
			}
		}
	}
	
	/**
	 * Represents a Single HTTP Request
	 * @author adefreit
	 *
	 */
	private class HttpJob
	{
		private String command;
		private String url;
		private String argument;
		private String callback;
		private String response;
		private Date   dateCreated;
		private Byte[] bytes;
		
		public HttpJob(String command, String url, String body, String callback)
		{
			this.command     = command;
			this.url 	     = url;
			this.argument 	     = body;
			this.callback    = callback;
			this.response    = null;
			this.dateCreated = new Date();
		}
	
		public void setResponse(String response)
		{
			this.response = response;
		}
		
		public void setBytes(Byte[] response)
		{
			bytes = response;
		}
		
		public Byte[] getBytes()
		{
			return bytes;
		}
		
		public String getCommand()
		{
			return command;
		}
		
		public String getURL()
		{
			return url;
		}
		
		public String getArgument()
		{
			return argument;
		}
		
		public String getCallback()
		{
			return callback;
		}
	
		public String getResponse()
		{
			return response;
		}
		
		public String toString()
		{
			return String.format("HTTP %s [url: %s; arg: %s; callback: %s]", getCommand(), getURL(), getArgument() != null, getCallback());
		}
	
		public Date getCreationDate()
		{
			return dateCreated;
		}
	}

	private class HttpRequestThread extends Thread
	{
		private HttpJob job;
		private Handler handler;
		
		public HttpRequestThread(HttpJob job, Handler handler)
		{
			this.job     = job;
			this.handler = handler;
		}
		
		public void run()
		{
			String response = null;
			
			// Determines What to Do Based Off of the Command
			if (job.getCommand().equals(GET_COMMAND))
			{
				response = httpGet(false);
			}
			else if (job.getCommand().equals(GET_BYTES_COMMAND))
			{
				response = httpGet(true);
			}
			else if (job.getCommand().equals(POST_COMMAND))
			{
				response = httpPost();
			}
			else if (job.getCommand().equals(PUT_COMMAND))
			{
				response = httpPut();
			}
			else if (job.getCommand().equals(DOWNLOAD_COMMAND))
			{
				response = httpDownload();
			}
			
			// Sets the HTTP Response (OK if NULL)
			job.setResponse(response);
			
			// Sends the Received Message to a Handler to be Processed
			Message m = Message.obtain();
			m.obj     = job;
			handler.sendMessage(m);
		}
		
		public HttpClient newInstance(String userAgent)
	    {
	        HttpParams params = new BasicHttpParams();

	        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	        HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
	        HttpProtocolParams.setUseExpectContinue(params, true);

	        HttpConnectionParams.setStaleCheckingEnabled(params, false);
	        HttpConnectionParams.setConnectionTimeout(params, 10000);
	        HttpConnectionParams.setSoTimeout(params, 10000);
	        HttpConnectionParams.setSocketBufferSize(params, 8192);
			
	        HttpClient client = AndroidHttpClient.newInstance("Android");

	        return client;
	    }
		
		private String httpGet(boolean getBytes)
		{
			InputStream  inputStream = null;
			HttpClient   httpclient  = null;
			String		 response    = null;
			
			try 
	        {
	            // create HttpClient
	        	HttpParams params = new BasicHttpParams();
	        	params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
	        	HttpConnectionParams.setConnectionTimeout(params, 5000);
	        	HttpConnectionParams.setSoTimeout(params, 5000); 
				httpclient = newInstance("Android");
				
	            // make GET request to the given URL
				HttpGet request = new HttpGet(job.getURL());
				request.setParams(params);
	        	HttpResponse httpResponse = httpclient.execute(request);
	            
	            // receive response as inputStream
	            if (httpResponse != null)
	            {
	            	inputStream = httpResponse.getEntity().getContent();
	            }
	            
	            // convert inputstream to string
	            if(inputStream != null)
	            {	       
	            	if (getBytes)
	            	{
	            		job.setBytes(convertInputStreamToBytes(inputStream, httpResponse.getEntity().getContentLength()));
	            	}
	            	else
	            	{
	            		response = convertInputStreamToString(inputStream);
	            	}
	            }
	            else
	            {
	            	Log.d(LOG_NAME, "GET Did not work!");
	            }
	            
	            httpResponse.getEntity().consumeContent();
	        } 
	        catch (Exception e) 
	        {
	        	if (e.getMessage() != null)
	        	{
	        		Log.d(LOG_NAME, e.getMessage());
	        	}
	        	else
	        	{
	        		e.printStackTrace();
	        	}
	        }

			try
			{
				if (inputStream != null)
				{
					inputStream.close();
				}
			}
			catch (Exception ex)
			{
				Log.d(LOG_NAME, ex.getLocalizedMessage());
			}
			
			try
			{
				
				if (httpclient != null)
				{
					httpclient.getConnectionManager().shutdown();
					((AndroidHttpClient)httpclient).close();
				}
			}
			catch (Exception ex)
			{
				Log.d(LOG_NAME, ex.getLocalizedMessage());
			}
			
			return response;
		}

		private String httpPost()
		{
			InputStream inputStream  = null;
			HttpClient  httpclient   = null;
			String		response     = null;
			
			try 
	        {
	            // create HttpClient and Set Parameters
	        	HttpParams params = new BasicHttpParams();
	        	params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
	        	HttpConnectionParams.setConnectionTimeout(params, 5000);
	        	HttpConnectionParams.setSoTimeout(params, 5000);

				httpclient = newInstance("Android");
	            
	            // create Post
	            HttpPost httppost = new HttpPost(job.getURL());
	            httppost.setParams(params);
	            httppost.addHeader("content-type", "application/x-www-form-urlencoded");
	            httppost.setEntity(new StringEntity(job.getArgument()));
	 
	            // make POST request to the given URL
	            HttpResponse httpResponse = httpclient.execute(httppost);
	            
	            // receive response as inputStream
	            if (httpResponse != null)
	            {
	            	inputStream = httpResponse.getEntity().getContent();
	            }
	            	            
	            // convert inputstream to string
	            if(inputStream != null)
	            {
	            	response = convertInputStreamToString(inputStream);
	            }
	            else
	            {
	            	Log.d(LOG_NAME, "POST Did not work!");
	            }
	            
	        	httpResponse.getEntity().consumeContent();
	        } 
	        catch (Exception e) 
	        {
	            Log.d(LOG_NAME, "HTTP Post Failed");
	            e.printStackTrace();
	        }
			
			try
			{
				if (inputStream != null)
				{
					inputStream.close();
				}
			}
			catch (Exception ex)
			{
				Log.d(LOG_NAME, ex.getLocalizedMessage());
			}
			
			try
			{
				
				if (httpclient != null)
				{
					httpclient.getConnectionManager().shutdown();
					((AndroidHttpClient)httpclient).close();
				}
			}
			catch (Exception ex)
			{
				Log.d(LOG_NAME, ex.getLocalizedMessage());
			}
			
			return response;
		}
	
		private String httpPut()
		{
			InputStream inputStream  = null;
			HttpClient  httpclient   = null;
			String		response     = null;
			
			try 
	        {
	            // create HttpClient and Set Parameters
	        	HttpParams params = new BasicHttpParams();
	        	params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
	        	HttpConnectionParams.setConnectionTimeout(params, 5000);
	        	HttpConnectionParams.setSoTimeout(params, 5000);

				httpclient = newInstance("Android");
	            
	            // create Post
				HttpPut httpput = new HttpPut(job.getURL());
				httpput.setParams(params);
				httpput.addHeader("content-type", "application/x-www-form-urlencoded");
	            httpput.setEntity(new StringEntity(job.getArgument()));
	 
	            // make POST request to the given URL
	            HttpResponse httpResponse = httpclient.execute(httpput);
	            
	            // receive response as inputStream
	            if (httpResponse != null)
	            {
	            	inputStream = httpResponse.getEntity().getContent();
	            }
	            	            
	            // convert inputstream to string
	            if(inputStream != null)
	            {
	            	response = convertInputStreamToString(inputStream);
	            }
	            else
	            {
	            	Log.d(LOG_NAME, "PUT Did not work!");
	            }
	            
	        	httpResponse.getEntity().consumeContent();
	        } 
	        catch (Exception e) 
	        {
	            Log.d(LOG_NAME, "HTTP Put Failed");
	            e.printStackTrace();
	        }
			
			try
			{
				if (inputStream != null)
				{
					inputStream.close();
				}
			}
			catch (Exception ex)
			{
				Log.d(LOG_NAME, ex.getLocalizedMessage());
			}
			
			try
			{
				
				if (httpclient != null)
				{
					httpclient.getConnectionManager().shutdown();
					((AndroidHttpClient)httpclient).close();
				}
			}
			catch (Exception ex)
			{
				Log.d(LOG_NAME, ex.getLocalizedMessage());
			}
			
			return response;
		}
		
		private String httpDownload()
		{				    
			try
		    {
				BufferedInputStream in   	 = null;
			    FileOutputStream    fout 	 = null;
			    File				dir  	 = new File(job.getArgument().substring(0, job.getArgument().lastIndexOf("/") + 1));
			    String 				filename = (job.getURL().substring(job.getURL().lastIndexOf("/")+1));
			    File 				f    	 = new File(dir + "/" + filename);
		
			    log(LOG_NAME, "Downloading . . .\nURL: " + job.getURL() + "\nDestination: " + f.getAbsolutePath());
			    
		    	try 
			    {	
		    		if (!dir.exists())
		    		{
		    			dir.mkdirs();
		    		}
		    		
		    		if (!f.exists())
		    		{
		    			f.createNewFile();
		    		}
		    		
			        in   = new BufferedInputStream(new URL(job.getURL()).openStream());
			        fout = new FileOutputStream(f);

			        final byte data[] = new byte[1024];
			        int count;
			        while ((count = in.read(data, 0, 1024)) != -1) 
			        {
			            fout.write(data, 0, count);
			        }
			    } 
			    finally 
			    {
			        if (in != null)
			        {
			            in.close();
			        }
			        if (fout != null) 
			        {
			            fout.close();
			        }
			    }
		    	
		    	return f.getAbsolutePath();
		    }
		    catch (Exception ex)
		    {
		    	ex.printStackTrace();
		    }
			
			return "FAILURE";
		}
	}
}
