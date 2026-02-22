package haexporterplugin;

import com.google.inject.Provides;
import javax.inject.Inject;

import haexporterplugin.data.HealthData;
import haexporterplugin.data.PrayerData;
import haexporterplugin.data.SpellbookData;
import haexporterplugin.notifiers.*;
import haexporterplugin.utils.EasterEggUtils;
import haexporterplugin.utils.MessageBuilder;
import haexporterplugin.utils.TickUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.*;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
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

    private NavigationButton navButton;
	private @Inject TickUtils tickUtils;
	private @Inject LevelNotifier levelNotifier;
	private @Inject ItemNotifier itemNotifier;
	private @Inject LootNotifier lootNotifier;
	private @Inject LocationNotifier locationNotifier;
	private @Inject DeathNotifier deathNotifier;
	private @Inject EasterEggUtils easterEggUtils;
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
		lootNotifier.init();

		easterEggUtils.init();

		log.debug("HA Exporter Started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		messageBuilder.addEvent("clientShutdown", "Disabled");
		tickUtils.sendNow();
		easterEggUtils.shutDown();
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		if (!initialized) return;
		messageBuilder.addEvent("clientShutdown", "Shutdown");
		tickUtils.sendShutdown();
	}

	@Subscribe
	public void onPlayerLootReceived(PlayerLootReceived playerLootReceived) {
		lootNotifier.onPlayerLootReceived(playerLootReceived);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		messageBuilder.setState(gameStateChanged.getGameState());
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
			messageBuilder.addEvent("clientShutdown", "Logout");
			tickUtils.sendNow();
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

	@Subscribe(priority = 1) // run before the base loot tracker plugin
	public void onChatMessage(ChatMessage chatMessage) {
		String source = chatMessage.getName() != null && !chatMessage.getName().isEmpty() ? chatMessage.getName() : chatMessage.getSender();
		switch (chatMessage.getType()){
			case GAMEMESSAGE:
				if ("runelite".equals(source)) {
					// filter out plugin-sourced chat messages
					return;
				}
				lootNotifier.onGameMessage(chatMessage.getMessage());
				deathNotifier.onGameMessage(chatMessage.getMessage());
				break;
		}

	}

	@Subscribe(priority = 1) // run before the base loot tracker plugin
	public void onServerNpcLoot(ServerNpcLoot serverNpcLoot) {
		// temporarily only use new event when needed
		int npcId = serverNpcLoot.getComposition().getId();
		var name = serverNpcLoot.getComposition().getName();
		if (npcId != NpcID.YAMA && npcId != NpcID.HESPORI && !name.startsWith("Hallowed Sepulchre")) {
			return;
		}

		lootNotifier.onServerNpcLoot(serverNpcLoot);
	}

	@Subscribe(priority = 1) // run before the base loot tracker plugin
	public void onNpcLootReceived(NpcLootReceived npcLootReceived) {
		if (npcLootReceived.getNpc().getId() == NpcID.YAMA) {
			// handled by ServerNpcLoot, but return just in case
			return;
		}

		lootNotifier.onNpcLootReceived(npcLootReceived);
	}

	@Subscribe
	public void onLootReceived(LootReceived lootReceived) {
		lootNotifier.onLootReceived(lootReceived);
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event) {
		deathNotifier.onScript(event);
	}

	@Subscribe
	public void onActorDeath(ActorDeath actor) {
		deathNotifier.onActorDeath(actor);
		if (client.getLocalPlayer() == actor.getActor()){
			easterEggUtils.playGarbage();
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event) {
		deathNotifier.onInteraction(event);
	}

	@Subscribe
	public void onGameTick(GameTick gameTick){
		tickUtils.onTick();

		updateSpellbook();

		levelNotifier.onTick();
		itemNotifier.onTick();
		locationNotifier.onTick();

		if (!initialized){
			initialize();
		}


//		log.debug(config.homeassistantConnections());

		tickUtils.sendOnSendRate();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		messageBuilder.setTickDelay(config.sendRate());
		lootNotifier.onConfigChanged(event.getKey(), event.getNewValue());
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
			messageBuilder.setTickDelay(config.sendRate());

			tickUtils.sendNow();

			initialized = true;
		}
	}

	private void updateSpellbook(){
		int spellbookId = client.getVarbitValue(4070);
		SpellbookData spellbook = new SpellbookData(spellbookId);
		messageBuilder.setData("spellbook", spellbook);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		log.debug("EVENT TRIGGERED, {} {} {} {}",event.getMenuOption(), event.getId(), ItemID.KEBAB, event.getItemId());
		if (event.getMenuOption().equals("Eat") && (event.getItemId() == ItemID.KEBAB || event.getItemId() == ItemID.UGTHANKI_KEBAB || event.getItemId() == ItemID.SUPER_KEBAB || event.getItemId() == ItemID.VARLAMORIAN_KEBAB)) {
			easterEggUtils.playKebab();
		}
	}
}
