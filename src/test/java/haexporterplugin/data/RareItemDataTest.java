package haexporterplugin.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class RareItemDataTest
{
	@Test
	public void testConstructor()
	{
		RareItemData rareItem = new RareItemData("Dragon warhammer", 13576, 30000000, 72000, 1, 0.002);

		assertEquals("Dragon warhammer", rareItem.getName());
		assertEquals(13576, rareItem.getId());
		assertEquals(30000000, rareItem.getGePrice());
		assertEquals(72000, rareItem.getHaPrice());
		assertEquals(1, rareItem.getQuantity());
		assertEquals(0.002, rareItem.getRarity(), 0.00001);
	}

	@Test
	public void testOfFactoryMethod()
	{
		ItemData base = new ItemData("Pet snakeling", 12921, 0, 0, 1);
		RareItemData rare = RareItemData.of(base, 0.00025);

		assertEquals("Pet snakeling", rare.getName());
		assertEquals(12921, rare.getId());
		assertEquals(0, rare.getGePrice());
		assertEquals(0, rare.getHaPrice());
		assertEquals(1, rare.getQuantity());
		assertEquals(0.00025, rare.getRarity(), 0.0000001);
	}

	@Test
	public void testExtendsItemData()
	{
		RareItemData rareItem = new RareItemData("Test", 1, 100, 50, 1, 0.5);
		assertTrue(rareItem instanceof ItemData);
	}

	@Test
	public void testSameInstanceIsEqual()
	{
		RareItemData a = new RareItemData("Test", 1, 100, 50, 1, 0.5);
		assertEquals(a, a);
	}

	@Test
	public void testDifferentInstancesNotEqual()
	{
		// ItemData doesn't override equals, so callSuper=true uses reference equality
		RareItemData a = new RareItemData("Test", 1, 100, 50, 1, 0.5);
		RareItemData b = new RareItemData("Test", 1, 100, 50, 1, 0.5);
		assertNotEquals(a, b);
	}

	@Test
	public void testToStringContainsRarity()
	{
		RareItemData rareItem = new RareItemData("Test", 1, 100, 50, 1, 0.5);
		assertTrue(rareItem.toString().contains("0.5"));
	}
}
