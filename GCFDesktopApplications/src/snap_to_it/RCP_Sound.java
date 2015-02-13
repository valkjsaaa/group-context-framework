package snap_to_it;

import com.adefreitas.groupcontextframework.GroupContextManager;
import com.adefreitas.messages.CommMessage;
import com.adefreitas.messages.ComputeInstruction;
import com.adefreitas.messages.ContextRequest;

public class RCP_Sound extends RemoteControlProvider
{
	// Provider Specific Variables Go Here
	private static final String UPLOAD_FOLDER = "/var/www/html/gcf/universalremote/Server/";	// The Folder Where this Program is Looking for Sound Files
	private final  String WEBSITE_URL   = "http://71.182.231.215/gcf/universalremote/Websites/sample.html";
	private boolean PLAYING_MUSIC = false;
	
	/**
	 * Constructor
	 * @param groupContextManager
	 */
	public RCP_Sound(GroupContextManager groupContextManager) 
	{
		super(groupContextManager);
		
		// Sends a Request for Light Data
		this.getGroupContextManager().sendRequest("MP3", ContextRequest.SINGLE_SOURCE, 10000, new String[0]);
		
		// TODO:  Loads the Pictures to Use
//		this.addPhoto(APP_DATA_FOLDER + "jambox1.jpg");
//		this.addPhoto(APP_DATA_FOLDER + "jambox2.jpg");
//		this.addPhoto(APP_DATA_FOLDER + "jambox3.jpg");
		this.addPhoto(APP_DATA_FOLDER + "speaker1.jpg");
	}
	
	public void sendMostRecentReading()
	{
		this.getGroupContextManager().sendContext(
				this.getContextType(), 
				"Radio", 
				new String[] { }, 
				//new String[] { "WEBSITE=" + WEBSITE_URL });
				new String[] { "UI=" + getUserInterface() });
	}
	
	@Override
	public void onComputeInstruction(ComputeInstruction instruction)
	{
		System.out.println("Received Instruction: " + instruction.toString());
		
		if (instruction.getCommand().equals("PLAY"))
		{
			String filePath = CommMessage.getValue(instruction.getParameters(), "uploadPath");
			System.out.println("I got notified of an upload at: " + filePath);
			
			PLAYING_MUSIC = true;
						
			this.getGroupContextManager().sendComputeInstruction("MP3", "PLAY", new String[] { "filePath=" + filePath });
		}
		else if (instruction.getCommand().equals("STOP"))
		{
			System.out.println("I got a request to stop playing.");
			
			PLAYING_MUSIC = false;
			
			this.getGroupContextManager().sendComputeInstruction("MP3", "STOP", new String[] { });
		}
		
		sendMostRecentReading();
	}

	private String getUserInterface()
	{
		String ui = "<html><title>Speaker Controls</title>";
		
		// Adds Specific Context Results
		if (!PLAYING_MUSIC)
		{
			//ui += "<div><input value=\"Play Sound\" type=\"button\" onclick=\"device.uploadFile('PLAY', '" + UPLOAD_FOLDER + "', ['.mp3']);\"/></div>";
			ui += "<h3>Ready for Music (MP3 Files Only)</h3><br/>";
			ui += "<input type=\"image\" src=\"http://71.182.231.215/gcf/universalremote/Websites/play.png\" name=\"play\" width=\"70\" height=\"70\" onclick=\"device.uploadFile('PLAY', '" + UPLOAD_FOLDER + "', ['.mp3']);\"/>";
		}
		else
		{
			ui += "<div>Currently Playing a Song</div><br />";
			ui += "<input type=\"image\" src=\"http://71.182.231.215/gcf/universalremote/Websites/stop.png\" name=\"play\" width=\"70\" height=\"70\" onclick=\"device.sendComputeInstruction('STOP', []);\"/>";
			//ui += "<div><input value=\"Stop Playing\" type=\"button\" onclick=\"device.sendComputeInstruction('STOP', []);\"/></div>";
		}
				
		ui += "</html>";
		
		return ui;
	}
}
