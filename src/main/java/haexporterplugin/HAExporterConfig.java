package haexporterplugin;

import haexporterplugin.enums.PlayerLookupService;
import net.runelite.client.config.*;

@ConfigGroup("HAExporter")
public interface HAExporterConfig extends Config
{

	/* ============================
       Config Section Setup
       ============================ */

	@ConfigSection(
			name = "Loot",
			description = "Settings for notifying when loot is dropped",
			position = 300,
			closedByDefault = false
	)
	String lootSection = "Loot";

	@ConfigSection(
		name = "Advanced",
		description = "Settings for notifying about Advanced features",
		position = 900,
		closedByDefault = true
	)
	String advancedSection = "Advanced Settings";

	/* ============================
       Loot Config Items
       ============================ */

	@ConfigItem(
			keyName = "minLootValue",
			name = "Min Loot value",
			description = "The minimum value of an item for a notification to be sent.<br/>" +
					"For PK chests, the <i>total</i> value of the items is compared with this threshold",
			position = 303,
			section = lootSection
	)
	default int minLootValue() {
		return 0;
	}

	@ConfigItem(
			keyName = "lootItemAllowlist",
			name = "Item Allowlist",
			description = "Always fire notifications for these items, despite value settings.<br/>" +
					"Place one item name per line (case-insensitive; asterisks are wildcards)",
			position = 307,
			section = lootSection
	)
	default String lootItemAllowlist() {
		return "";
	}

	@ConfigItem(
			keyName = "lootItemDenylist",
			name = "Item Denylist",
			description = "Never fire notifications for these items, despite value or rarity settings.<br/>" +
					"Place one item name per line (case-insensitive; asterisks are wildcards)",
			position = 307,
			section = lootSection
	)
	default String lootItemDenylist() {
		return "";
	}

	@ConfigItem(
			keyName = "lootSourceDenylist",
			name = "Source Denylist",
			description = "Never fire notifications for these loot sources, despite value or rarity settings.<br/>" +
					"Place one NPC/source name per line (case-insensitive).<br/>" +
					"Does <i>not</i> apply to player names for PK loot",
			position = 307,
			section = lootSection
	)
	default String lootSourceDenylist() {
		return "Einar\n";
	}

	@ConfigItem(
			keyName = "lootRarityThreshold",
			name = "Rarity Override (1 in X)",
			description = "Fires notifications for sufficiently rare drops, despite the 'Min Loot value' threshold.<br/>" +
					"Corresponds to a 1 in X chance. For example, 100 notifies for items with 1% drop rate or rarer.<br/>" +
					"Has no effect when set to zero.<br/>" +
					"Currently only applies to NPC drops",
			position = 308,
			section = lootSection
	)
	default int lootRarityThreshold() {
		return 0;
	}

	@ConfigItem(
			keyName = "lootRarityValueIntersection",
			name = "Require both Rarity and Value",
			description = "Whether items must exceed <i>both</i> the Min Value AND Rarity thresholds to be notified.<br/>" +
					"Does not apply to drops where Dink lacks rarity data.<br/>" +
					"Currently only impacts NPC drops",
			position = 309,
			section = lootSection
	)
	default boolean lootRarityValueIntersection() {
		return false;
	}
	/* ============================
       Advanced Config Items
       ============================ */

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
			keyName = "playerLookupService",
			name = "Player Lookup Service",
			description = "The service used to lookup a players account",
			position = 902,
			section = advancedSection
	)
	default PlayerLookupService playerLookupService() {
		return PlayerLookupService.OSRS_HISCORE;
	}


	// Whether to play sound on eating kebab
	@ConfigItem(
			keyName = "kebab",
			name = "Kebab?",
			description = "Do you like kebab?",
			position = 902,
			section = advancedSection
	)
	default boolean kebab() {
		return true;
	}

	// Whether to play sound on eating kebab
	@ConfigItem(
			keyName = "garbage",
			name = "Garbage?",
			description = "Do you feel like garbage?",
			position = 902,
			section = advancedSection
	)
	default boolean garbage() {
		return true;
	}

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
