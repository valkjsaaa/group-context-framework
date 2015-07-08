package impromptu_apps.snaptoit;


import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;

import org.apache.commons.vfs2.provider.UriParser;

import com.adefreitas.desktopframework.toolkit.HttpToolkit;
import com.adefreitas.groupcontextframework.CommManager.CommMode;
import com.adefreitas.groupcontextframework.ContextSubscriptionInfo;
import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Sti_GenericDevice extends SnapToItApplicationProvider
{	
	String[] urls;
	
	public Sti_GenericDevice(GroupContextManager groupContextManager, String name, String[] urls, CommMode commMode, String ipAddress, int port)
	{
		super(groupContextManager, 
				"STI_GENERIC_" + name,
				name,
				"Generic Description",
				"Snap-To-It",
				new String[] { },  // Contexts
				new String[] { },  // Preferences
				"",				   // LOGO
				60,
				commMode,
				ipAddress,
				port);
		
		this.name = name;
		this.urls = urls;
		this.addPhotoFromWeb(urls);
	}

	@Override
	public String[] getInterface(ContextSubscriptionInfo subscription)
	{	
		return new String[] { "WEBSITE=http://www.google.com" };
	}

	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		super.onComputeInstruction(instruction);
	}

}
