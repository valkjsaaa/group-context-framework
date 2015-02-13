package com.adefreitas.desktopframework;

import com.adefreitas.messages.ContextRequest;

public interface RequestProcessor 
{

	public void onSendingRequest(ContextRequest request);

}
