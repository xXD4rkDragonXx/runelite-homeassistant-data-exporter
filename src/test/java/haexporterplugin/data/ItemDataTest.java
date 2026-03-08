package haexporterplugin.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class ItemDataTest
{
	@Test
	public void testDefaultConstructorQuantityIsOne()
	{
		ItemData item = new ItemData();
		assertEquals(1, item.getQuantity());
	}

	@Test
	public void testParameterizedConstructor()
	{
		ItemData item = new ItemData("Abyssal whip", 4151, 1650000, 72000, 1);

		assertEquals("Abyssal whip", item.getName());
		assertEquals(4151, item.getId());
		assertEquals(1650000, item.getGePrice());
		assertEquals(72000, item.getHaPrice());
		assertEquals(1, item.getQuantity());
	}

	@Test
	public void testCopyConstructor()
	{
		ItemData original = new ItemData("Dragon defender", 12954, 500000, 68000, 1);
		original.setEquipmentSlot("SHIELD");

		ItemData copy = new ItemData(original);

		assertEquals("Dragon defender", copy.getName());
		assertEquals(12954, copy.getId());
		assertEquals(500000, copy.getGePrice());
		assertEquals(68000, copy.getHaPrice());
		assertEquals(1, copy.getQuantity());
		assertEquals("SHIELD", copy.getEquipmentSlot());
	}

	@Test
	public void testSetters()
	{
		ItemData item = new ItemData();
		item.setName("Coins");
		item.setId(995);
		item.setGePrice(1);
		item.setHaPrice(1);
		item.setQuantity(10000);
		item.setEquipmentSlot(null);

		assertEquals("Coins", item.getName());
		assertEquals(995, item.getId());
		assertEquals(1, item.getGePrice());
		assertEquals(1, item.getHaPrice());
		assertEquals(10000, item.getQuantity());
		assertNull(item.getEquipmentSlot());
	}

	@Test
	public void testCopyConstructorIsDeepEnough()
	{
		ItemData original = new ItemData("Test", 1, 100, 50, 5);
		ItemData copy = new ItemData(original);

		copy.setName("Modified");
		copy.setQuantity(10);

		// Original should remain unchanged
		assertEquals("Test", original.getName());
		assertEquals(5, original.getQuantity());
	}
}
