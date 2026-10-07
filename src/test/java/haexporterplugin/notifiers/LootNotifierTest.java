package haexporterplugin.notifiers;

import com.google.inject.Injector;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.TestUtils;
import haexporterplugin.utils.MessageBuilder;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class LootNotifierTest
{
	private static final int COIN_POUCH_ID = 1;
	private static final int ARROW_ID = 2;
	private static final int BOLT_ID = 3;

	private final Client client = mock(Client.class);
	private final HAExporterConfig config = mock(HAExporterConfig.class);
	private MessageBuilder messageBuilder;
	private LootNotifier lootNotifier;

	@Before
	public void setUp()
	{
		when(config.minLootValue()).thenReturn(25_000);
		when(config.lootItemAllowlist()).thenReturn("");
		when(config.lootItemDenylist()).thenReturn("");
		when(config.lootSourceDenylist()).thenReturn("");
		when(config.lootIncludePlayer()).thenReturn(true);
		when(config.lootIncludePickpocket()).thenReturn(true);
		when(config.lootIncludePkChest()).thenReturn(true);

		ItemComposition pouch = item(COIN_POUCH_ID, "Coin pouch");
		ItemComposition arrow = item(ARROW_ID, "Dragon arrow");
		ItemComposition bolt = item(BOLT_ID, "Dragon bolts");
		ItemManager itemManager = mock(ItemManager.class);
		when(itemManager.getItemComposition(anyInt())).thenAnswer(inv ->
		{
			int id = inv.getArgument(0);
			return id == COIN_POUCH_ID ? pouch : id == ARROW_ID ? arrow : bolt;
		});
		when(itemManager.getItemPrice(COIN_POUCH_ID)).thenReturn(50_000L);
		when(itemManager.getItemPrice(ARROW_ID)).thenReturn(15_000L);
		when(itemManager.getItemPrice(BOLT_ID)).thenReturn(15_000L);

		Injector injector = TestUtils.injector(Map.of(Client.class, client, ItemManager.class, itemManager, HAExporterConfig.class, config));
		messageBuilder = injector.getInstance(MessageBuilder.class);
		lootNotifier = injector.getInstance(LootNotifier.class);
		lootNotifier.init();
	}

	@Test
	public void testPickpocketLootIsSentUntilTurnedOff()
	{
		pickpocket();
		assertEquals(List.of("loot"), eventTypes());

		when(config.lootIncludePickpocket()).thenReturn(false);
		pickpocket();
		assertEquals(List.of("loot"), eventTypes());
	}

	@Test
	public void testPkChestUsesTotalValueUntilTurnedOff()
	{
		pkChest();
		assertEquals(List.of("pkLoot"), eventTypes());

		when(config.lootIncludePkChest()).thenReturn(false);
		pkChest();
		assertEquals(List.of("pkLoot"), eventTypes());
	}

	@Test
	public void testPkLootCanBeTurnedOff()
	{
		when(config.lootIncludePlayer()).thenReturn(false);
		Player victim = mock(Player.class);
		when(victim.getName()).thenReturn("Victim");

		lootNotifier.onPlayerLootReceived(new PlayerLootReceived(victim, List.of(new ItemStack(COIN_POUCH_ID, 1))));

		assertTrue(eventTypes().isEmpty());
		// Returns before the safe-area check even looks at the player
		verify(client, never()).getLocalPlayer();
	}

	private void pickpocket()
	{
		lootNotifier.onLootReceived(new LootReceived("Master Farmer", -1, LootRecordType.PICKPOCKET,
				List.of(new ItemStack(COIN_POUCH_ID, 1)), 1, null));
	}

	// Two items each below the 25k minimum, together above it
	private void pkChest()
	{
		lootNotifier.onLootReceived(new LootReceived("Loot Chest", -1, LootRecordType.EVENT,
				List.of(new ItemStack(ARROW_ID, 1), new ItemStack(BOLT_ID, 1)), 1, null));
	}

	private List<Object> eventTypes()
	{
		return messageBuilder.getRoot().getEvents().stream()
				.map(event -> ((Map<?, ?>) event).get("type"))
				.collect(Collectors.toList());
	}

	private static ItemComposition item(int id, String name)
	{
		ItemComposition composition = mock(ItemComposition.class);
		when(composition.getId()).thenReturn(id);
		when(composition.getName()).thenReturn(name);
		return composition;
	}
}
