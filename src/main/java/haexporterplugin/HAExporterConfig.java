package haexporterplugin;

import haexporterplugin.enums.CombatTaskTier;
import haexporterplugin.enums.DiaryTier;
import haexporterplugin.enums.PlayerLookupService;
import net.runelite.client.config.*;

@ConfigGroup("HAExporter")
public interface HAExporterConfig extends Config
{
	// Note: keyNames are persisted settings. Change names/descriptions freely, but never rename a keyName.

	String MASTER_SWITCH_NOTE = "<br/>Applies to <i>all</i> connections: when off, this is disabled in the side panel too.";

	/* ============================
       Config Section Setup
       ============================ */

	@ConfigSection(
			name = "General",
			description = "How often and how quickly data is sent to Home Assistant",
			position = 100
	)
	String generalSection = "General";

	@ConfigSection(
			name = "Data Sharing",
			description = "Which parts of your player state are sent with every update",
			position = 200,
			closedByDefault = true
	)
	String dataSection = "Data Sharing";

	@ConfigSection(
			name = "Loot",
			description = "Which drops are sent as loot events",
			position = 300
	)
	String lootSection = "Loot";

	@ConfigSection(
			name = "Level Up",
			description = "Which level-ups are sent as events",
			position = 400,
			closedByDefault = true
	)
	String levelSection = "Level Up";

	@ConfigSection(
			name = "Collection Log",
			description = "Which new collection log slots are sent as events",
			position = 500,
			closedByDefault = true
	)
	String clogSection = "Collection Log";

	@ConfigSection(
			name = "Achievement Diary",
			description = "Which achievement diary tasks are sent as events",
			position = 600,
			closedByDefault = true
	)
	String diarySection = "Achievement Diary";

	@ConfigSection(
			name = "Combat Tasks",
			description = "Which combat achievement tasks are sent as events",
			position = 700,
			closedByDefault = true
	)
	String combatTaskSection = "Combat Tasks";

	@ConfigSection(
			name = "Deaths",
			description = "Death events",
			position = 800,
			closedByDefault = true
	)
	String deathSection = "Deaths";

	@ConfigSection(
			name = "Superior Spawns",
			description = "Superior slayer monster spawn events",
			position = 850,
			closedByDefault = true
	)
	String superiorSection = "Superior Spawns";

	@ConfigSection(
			name = "Just for Fun",
			description = "Silly extras that have nothing to do with Home Assistant",
			position = 950,
			closedByDefault = true
	)
	String funSection = "Just for Fun";

	/* ============================
       General
       ============================ */

	@Range(
			min = 1
	)
	@ConfigItem(
			keyName = "sendRate",
			name = "Update interval (ticks)",
			description = "Game ticks between regular state updates (1 tick = 0.6s, so 100 is about a minute).<br/>" +
					"Events such as drops and deaths are always sent right away",
			position = 101,
			section = generalSection
	)
	default int sendRate() {return 100;}

	@ConfigItem(
			keyName = "sendHealthInstantly",
			name = "Send health changes instantly",
			description = "When on, every health change is sent right away.<br/>" +
					"When off, health is only sent with the regular update",
			position = 102,
			section = generalSection
	)
	default boolean sendHealthInstantly() {
		return true;
	}

	@ConfigItem(
			keyName = "sendPrayerInstantly",
			name = "Send prayer changes instantly",
			description = "When on, every prayer point change is sent right away.<br/>" +
					"When off, prayer is only sent with the regular update",
			position = 103,
			section = generalSection
	)
	default boolean sendPrayerInstantly() {
		return true;
	}

	/* ============================
       Data Sharing
       ============================ */

	@ConfigItem(
			keyName = "includeInventory",
			name = "Share inventory",
			description = "Send the contents of your inventory." + MASTER_SWITCH_NOTE,
			position = 201,
			section = dataSection
	)
	default boolean includeInventory() {
		return true;
	}

	@ConfigItem(
			keyName = "includeEquipment",
			name = "Share equipment",
			description = "Send your worn equipment." + MASTER_SWITCH_NOTE,
			position = 202,
			section = dataSection
	)
	default boolean includeEquipment() {
		return true;
	}

	@ConfigItem(
			keyName = "includeLocation",
			name = "Share location",
			description = "Send your in-game coordinates." + MASTER_SWITCH_NOTE,
			position = 203,
			section = dataSection
	)
	default boolean includeLocation() {
		return true;
	}

