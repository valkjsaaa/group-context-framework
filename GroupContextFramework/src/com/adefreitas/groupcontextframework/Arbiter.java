package com.adefreitas.groupcontextframework;

import java.util.ArrayList;

import com.adefreitas.messages.ContextCapability;
import com.adefreitas.messages.ContextRequest;

public abstract class Arbiter 
{
	private int requestType;
	
	public Arbiter(int requestType)
	{
		this.requestType = requestType;
	}
	
	public int getRequestType()
	{
		return requestType;
	}
	
	// ABSTRACT METHOD ------------------------------------------------------------------
	public abstract ArrayList<ContextCapability> selectCapability(
			ContextRequest request, 
			GroupContextManager gcm, 
			ArrayList<ContextCapability> subscribedCapabilities, 
			ArrayList<ContextCapability> receivedCapabilities);
}
