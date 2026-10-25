package haexporterplugin.notifiers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Injector;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.TestUtils;
import haexporterplugin.utils.MessageBuilder;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BankNotifierTest
{
	private static final int WHIP = ItemID.ABYSSAL_WHIP;

	private final Gson gson = new Gson();
	private final Client client = mock(Client.class);
	private final HAExporterConfig config = mock(HAExporterConfig.class);
	private final ItemManager itemManager = mock(ItemManager.class);
	private final ItemContainer bank = mock(ItemContainer.class);
	private final ConfigManager configManager = mock(ConfigManager.class);
	// The account's RuneLite profile
	private final Map<String, Object> profile = new HashMap<>();
	private MessageBuilder messageBuilder;
	private BankNotifier notifier;
	// The bank value of every message built so far
	private final List<Long> seen = new ArrayList<>();

	@Before
	public void setUp()
	{
		when(config.includeBankValue()).thenReturn(true);
		when(client.getItemContainer(InventoryID.BANK)).thenReturn(bank);
		when(itemManager.getItemPrice(anyInt())).thenReturn(0L);
		when(itemManager.getItemPrice(ItemID.COINS)).thenReturn(1L);
		when(itemManager.getItemPrice(WHIP)).thenReturn(1_500_000L);
		doAnswer(i -> profile.put(i.getArgument(1), i.getArgument(2)))
			.when(configManager).setRSProfileConfiguration(eq(BankNotifier.CONFIG_GROUP), anyString(), any(Object.class));
		when(configManager.getRSProfileConfiguration(eq(BankNotifier.CONFIG_GROUP), anyString(), eq(Long.class)))
			.thenAnswer(i -> profile.get(i.<String>getArgument(1)));

		Injector injector = TestUtils.injector(Map.of(Client.class, client, HAExporterConfig.class, config,
			ItemManager.class, itemManager, ConfigManager.class, configManager));
		messageBuilder = injector.getInstance(MessageBuilder.class);
		notifier = injector.getInstance(BankNotifier.class);
	}

	// A bank is easily worth more than Integer.MAX_VALUE
	@Test
	public void testReadsTheValueWhenTheBankOpens()
	{
		bankHolds(new Item(ItemID.COINS, 2_000_000_000), new Item(WHIP, 3));
		open();

		assertEquals(Long.valueOf(2_004_500_000L), bankInMessage());
	}

	// Placeholders are the item's id with quantity 0
	@Test
	public void testPlaceholdersAndEmptySlotsAddNothing()
	{
		bankHolds(new Item(WHIP, 0), new Item(-1, 0), new Item(ItemID.COINS, 10));
		open();

		assertEquals(Long.valueOf(10L), bankInMessage());
	}

	// Withdrawing gear and closing keeps the bank as it was left
	@Test
	public void testUpdatesOnCloseWhenTheValueChanged()
	{
		bankHolds(new Item(WHIP, 3));
		open();
		bankInMessage();
		bankHolds(new Item(WHIP, 1));
		notifier.onItemContainerChanged(InventoryID.BANK);
		notifier.onWidgetClosed(InterfaceID.BANKMAIN);
		bankInMessage();

		assertEquals(List.of(4_500_000L, 1_500_000L), seen);
	}

	// The field is sent with every message, not only after a bank visit
	@Test
	public void testEveryMessageCarriesTheLastReading()
	{
		bankHolds(new Item(WHIP, 1));
		open();
		notifier.onWidgetClosed(InterfaceID.BANKMAIN);
		bankInMessage();
		bankInMessage();

		assertEquals(List.of(1_500_000L, 1_500_000L), seen);
		JsonObject sent = gson.fromJson(messageBuilder.build(), JsonObject.class).getAsJsonObject("player").getAsJsonObject("bank");
		assertTrue(sent.get("readAt").getAsLong() > 0);
	}

	// Logging out drops the player's data; the next login starts with the last reading
	@Test
	public void testTheReadingSurvivesALogout()
	{
		bankHolds(new Item(WHIP, 2));
		open();
		long readAt = gson.fromJson(messageBuilder.build(), JsonObject.class)
			.getAsJsonObject("player").getAsJsonObject("bank").get("readAt").getAsLong();

		messageBuilder.resetData();
		notifier.reset();
		assertEquals(null, bankInMessage());

		notifier.restore();
		JsonObject sent = gson.fromJson(messageBuilder.build(), JsonObject.class).getAsJsonObject("player").getAsJsonObject("bank");
		assertEquals(3_000_000L, sent.get("value").getAsLong());
		assertEquals(readAt, sent.get("readAt").getAsLong());
	}

	@Test
	public void testWaitsForTheItemsOrTwoTicks()
	{
		bankHolds(new Item(WHIP, 1));
		notifier.onWidgetLoaded(InterfaceID.BANKMAIN);
		notifier.onTick();
		assertEquals(null, bankInMessage());
		notifier.onTick();
		assertEquals(Long.valueOf(1_500_000L), bankInMessage());
	}

	@Test
	public void testOtherInterfacesAndTheSwitchSendNothing()
	{
		bankHolds(new Item(WHIP, 1));
		notifier.onWidgetLoaded(InterfaceID.BANKSIDE);
		notifier.onTick();
		notifier.onTick();
		assertEquals(null, bankInMessage());

		when(config.includeBankValue()).thenReturn(false);
		open();
		assertEquals(null, bankInMessage());
		assertFalse(profile.containsKey(BankNotifier.VALUE_KEY));
	}

	private void bankHolds(Item... items)
	{
		when(bank.getItems()).thenReturn(items);
	}

	private void open()
	{
		notifier.onWidgetLoaded(InterfaceID.BANKMAIN);
		notifier.onItemContainerChanged(InventoryID.BANK);
		notifier.onTick();
	}

	// The bank value in the message as it would be built now
	private Long bankInMessage()
	{
		JsonObject player = gson.fromJson(messageBuilder.build(), JsonObject.class).getAsJsonObject("player");
		if (player == null || !player.has("bank")) return null;
		long value = player.getAsJsonObject("bank").get("value").getAsLong();
		seen.add(value);
		return value;
	}
}