	/* ============================
       Loot
       ============================ */

	@ConfigItem(
			keyName = "includeLootEvents",
			name = "Send loot events",
			description = "Send an event when you receive a notable drop." + MASTER_SWITCH_NOTE,
			position = 301,
			section = lootSection
	)
	default boolean includeLootEvents() {
		return true;
	}

	@ConfigItem(
			keyName = "minLootValue",
			name = "Minimum item value (gp)",
			description = "Only send a drop when an item stack is worth at least this much",
			position = 302,
			section = lootSection
	)
	default int minLootValue() {
		return 25000;
	}

	@ConfigItem(
			keyName = "lootRarityThreshold",
			name = "Also send if rarer than 1 in X",
			description = "Also send drops this rare, even when they are below the minimum value.<br/>" +
					"For example, 100 sends anything with a 1% drop rate or rarer.<br/>" +
					"Set to 0 to turn off. Only works for NPC drops and pickpocketing",
			position = 303,
			section = lootSection
	)
	default int lootRarityThreshold() {
		return 0;
	}

	@ConfigItem(
			keyName = "lootRarityValueIntersection",
			name = "Require value AND rarity",
			description = "When on, a drop must pass <i>both</i> the minimum value and the rarity threshold.<br/>" +
					"Drops without known rarity only need to pass the minimum value",
			position = 304,
			section = lootSection
	)
	default boolean lootRarityValueIntersection() {
		return false;
	}

	@ConfigItem(
			keyName = "lootItemAllowlist",
			name = "Always send these items",
			description = "These items are always sent, even when below the value or rarity threshold.<br/>" +
					"One item name per line, not case-sensitive. Use * as a wildcard (e.g. <i>*pet*</i>)",
			position = 305,
			section = lootSection
	)
	default String lootItemAllowlist() {
		return "";
	}

	@ConfigItem(
			keyName = "lootItemDenylist",
			name = "Never send these items",
			description = "These items are never sent, whatever their value or rarity.<br/>" +
					"Wins over 'Always send these items'.<br/>" +
					"One item name per line, not case-sensitive. Use * as a wildcard",
			position = 306,
			section = lootSection
	)
	default String lootItemDenylist() {
		return "";
	}

	@ConfigItem(
			keyName = "lootSourceDenylist",
			name = "Never send loot from these sources",
			description = "Drops from these NPCs or activities are never sent.<br/>" +
					"One name per line, not case-sensitive.<br/>" +
					"Does <i>not</i> apply to player names for PK loot",
			position = 307,
			section = lootSection
	)
	default String lootSourceDenylist() {
		return "Einar\n";
	}

	@ConfigItem(
			keyName = "lootIncludePlayer",
			name = "Send PK loot",
			description = "Send loot you get from killing other players",
			position = 308,
			section = lootSection
	)
	default boolean lootIncludePlayer() {
		return true;
	}

	@ConfigItem(
			keyName = "lootIncludePkChest",
			name = "PK loot chest: use total value",
			description = "For PK loot chests, send when the <i>combined</i> value passes the minimum,<br/>" +
					"even if no single item does",
			position = 309,
			section = lootSection
	)
	default boolean lootIncludePkChest() {
		return true;
	}

	@ConfigItem(
			keyName = "lootIncludePickpocket",
			name = "Send pickpocket loot",
			description = "Send loot you get from pickpocketing",
			position = 310,
			section = lootSection
	)
	default boolean lootIncludePickpocket() {
		return true;
	}

	@ConfigItem(
			keyName = "playerLookupService",
			name = "Player lookup link",
			description = "Which website player names in PK loot events link to",
			position = 311,
			section = lootSection
	)
	default PlayerLookupService playerLookupService() {
		return PlayerLookupService.OSRS_HISCORE;
	}

	/* ============================
       Level Up
       ============================ */

	@ConfigItem(
			keyName = "includeLevelUpEvents",
			name = "Send level-up events",
			description = "Send an event when you gain a level." + MASTER_SWITCH_NOTE + "<br/>" +
					"Your skill levels are still shared with every update",
			position = 401,
			section = levelSection
	)
	default boolean includeLevelUpEvents() {
		return true;
	}

	@Range(
			min = 1,
			max = 127
	)
	@ConfigItem(
			keyName = "levelMinValue",
			name = "Minimum level",
			description = "Only send level-ups to this level or higher",
			position = 402,
			section = levelSection
	)
	default int levelMinValue() {
		return 1;
	}

