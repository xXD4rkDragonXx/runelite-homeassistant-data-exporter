package haexporterplugin;

import com.google.inject.Provides;
import javax.inject.Inject;

import haexporterplugin.notifiers.LevelNotifier;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

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

	private @Inject LevelNotifier levelNotifier;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Example started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Example stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "HA Exporter Enabled", null);
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
		levelNotifier.onTick();
	}

	@Provides
	HAExporterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HAExporterConfig.class);
	}
}
