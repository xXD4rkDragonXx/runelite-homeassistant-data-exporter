package haexporterplugin.notifiers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import net.runelite.client.game.ItemManager;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
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
	private MessageBuilder messageBuilder;
	private BankNotifier notifier;

	@Before
	public void setUp()
	{
		when(config.includeBankValue()).thenReturn(true);
		when(client.getItemContainer(InventoryID.BANK)).thenReturn(bank);
		when(itemManager.getItemPrice(anyInt())).thenReturn(0L);
		when(itemManager.getItemPrice(ItemID.COINS)).thenReturn(1L);
		when(itemManager.getItemPrice(WHIP)).thenReturn(1_500_000L);

		Injector injector = TestUtils.injector(Map.of(Client.class, client, HAExporterConfig.class, config, ItemManager.class, itemManager));
		messageBuilder = injector.getInstance(MessageBuilder.class);
		notifier = injector.getInstance(BankNotifier.class);
	}

	@Test
	public void testSendsTheValueWhenTheBankOpens()
	{
		bankHolds(new Item(ItemID.COINS, 2_000_000_000), new Item(WHIP, 3));
		open();

		assertEquals(List.of(2_004_500_000L), sentValues());
	}

	// Placeholders are the item's id with quantity 0
	@Test
	public void testPlaceholdersAndEmptySlotsAddNothing()
	{
		bankHolds(new Item(WHIP, 0), new Item(-1, 0), new Item(ItemID.COINS, 10));
		open();

		assertEquals(List.of(10L), sentValues());
	}

	// Withdrawing gear and closing sends the bank as it was left; closing unchanged sends nothing more
	@Test
	public void testSendsAgainOnCloseOnlyWhenTheValueChanged()
	{
		bankHolds(new Item(WHIP, 3));
		open();
		bankHolds(new Item(WHIP, 1));
		notifier.onItemContainerChanged(InventoryID.BANK);
		notifier.onWidgetClosed(InterfaceID.BANKMAIN);

		bankHolds(new Item(WHIP, 1));
		open();
		notifier.onWidgetClosed(InterfaceID.BANKMAIN);

		assertEquals(List.of(4_500_000L, 1_500_000L, 1_500_000L), sentValues());
	}

	@Test
	public void testWaitsForTheItemsOrTwoTicks()
	{
		bankHolds(new Item(WHIP, 1));
		notifier.onWidgetLoaded(InterfaceID.BANKMAIN);
		notifier.onTick();
		assertEquals(List.of(), sentValues());
		notifier.onTick();
		assertEquals(List.of(1_500_000L), sentValues());
	}

	@Test
	public void testOtherInterfacesAndTheSwitchSendNothing()
	{
		bankHolds(new Item(WHIP, 1));
		notifier.onWidgetLoaded(InterfaceID.BANKSIDE);
		notifier.onTick();
		notifier.onTick();
		assertEquals(List.of(), sentValues());

		when(config.includeBankValue()).thenReturn(false);
		open();
		assertEquals(List.of(), sentValues());
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

	private List<Long> sentValues()
	{
		JsonObject root = gson.fromJson(messageBuilder.build(), JsonObject.class);
		List<Long> values = new ArrayList<>();
		JsonArray events = root.getAsJsonArray("events");
		if (events == null) return values;
		events.forEach(e -> {
			JsonObject event = e.getAsJsonObject();
			assertEquals("bankValue", event.get("type").getAsString());
			values.add(event.getAsJsonObject("data").get("value").getAsLong());
		});
		return values;
	}
}
