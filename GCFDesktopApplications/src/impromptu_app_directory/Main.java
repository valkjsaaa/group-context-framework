package impromptu_app_directory;

import java.util.ArrayList;

public class Main 
{
	public static void main(String[] args) 
	{
		ArrayList<GCFController> controllers = new ArrayList<GCFController>();

		controllers.clear();
			
		try
		{
			// TRUE if Use Bluetooth for Data, FALSE Otherwise
			//controllers.add(new GCFController("DIAGNOSTICS", false));
			controllers.add(new GCFController());
		}
		catch (Exception ex)
		{
			System.out.println("ERROR: " + ex.getMessage());
			
			for (GCFController controller : controllers)
			{
				controller.gcm.disconnect();
			}
		}
	}
}
