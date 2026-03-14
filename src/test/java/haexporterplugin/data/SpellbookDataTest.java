package haexporterplugin.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class SpellbookDataTest
{
	@Test
	public void testStandardSpellbook()
	{
		assertEquals("standard", SpellbookData.getSpellbookName(0));
	}

	@Test
	public void testAncientSpellbook()
	{
		assertEquals("ancient", SpellbookData.getSpellbookName(1));
	}

	@Test
	public void testLunarSpellbook()
	{
		assertEquals("lunar", SpellbookData.getSpellbookName(2));
	}

	@Test
	public void testArceuusSpellbook()
	{
		assertEquals("arceuus", SpellbookData.getSpellbookName(3));
	}

	@Test
	public void testUnknownSpellbook()
	{
		assertEquals("unknown", SpellbookData.getSpellbookName(99));
	}

	@Test
	public void testNegativeIdReturnsUnknown()
	{
		assertEquals("unknown", SpellbookData.getSpellbookName(-1));
	}

	@Test
	public void testConstructorCreatesInstance()
	{
		SpellbookData data = new SpellbookData(0);
		assertNotNull(data);
	}

	@Test
	public void testAllValidIds()
	{
		// Exhaustively test all valid spellbook IDs
		String[] expected = {"standard", "ancient", "lunar", "arceuus"};
		for (int i = 0; i < expected.length; i++)
		{
			assertEquals(expected[i], SpellbookData.getSpellbookName(i));
		}
	}
}
