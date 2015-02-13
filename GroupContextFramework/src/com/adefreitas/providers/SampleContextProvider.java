package com.adefreitas.providers;

import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.GroupContextManager;

/**
 * This is a template for a context provider.
 * COPY AND PASTE; NEVER USE
 * @author adefreit
 */
public class SampleContextProvider extends ContextProvider
{
	// Context Configuration
	//private static final String FRIENDLY_NAME = "SAMPLE";	
	private static final String CONTEXT_TYPE  = "CONTEXT_TYPE";
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	
	public SampleContextProvider(GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
	}

	@Override
	public void start() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started");
	}

	@Override
	public void stop() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendMostRecentReading() 
	{
		this.getGroupContextManager().sendContext(this.getContextType(), "SAMPLE DESCRIPTION", new String[0], new String[] { "SAMPLE DATA" });
	}
}
