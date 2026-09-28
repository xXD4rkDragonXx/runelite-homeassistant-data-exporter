package haexporterplugin;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class HAExporterPluginTest
{
	// loadBuiltin takes generic varargs (Class<? extends Plugin>...), which javac flags as unchecked
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(HAExporterPlugin.class);
		RuneLite.main(args);
	}
}