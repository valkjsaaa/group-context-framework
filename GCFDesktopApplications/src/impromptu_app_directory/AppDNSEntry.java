package impromptu_app_directory;

import java.util.Date;

import com.adefreitas.gcf.messages.CommMessage;

public class AppDNSEntry 
{
	private String[] appInformation;
	private Date	 dateCreated;
	
	/**
	 * Constructor
	 * @param registrationInfo
	 */
	public AppDNSEntry(String[] registrationInfo)
	{
		this.appInformation = registrationInfo;
		this.dateCreated 	= new Date();
	}
	
	/**
	 * Returns the Context Type of this Application
	 * @return
	 */
	public String getContextType()
	{
		return CommMessage.getValue(appInformation, "APP_CONTEXT_TYPE");
	}
	
	/**
	 * Returns the Device ID that Sent this Entry
	 * @return
	 */
	public String getDeviceID()
	{
		return CommMessage.getValue(appInformation, "DEVICE_ID");
	}
	
	/**
	 * Returns the Application ID
	 * @return
	 */
	public String getAppID()
	{
		return CommMessage.getValue(appInformation, "APP_ID");
	}
	
	/**
	 * Returns the Application Information
	 * @return
	 */
	public String[] getAppInformation()
	{
		return appInformation;
	}

	/**
	 * Returns the age of this entry in milliseconds
	 * @return
	 */
	public long getAge()
	{
		return new Date().getTime() - dateCreated.getTime();
	}
}
