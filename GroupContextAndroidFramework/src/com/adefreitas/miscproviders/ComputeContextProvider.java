package com.adefreitas.miscproviders;

import java.util.ArrayList;
import java.util.Date;

import com.adefreitas.groupcontextframework.*;

import android.os.AsyncTask;
import android.util.Log;

public class ComputeContextProvider extends ContextProvider
{
	public ComputeContextProvider(GroupContextManager groupContextManager) 
	{
		super("COM", groupContextManager);
	}

	@Override
	public void start() 
	{
		
	}

	@Override
	public void stop() 
	{
		
	}

	@Override
	public double getFitness(String[] parameters) 
	{
		return 0.0;
	}

	class SummationComputeTask extends AsyncTask<Integer, Integer, Integer>
	{
		private GroupContextManager groupContextManager;
		private Date 				start;
		
		public SummationComputeTask(GroupContextManager gcm)
		{
			this.groupContextManager = gcm;
		}
		
		@Override
		protected void onPreExecute()
		{
			Log.i("GCM-ContextProvider", "Preparing Summation Compute Task");
			start = new Date();
		}
		
		@Override
		protected Integer doInBackground(Integer... params) 
		{
			Log.i("GCM-ContextProvider", "Performing Compute Task");
			int sum = 0;

			if (params.length == 1)
			{
				for (int i=0; i<params[0]; i++)
				{
					sum += i;
				}	
			}
			else if (params.length == 2)
			{
				for (int i=params[0]; i<params[1]; i++)
				{
					sum++;
				}
			}
						
			return sum;
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			Log.i("GCM-ContextProvider", "Compute Task Completed (Result = " + result + ")");
			Double duration = (new Date().getTime() - start.getTime())/1000.0;
			groupContextManager.sendContext(getContextType(), "", new String[0], new String[] { result.toString(), duration.toString() });
		}

	}
	
	class PrimeNumberComputeTask extends AsyncTask<Integer, Integer, Integer[]>
	{
		private GroupContextManager groupContextManager;
		private ArrayList<Integer>  primes;
		private int 				lastLeftOff = 0;
		
		public PrimeNumberComputeTask(GroupContextManager groupContextManager)
		{
			this.groupContextManager = groupContextManager;
		}

		@Override
		protected void onPreExecute()
		{
			Log.i("GCM-ContextProvider", "Preparing Prime Number Compute Task");
			primes = new ArrayList<Integer>();
		}
		
		@Override
		protected Integer[] doInBackground(Integer... params) {
						
			for (int i=2; i<500; i++)
			{
				if (isPrime(i))
				{					
					//groupContextManager.sendContext("COM", groupContextManager.getDeviceID(), new String[] { Integer.toString(i) });
					primes.add(i);
				}
			}

			return primes.toArray(new Integer[0]);
		}
		
		protected void onProgressUpdate(Integer... values) {
			for (int i=lastLeftOff; i<primes.size(); i++)
			{
				groupContextManager.sendContext("COM", "", new String[0], new String[] { Integer.toString(primes.get(i)) });
				lastLeftOff++;
			}
		}
		
		@Override
		protected void onPostExecute(Integer[] result)
		{
			Log.i("GCM-ContextProvider", "Compute Task Completed");
			
			ArrayList<String> sResults = new ArrayList<String>();
			
			for (Integer i : result)
			{
				sResults.add(Integer.toString(i));	
			}			
			
			groupContextManager.sendContext("COM", "", new String[0], sResults.toArray(new String[0]));
		}
		
		private boolean isPrime(int value)
		{
			for (int i=2; i<value/2; i++)
			{
				if (value % i == 0)
				{
					return false;
				}
			}
			
			return true;
		}
	}

	@Override
	public void sendMostRecentReading() 
	{
		System.out.println("Starting Compute");
//		
//		if (parameters[0].equals("SUM"))
//		{
//			System.out.println("Summation Task Detected");
//			SummationComputeTask computeTask = new SummationComputeTask(getGroupContextManager());
//			Integer[] 			 arguments;
//			
//			if (parameters.length == 2)
//			{
//				arguments = new Integer[] { Integer.valueOf(parameters[1]) };
//				computeTask.execute(arguments);
//			}
//			else if (parameters.length == 3)
//			{
//				arguments = new Integer[] { Integer.valueOf(parameters[1]), Integer.valueOf(parameters[2]) };
//				computeTask.execute(arguments);
//			}
//		}
//		else if (parameters[0].equals("PRIME"))
//		{
//			System.out.println("Prime Task Detected");
//			PrimeNumberComputeTask computeTask = new PrimeNumberComputeTask(getGroupContextManager());
//			Integer[] 			   arguments;
//			
//			if (parameters.length == 2)
//			{
//				arguments = new Integer[] { Integer.valueOf(parameters[1]) };
//				computeTask.execute(arguments);
//			}
//			else if (parameters.length == 3)
//			{
//				arguments = new Integer[] { Integer.valueOf(parameters[1]), Integer.valueOf(parameters[2]) };
//				computeTask.execute(arguments);
//			}
//			else
//			{
//				System.out.println("NOTHING HAPPENS");
//			}
//		}
	}
}
