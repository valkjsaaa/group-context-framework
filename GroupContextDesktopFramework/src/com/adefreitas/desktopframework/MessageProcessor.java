package com.adefreitas.desktopframework;
import com.adefreitas.messages.CommMessage;

public interface MessageProcessor
{

	public void onMessage(CommMessage message);
	
}
