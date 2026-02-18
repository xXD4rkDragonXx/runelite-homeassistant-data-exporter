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

	@ConfigItem(
			keyName = "homeassistant_url",
			name = "Homeassistant Base URL",
			description = "example: http://homeassistant.local:8123",
			section = homeassistantSection,
			position = 101
	)
	default String homeassistantUrl()
	{ return ""; }

	@ConfigItem(
			keyName = "homeassistant_token",
			name = "Homeassistant Access token",
			description = "Your home assistant access token",
			section = homeassistantSection,
			secret = true,
			position = 102
	)
	default String homeassistantToken()
	{ return ""; }

	@ConfigItem(
			keyName = "validate_token",
			name = "Validate Home Assistant Token",
			description = "Turn on to validate your homeassistant setup, will provide details in game messages. ",
			section = homeassistantSection,
			position = 103
	)
	default boolean validateToken()
	{ return false; }

	@ConfigItem(
		keyName = "webhook",
		name = "webhook URL",
		description = "webhook to send the data",
		position = 1
	)
	default String webhook() {return null;}

}
