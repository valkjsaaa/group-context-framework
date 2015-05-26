package com.adefreitas.desktopproviders;

import java.io.IOException;
import java.util.Vector;

import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.GroupContextManager;

/**
 * This is a provider that reports Bluetooth discovery
 * @author adefreit
 */
public class BluetoothContextProvider extends ContextProvider
{

	private Vector    		  devicesDiscovered     = new Vector();
	private Object    		  inquiryCompletedEvent = new Object();
	private DiscoveryListener listener 				= new DiscoveryListener() 
	{
		public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) 
		{
            System.out.println("Device " + btDevice.getBluetoothAddress() + " found");
            devicesDiscovered.addElement(btDevice);
            try 
            {
                System.out.println("     name " + btDevice.getFriendlyName(false));
            } 
            catch (IOException cantGetDeviceName) 
            {
            	// Do Nothing
            }
        }

        public void inquiryCompleted(int discType) 
        {
            System.out.println("Device Inquiry completed!");
            synchronized(inquiryCompletedEvent)
            {
                inquiryCompletedEvent.notifyAll();
            }
        }

        public void serviceSearchCompleted(int transID, int respCode) 
        {
        	// Do Nothing
        }

        public void servicesDiscovered(int transID, ServiceRecord[] servRecord) 
        {
        	// Do Nothing
        }
	};
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public BluetoothContextProvider(GroupContextManager groupContextManager) 
	{
		super("BLU", groupContextManager);
		
		try
		{
			synchronized(inquiryCompletedEvent) 
			{
	            boolean started = LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, listener);
	            
	            if (started) 
	            {
	                System.out.println("wait for device inquiry to complete...");
	                inquiryCompletedEvent.wait();
	                System.out.println(devicesDiscovered.size() +  " device(s) found");
	            }
	            else
	            {
	            	System.out.println("didn't even get started");
	            }
	        }
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	@Override
	public void start() 
	{
		this.getGroupContextManager().log("GCM-ContextProvider", this.getContextType() + " Provider Started");
	}

	@Override
	public void stop() 
	{		
		this.getGroupContextManager().log("GCM-ContextProvider", this.getContextType() + " Provider Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendContext() 
	{
		this.getGroupContextManager().sendContext(this.getContextType(), new String[0], new String[] { "SAMPLE DATA" });
	}
}
