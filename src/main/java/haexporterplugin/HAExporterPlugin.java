package haexporterplugin;

import com.google.inject.Provides;
import javax.inject.Inject;

import haexporterplugin.data.HealthData;
import haexporterplugin.data.PrayerData;
import haexporterplugin.data.SpellbookData;
import haexporterplugin.notifiers.DeathNotifier;
import haexporterplugin.notifiers.ItemNotifier;
import haexporterplugin.notifiers.LevelNotifier;
import haexporterplugin.notifiers.LocationNotifier;
import haexporterplugin.utils.MessageBuilder;
import haexporterplugin.utils.TickUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "HA Exporter",
	description = "Export Game data to your HomeAssistant"
)
public class HAExporterPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private HAExporterConfig config;

	@Inject
	private MessageBuilder messageBuilder;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;
    private NavigationButton navButton;
	private @Inject TickUtils tickUtils;
	private @Inject LevelNotifier levelNotifier;
	private @Inject ItemNotifier itemNotifier;
	private @Inject LocationNotifier locationNotifier;
	private @Inject DeathNotifier deathNotifier;
	private boolean initialized = false;

	@Override
	protected void startUp() throws Exception
	{
        HAExporterPanel panel = injector.getInstance(HAExporterPanel.class);
		navButton = NavigationButton.builder()
			.tooltip("HA Exporter")
			.icon(ImageUtil.loadImageResource(getClass(), "/ha-exporter-icon.png"))
			.priority(420)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		panel.initialize();

		log.debug("Example started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		log.debug("Example stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "HA Exporter Enabled", null);
		}
		if (gameStateChanged.getGameState() == GameState.HOPPING)
		{
			initialized = false;
		}
		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN)
		{
			initialized = false;
			messageBuilder.resetData();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		var exp = statChanged.getSkill();
		log.debug(String.valueOf(exp));
		log.debug(String.valueOf(statChanged.getXp()));
		log.debug(statChanged.toString());

		if (statChanged.getSkill() == Skill.HITPOINTS){
			HealthData health = new HealthData(statChanged.getBoostedLevel(), client.getRealSkillLevel(Skill.HITPOINTS));
			messageBuilder.setData("health", health);
			tickUtils.sendNow();
		}
		if (statChanged.getSkill() == Skill.PRAYER){
			PrayerData prayer = new PrayerData(statChanged.getBoostedLevel(), client.getRealSkillLevel(Skill.PRAYER));
			messageBuilder.setData("prayer", prayer);
			tickUtils.sendNow();
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath actor) {
		deathNotifier.onActorDeath(actor);
	}

	@Subscribe
	public void onGameTick(GameTick gameTick){
		tickUtils.onTick();

		if (!initialized){
			initialize();
		}

		updateSpellbook();

		levelNotifier.onTick();
		itemNotifier.onTick();
		locationNotifier.onTick();
//		log.debug(config.homeassistantConnections());

		tickUtils.sendOnSendRate();
	}

	@Provides
	HAExporterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HAExporterConfig.class);
	}

	private void initialize(){
		if (client.getLocalPlayer() != null){
			String name = client.getLocalPlayer().getName();
			log.debug("USERNAME AFTER INIT: {}", name);

			messageBuilder.setData("world", String.valueOf(client.getWorld()));
			int accountType = client.getVarbitValue(4354);
			messageBuilder.setData("accounttype", String.valueOf(accountType));
            assert name != null;
            messageBuilder.setData("name", name);

			// Get and set Health & Prayer
			HealthData health = new HealthData(client.getBoostedSkillLevel(Skill.HITPOINTS), client.getRealSkillLevel(Skill.HITPOINTS));
			PrayerData prayer = new PrayerData(client.getBoostedSkillLevel(Skill.PRAYER), client.getRealSkillLevel(Skill.PRAYER));
			messageBuilder.setData("health", health);
			messageBuilder.setData("prayer", prayer);

			initialized = true;
		}
	}

	private void updateSpellbook(){
		int spellbookId = client.getVarbitValue(4070);
		SpellbookData spellbook = new SpellbookData(spellbookId);
		messageBuilder.setData("spellbook", spellbook);
	}
}
