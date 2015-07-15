package com.adefreit.openimajtoolkit;

public class OpenImajTest 
{
	public static void main(String[] args)
	{
		System.out.println("OpenImajToolkit");
		
		OpenImajToolkit toolkit = new OpenImajToolkit();
		
		System.out.println("Comparison Results: " + toolkit.compareImages("printer1.jpeg", "printer2.jpeg"));
		
		System.out.println("DONE");
	}
}
