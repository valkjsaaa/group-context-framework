package com.adefreitas.desktopproviders;

import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.ContextReportingThread;
import com.adefreitas.groupcontextframework.GroupContextManager;

public class FixedLocationContextProvider extends ContextProvider
{
	private double lat;
	private double lon;
	
	private ContextReportingThread t;
	
	public FixedLocationContextProvider(GroupContextManager groupContextManager, double lat, double lon) {
		super("LOC", groupContextManager);
		this.lat = lat;
		this.lon = lon;
	}

	@Override
	public void start() 
	{	
		t = new ContextReportingThread(this);
		t.start();
		
		this.getGroupContextManager().log("GCM-ContextProvider", "Fixed Location Sensor Started");
	}

	@Override
	public void stop() {
		// Halts the Reporting Thread
		if (t != null)
		{
			t.halt();
			t = null;	
		}
	}
	
	@Override
	public double getFitness(String[] parameters) {
		return 1.0;
	}

	@Override
	public void sendContext() 
	{
		getGroupContextManager().sendContext(this.getContextType(), new String[0], new String[] { Double.toString(lat), Double.toString(lon), "250.0" });
	}
}
