package com.damoscreenshot;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DamoScreenshotPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DamoScreenshotPlugin.class);
		RuneLite.main(args);
	}
}
