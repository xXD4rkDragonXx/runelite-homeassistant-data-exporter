package haexporterplugin.data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class InventoryTest
{
	@Test
	public void testDefaultConstructor()
	{
		Inventory inventory = new Inventory();
		assertNull(inventory.getItems());
	}

	@Test
	public void testParameterizedConstructor()
	{
		List<ItemData> items = Arrays.asList(
			new ItemData("Coins", 995, 1, 1, 10000),
			new ItemData("Abyssal whip", 4151, 1650000, 72000, 1)
		);
		Inventory inventory = new Inventory(items);

		assertNotNull(inventory.getItems());
		assertEquals(2, inventory.getItems().size());
	}

	@Test
	public void testSetItems()
	{
		Inventory inventory = new Inventory();
		List<ItemData> items = new ArrayList<>();
		items.add(new ItemData("Lobster", 379, 100, 50, 5));
		inventory.setItems(items);

		assertEquals(1, inventory.getItems().size());
		assertEquals("Lobster", inventory.getItems().get(0).getName());
	}

	@Test
	public void testEmptyInventory()
	{
		Inventory inventory = new Inventory(new ArrayList<>());
		assertNotNull(inventory.getItems());
		assertTrue(inventory.getItems().isEmpty());
	}
}
