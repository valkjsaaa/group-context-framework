package com.adefreitas.androidframework;

import com.adefreitas.androidbluewave.JSONContextParser;
import com.adefreitas.messages.ContextData;

/**
 * Interface so that Activities can Get Context Without Having to Listen to Intents (the Application does this for them) 
 * @author adefreit
 */
public interface ContextReceiver
{
	public void onContextData(ContextData data);
	
	public void onGCFOutput(String output);

	public void onBluewaveContext(JSONContextParser parser);
}
