package com.adefreitas.desktopframework;

import com.adefreitas.messages.ContextData;

/**
 * Interface so that Applications Can Receive Context
 * @author adefreit
 */
public interface EventReceiver
{
	public void onContextData(ContextData data);
}
