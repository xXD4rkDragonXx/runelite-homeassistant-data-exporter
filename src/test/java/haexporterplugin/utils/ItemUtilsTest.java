package haexporterplugin.utils;

import haexporterplugin.data.ItemData;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ItemUtilsTest
{
	private static final long ABOVE_INT_MAX = 3_000_000_000L;

	// Stack values easily pass Integer.MAX_VALUE
	@Test
	public void testStackGePriceIsALongSum()
	{
		ItemData stack = new ItemData("Test item", 1, 30000, 1000, 100000);
		assertEquals(3_000_000_000L, ItemUtils.getStackGePrice(stack));
		assertEquals(3_000_000_000L, ItemUtils.getStackGePrice(List.of(stack)));
		assertEquals(2 * ABOVE_INT_MAX, ItemUtils.getStackGePrice(new ItemData("Pricey", 1, ABOVE_INT_MAX, 0, 2)));

		ItemData coins = new ItemData("Coins", 995, 1, 1, 50000);
		ItemData whip = new ItemData("Abyssal whip", 4151, 1650000, 72000, 1);
		assertEquals(1_700_000L, ItemUtils.getStackGePrice(List.of(coins, whip)));
	}

	@Test
	public void testCreateItemDataKeepsPriceAboveIntMax()
	{
		ItemManager itemManager = mock(ItemManager.class);
		when(itemManager.getItemPrice(1)).thenReturn(ABOVE_INT_MAX);
		ItemComposition composition = item(1, "Pricey");
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
		ItemManager itemManager = itemManager(Map.of(6739, "Dragon axe", 2347, "Hammer"));

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

		List<ItemData> items = ItemUtils.getEquippedItems(client, itemManager(Map.of(6739, "Dragon axe")));

		assertEquals(1, items.size());
		assertEquals("HEAD", items.get(0).getEquipmentSlot());
		assertNull(items.get(0).getInventorySlot());
	}

	private static ItemManager itemManager(Map<Integer, String> names)
	{
		ItemManager itemManager = mock(ItemManager.class);
		names.forEach((id, name) ->
		{
			ItemComposition composition = item(id, name);
			when(itemManager.getItemComposition(id)).thenReturn(composition);
		});
		return itemManager;
	}

	private static ItemComposition item(int id, String name)
	{
		ItemComposition composition = mock(ItemComposition.class);
		when(composition.getId()).thenReturn(id);
		when(composition.getName()).thenReturn(name);
		return composition;
	}
}
