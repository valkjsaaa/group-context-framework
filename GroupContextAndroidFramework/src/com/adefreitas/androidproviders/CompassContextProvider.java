package com.adefreitas.androidproviders;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.ContextReportingThread;
import com.adefreitas.groupcontextframework.GroupContextManager;

/**
 * Compass Context Provider [COMPASS]
 * Delivers Compass Heading using the Magnetometer and Accelerometer
 * 
 * Parameters
 *      NONE 
 * 
 * Author: Adrian de Freitas
 */
public class CompassContextProvider extends ContextProvider implements SensorEventListener
{
	// GCF Context Configuration
	private static final String CONTEXT_TYPE = "COMPASS";
	private static final String LOG_NAME     = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	private static final String DESCRIPTION  = "Shares compass data using the phone's magnetometer and accelerometer.";
	
    // Device sensor manager
    private SensorManager sensorManager;
    private Sensor        accelerometer;
    private Sensor        magnetometer;
    
    // Values
    double  accuracy;
    double  azimuth;
    double  pitch;
    double  roll;
    float[] mGravity;
    float[] mGeomagnetic;
	
	public CompassContextProvider(Context context, GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, DESCRIPTION, groupContextManager);
		
		// initialize your android device sensor capabilities
        sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer  = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
      
        azimuth  = 0.0;
        pitch    = 0.0;
        roll     = 0.0;
        accuracy = 0.0;
	}

	@Override
	public void start() 
	{
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
	    sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
	  
		Log.d(LOG_NAME, "Compass Sensor Started");
	}

	@Override
	public void stop() 
	{
		sensorManager.unregisterListener(this);
		Log.d(LOG_NAME, "GPS Sensor Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendContext() 
	{
		this.getGroupContextManager().sendContext(this.getContextType(), 
				this.getSubscriptionDeviceIDs(), 
				new String[] { "AZIMUTH=" + azimuth, "PITCH=" + pitch, "ROLL=" + roll});
	}

	@Override
	public void onSensorChanged(SensorEvent event) 
	{
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			mGravity = event.values;
		}
		   
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
		{
			mGeomagnetic = event.values;
		}
		     
		if (mGravity != null && mGeomagnetic != null) 
		{
			float R[] = new float[9];
		    float I[] = new float[9];
		    
		    boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
		    
		    if (success) 
		    {
		    	float orientation[] = new float[3];
		        SensorManager.getOrientation(R, orientation);
		        azimuth = orientation[0];
		        pitch   = orientation[1];
		        roll    = orientation[2];
		    }
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) 
	{
		this.accuracy = accuracy;
	}
}
