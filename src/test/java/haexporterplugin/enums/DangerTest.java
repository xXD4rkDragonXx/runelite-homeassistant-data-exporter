package haexporterplugin.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class DangerTest
{
	@Test
	public void testSafeExists()
	{
		assertNotNull(Danger.SAFE);
	}

	@Test
	public void testDangerousExists()
	{
		assertNotNull(Danger.DANGEROUS);
	}

	@Test
	public void testExceptionalExists()
	{
		assertNotNull(Danger.EXCEPTIONAL);
	}

	@Test
	public void testAllValues()
	{
		Danger[] values = Danger.values();
		assertEquals(3, values.length);
	}

	@Test
	public void testValueOf()
	{
		assertEquals(Danger.SAFE, Danger.valueOf("SAFE"));
		assertEquals(Danger.DANGEROUS, Danger.valueOf("DANGEROUS"));
		assertEquals(Danger.EXCEPTIONAL, Danger.valueOf("EXCEPTIONAL"));
	}
}
