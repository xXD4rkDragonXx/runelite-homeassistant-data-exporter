package haexporterplugin.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class LootCriteriaTest
{
	@Test
	public void testAllValues()
	{
		LootCriteria[] values = LootCriteria.values();
		assertEquals(4, values.length);
	}

	@Test
	public void testValueOf()
	{
		assertEquals(LootCriteria.ALLOWLIST, LootCriteria.valueOf("ALLOWLIST"));
		assertEquals(LootCriteria.DENYLIST, LootCriteria.valueOf("DENYLIST"));
		assertEquals(LootCriteria.VALUE, LootCriteria.valueOf("VALUE"));
		assertEquals(LootCriteria.RARITY, LootCriteria.valueOf("RARITY"));
	}

	@Test
	public void testEnumConstants()
	{
		assertNotNull(LootCriteria.ALLOWLIST);
		assertNotNull(LootCriteria.DENYLIST);
		assertNotNull(LootCriteria.VALUE);
		assertNotNull(LootCriteria.RARITY);
	}
}
