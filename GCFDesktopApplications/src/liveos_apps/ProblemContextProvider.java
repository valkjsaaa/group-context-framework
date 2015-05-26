package liveos_apps;

import com.adefreitas.desktopframework.toolkit.HttpToolkit;
import com.adefreitas.groupcontextframework.ContextProvider;
import com.adefreitas.groupcontextframework.GroupContextManager;

/**
 * This Context Provider Provides 
 * @author adefreit
 */
public class ProblemContextProvider extends ContextProvider
{
	// Context Configuration
	private static final String CONTEXT_TYPE  = "PROB";
	private static final String LOG_NAME      = "GCF-ContextProvider [" + CONTEXT_TYPE + "]";
	private static final String URL			  = "http://gcf.cmu-tbank.com/apps/creationfest/items.xml";
	
	
	public ProblemContextProvider(GroupContextManager groupContextManager) 
	{
		super(CONTEXT_TYPE, groupContextManager);
	}

	@Override
	public void start() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Started");
	}

	@Override
	public void stop() 
	{
		this.getGroupContextManager().log(LOG_NAME, this.getContextType() + " Provider Stopped");
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 1.0;
	}

	@Override
	public void sendContext() 
	{
		System.out.print("Downloading " + URL + " . . . ");
		String contents = HttpToolkit.get(URL);
		System.out.println("DONE!");
		
		this.getGroupContextManager().sendContext(this.getContextType(), new String[0], new String[] { "XML=" + contents });
	}
}
