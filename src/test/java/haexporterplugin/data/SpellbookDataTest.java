package haexporterplugin.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpellbookDataTest
{
	@Test
	public void testSpellbookNames()
	{
		String[] names = {"standard", "ancient", "lunar", "arceuus"};
		for (int id = 0; id < names.length; id++)
		{
			assertEquals(names[id], SpellbookData.getSpellbookName(id));
		}
		assertEquals("unknown", SpellbookData.getSpellbookName(-1));
		assertEquals("unknown", SpellbookData.getSpellbookName(99));
	}
}
