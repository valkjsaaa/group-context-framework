package com.adefreitas.desktopframework;
import com.adefreitas.groupcontextframework.BatteryMonitor;

public class DesktopBatteryMonitor extends BatteryMonitor
{

	public DesktopBatteryMonitor()
	{
		super(5);
	}

	public double getBatteryPowerRemaining() {
		return 100.0;
	}
	
	public double getBatteryChange(long duration)
	{
		return 0.0;
	}
}