	@Range(
			min = 1,
			max = 99
	)
	@ConfigItem(
			keyName = "levelInterval",
			name = "Send every Nth level",
			description = "Only send levels that are a multiple of this number (e.g. 5 sends 5, 10, 15...).<br/>" +
					"Level 99 is always sent",
			position = 403,
			section = levelSection
	)
	default int levelInterval() {
		return 1;
	}

	@ConfigItem(
			keyName = "levelIncludeVirtual",
			name = "Send virtual levels",
			description = "Send levels above 99 (virtual levels, up to 126, and 200M xp as 127)",
			position = 404,
			section = levelSection
	)
	default boolean levelIncludeVirtual() {
		return true;
	}

	@ConfigItem(
			keyName = "levelIncludeCombat",
			name = "Send combat level",
			description = "Send an event when your combat level goes up",
			position = 405,
			section = levelSection
	)
	default boolean levelIncludeCombat() {
		return true;
	}

	/* ============================
       Collection Log
       ============================ */

	@ConfigItem(
			keyName = "includeCollectionLogEvents",
			name = "Send collection log events",
			description = "Send an event when you fill a new collection log slot." + MASTER_SWITCH_NOTE + "<br/>" +
					"Needs the in-game 'Collection log - New addition notification' setting to be on",
			position = 501,
			section = clogSection
	)
	default boolean includeCollectionLogEvents() {
		return true;
	}

	@ConfigItem(
			keyName = "clogMinValue",
			name = "Minimum item value (gp)",
			description = "Only send new collection log items worth at least this much.<br/>" +
					"Items without a GE price (such as pets) are always sent",
			position = 502,
			section = clogSection
	)
	default int clogMinValue() {
		return 0;
	}

	/* ============================
       Achievement Diary
       ============================ */

	@ConfigItem(
			keyName = "includeAchievementDiaryEvents",
			name = "Send diary events",
			description = "Send an event when you complete an achievement diary task." + MASTER_SWITCH_NOTE,
			position = 601,
			section = diarySection
	)
	default boolean includeAchievementDiaryEvents() {
		return true;
	}

	@ConfigItem(
			keyName = "diaryMinTier",
			name = "Minimum tier",
			description = "Only send diary tasks of this tier or harder",
			position = 602,
			section = diarySection
	)
	default DiaryTier diaryMinTier() {
		return DiaryTier.EASY;
	}

	/* ============================
       Combat Tasks
       ============================ */

	@ConfigItem(
			keyName = "includeCombatTaskEvents",
			name = "Send combat task events",
			description = "Send an event when you complete a combat achievement task." + MASTER_SWITCH_NOTE,
			position = 701,
			section = combatTaskSection
	)
	default boolean includeCombatTaskEvents() {
		return true;
	}

	@ConfigItem(
			keyName = "combatTaskMinTier",
			name = "Minimum tier",
			description = "Only send combat tasks of this tier or harder",
			position = 702,
			section = combatTaskSection
	)
	default CombatTaskTier combatTaskMinTier() {
		return CombatTaskTier.EASY;
	}

	/* ============================
       Deaths
       ============================ */

	@ConfigItem(
			keyName = "includeDeathEvents",
			name = "Send death events",
			description = "Send an event when you die, with the items kept and lost and who killed you." + MASTER_SWITCH_NOTE,
			position = 801,
			section = deathSection
	)
	default boolean includeDeathEvents() {
		return true;
	}

	/* ============================
       Superior Spawns
       ============================ */

	@ConfigItem(
			keyName = "includeSuperiorEvents",
			name = "Send superior spawn events",
			description = "Send an event when a superior slayer monster appears." + MASTER_SWITCH_NOTE,
			position = 851,
			section = superiorSection
	)
	default boolean includeSuperiorEvents() {
		return true;
	}

	/* ============================
       Just for Fun
       ============================ */

	@ConfigItem(
			keyName = "kebab",
			name = "Kebab?",
			description = "Do you like kebab?",
			position = 951,
			section = funSection
	)
	default boolean kebab() {
		return false;
	}

	@ConfigItem(
			keyName = "garbage",
			name = "Garbage?",
			description = "Do you feel like garbage?",
			position = 952,
			section = funSection
	)
	default boolean garbage() {
		return false;
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
