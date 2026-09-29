package haexporterplugin.utils;

import haexporterplugin.data.ItemData;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ItemUtilsTest
{
	private static final long ABOVE_INT_MAX = 3_000_000_000L;

	@Test
	public void testGetStackGePriceLargeSingleStackDoesNotOverflow()
	{
		ItemData item = new ItemData("Test item", 1, 30000, 1000, 100000);

		assertEquals(3_000_000_000L, ItemUtils.getStackGePrice(item));
		assertEquals(3_000_000_000L, ItemUtils.getStackGePrice(Collections.singletonList(item)));
	}

	@Test
	public void testGetStackGePriceSumsMultipleLargeStacks()
	{
		ItemData first = new ItemData("First", 1, 30000, 1000, 100000);
		ItemData second = new ItemData("Second", 2, 2_000_000, 1000, 2000);

		assertEquals(7_000_000_000L, ItemUtils.getStackGePrice(Arrays.asList(first, second)));
	}

	@Test
	public void testGetStackGePriceMaxIntPriceTimesTwo()
	{
		ItemData item = new ItemData("Max", 1, Integer.MAX_VALUE, 0, 2);

		assertEquals(2L * Integer.MAX_VALUE, ItemUtils.getStackGePrice(item));
	}

	@Test
	public void testGetStackGePriceSmallValues()
	{
		ItemData coins = new ItemData("Coins", 995, 1, 1, 50000);
		ItemData whip = new ItemData("Abyssal whip", 4151, 1650000, 72000, 1);

		assertEquals(1_700_000L, ItemUtils.getStackGePrice(Arrays.asList(coins, whip)));
	}

	@Test
	public void testGetStackGePriceEmptyList()
	{
		assertEquals(0L, ItemUtils.getStackGePrice(Collections.emptyList()));
	}

	@Test
	public void testGetStackGePriceWithPriceAboveIntMax()
	{
		ItemData item = new ItemData("Pricey", 1, ABOVE_INT_MAX, 0, 2);

		assertEquals(2 * ABOVE_INT_MAX, ItemUtils.getStackGePrice(item));
	}

	@Test
	public void testGetPriceKeepsPriceAboveIntMax()
	{
		ItemManager itemManager = mock(ItemManager.class);
		when(itemManager.getItemPrice(1)).thenReturn(ABOVE_INT_MAX);

		assertEquals(ABOVE_INT_MAX, ItemUtils.getPrice(1, itemManager));
	}

	@Test
	public void testCreateItemDataKeepsPriceAboveIntMax()
	{
		ItemManager itemManager = mock(ItemManager.class);
		when(itemManager.getItemPrice(1)).thenReturn(ABOVE_INT_MAX);
		ItemComposition composition = mock(ItemComposition.class);
		when(composition.getId()).thenReturn(1);
		when(composition.getName()).thenReturn("Pricey");
		when(composition.getHaPrice()).thenReturn(1000);

		ItemData item = ItemUtils.createItemData(composition, 3, itemManager);

		assertEquals(ABOVE_INT_MAX, item.getGePrice());
		assertEquals(1000, item.getHaPrice());
		assertEquals(3, item.getQuantity());
	}

	@Test
	public void testGetInventoryItemsSetsSlotIndexAndSkipsEmptySlots()
	{
		Client client = mock(Client.class);
		ItemContainer container = mock(ItemContainer.class);
		when(client.getItemContainer(InventoryID.INV)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{
			new Item(6739, 1),
			new Item(-1, 0),
			new Item(-1, 0),
			new Item(-1, 0),
			new Item(-1, 0),
			new Item(2347, 1)
		});
		ItemManager itemManager = mockItemManager(6739, "Dragon axe", 2347, "Hammer");

		List<ItemData> items = ItemUtils.getInventoryItems(client, itemManager);

		assertEquals(2, items.size());
		assertEquals("Dragon axe", items.get(0).getName());
		assertEquals(Integer.valueOf(0), items.get(0).getInventorySlot());
		assertEquals("Hammer", items.get(1).getName());
		assertEquals(Integer.valueOf(5), items.get(1).getInventorySlot());
	}

	@Test
	public void testGetInventoryItemsWithoutContainerReturnsEmptyList()
	{
		Client client = mock(Client.class);
		when(client.getItemContainer(InventoryID.INV)).thenReturn(null);

		assertTrue(ItemUtils.getInventoryItems(client, mock(ItemManager.class)).isEmpty());
	}

	@Test
	public void testGetEquippedItemsHasNoInventorySlot()
	{
		Client client = mock(Client.class);
		ItemContainer container = mock(ItemContainer.class);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{new Item(6739, 1)});
		ItemManager itemManager = mockItemManager(6739, "Dragon axe", 0, null);

		List<ItemData> items = ItemUtils.getEquippedItems(client, itemManager);

		assertEquals(1, items.size());
		assertEquals("HEAD", items.get(0).getEquipmentSlot());
		assertNull(items.get(0).getInventorySlot());
	}

	private static ItemManager mockItemManager(int firstId, String firstName, int secondId, String secondName)
	{
		ItemManager itemManager = mock(ItemManager.class);
		ItemComposition first = mock(ItemComposition.class);
		when(first.getId()).thenReturn(firstId);
		when(first.getName()).thenReturn(firstName);
		when(itemManager.getItemComposition(firstId)).thenReturn(first);
		if (secondName != null)
		{
			ItemComposition second = mock(ItemComposition.class);
			when(second.getId()).thenReturn(secondId);
			when(second.getName()).thenReturn(secondName);
			when(itemManager.getItemComposition(secondId)).thenReturn(second);
		}
		return itemManager;
	}
}
