package haexporterplugin;

import com.google.inject.Provides;
import javax.inject.Inject;

import haexporterplugin.notifiers.ItemNotifier;
import haexporterplugin.notifiers.LevelNotifier;
import haexporterplugin.utils.MessageBuilder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.util.Arrays;

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
	private @Inject LevelNotifier levelNotifier;
	private @Inject ItemNotifier itemNotifier;
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
	}

	@Subscribe
	public void onGameTick(GameTick gameTick){
		if (!initialized){
			initialize();
		}

		levelNotifier.onTick();
		itemNotifier.onTick();
//		log.debug(config.homeassistantConnections());
//		var itemContainers = client.getItemContainers();
//		log.debug("Inventory Content: {}", Arrays.toString(client.getItemContainer(InventoryID.INV).getItems()));
//		log.debug("Equipped Gear: {}", Arrays.toString(client.getItemContainer(InventoryID.WORN).getItems()));
//		for (var item : client.getItemContainer(InventoryID.INV).getItems()){
//			ItemComposition ic = itemManager.getItemComposition(item.getId());
//			log.debug("Item Data: {}, {}, {}", ic.getName(), itemManager.getItemPrice(ic.getId()), getItemImageUrl(ic.getId()));
//		}
//		for (var item : client.getItemContainer(InventoryID.WORN).getItems()){
//			ItemComposition ic = itemManager.getItemComposition(item.getId());
//			log.debug("Item Data: {}, {}, {}", ic.getName(), itemManager.getItemPrice(ic.getId()), getItemImageUrl(ic.getId()));
//		}
	}

	final String ITEM_CACHE_BASE_URL = "https://static.runelite.net/cache/item/";
	public String getItemImageUrl(int itemId) {
		return ITEM_CACHE_BASE_URL + "icon/" + itemId + ".png";
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

			initialized = true;
		}
	}
}
