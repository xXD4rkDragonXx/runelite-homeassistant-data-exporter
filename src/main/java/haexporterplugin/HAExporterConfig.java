package haexporterplugin;

import net.runelite.client.config.*;

@ConfigGroup("HAExporter")
public interface HAExporterConfig extends Config
{
	@ConfigSection(
		name = "Home Assistant",
		description = "Settings for connecting to Home Assistant",
		position = 100
	)
	String homeassistantSection = "Home Assistant Settings";

	@ConfigSection(
		name = "Advanced",
		description = "Settings for notifying about Advanced features",
		position = 900,
		closedByDefault = true
	)
	String advancedSection = "Advanced Settings";

	@Range(
			min = 1
	)
	@ConfigItem(
		keyName = "sendRate",
		name = "Send rate",
		description = "How many game ticks between base messages (Does not impact other triggers eg. drops and deaths)",
		position = 901,
		section = advancedSection
	)
	default int sendRate() {return 100;}

	/* ============================
       Hidden Config Items
       ============================ */
	@ConfigItem(
			keyName = "homeassistantConnections",
			name = "Home Assistant Connections",
			description = "Stores all configured connections",
			hidden = true
	)
	default String homeassistantConnections()
	{
		return "[]"; // start empty as JSON array
	}

	@ConfigItem(
			keyName = "homeassistantConnections",
			name = "Home Assistant Connections",
			description = "Stores all configured connections",
			hidden = true
	)
	void setHomeassistantConnections(String value);
}
