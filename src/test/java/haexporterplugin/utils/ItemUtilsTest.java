package haexporterplugin.utils;

import haexporterplugin.data.ItemData;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

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
}
