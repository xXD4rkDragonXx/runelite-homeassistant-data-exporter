package haexporterplugin.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class AccountTypeTest
{
	@Test
	public void testGetNormal()
	{
		assertEquals(AccountType.NORMAL, AccountType.get(0));
	}

	@Test
	public void testGetIronman()
	{
		assertEquals(AccountType.IRONMAN, AccountType.get(1));
	}

	@Test
	public void testGetUltimateIronman()
	{
		assertEquals(AccountType.ULTIMATE_IRONMAN, AccountType.get(2));
	}

	@Test
	public void testGetHardcoreIronman()
	{
		assertEquals(AccountType.HARDCORE_IRONMAN, AccountType.get(3));
	}

	@Test
	public void testGetGroupIronman()
	{
		assertEquals(AccountType.GROUP_IRONMAN, AccountType.get(4));
	}

	@Test
	public void testGetHardcoreGroupIronman()
	{
		assertEquals(AccountType.HARDCORE_GROUP_IRONMAN, AccountType.get(5));
	}

	@Test
	public void testGetUnrankedGroupIronman()
	{
		assertEquals(AccountType.UNRANKED_GROUP_IRONMAN, AccountType.get(6));
	}

	@Test
	public void testGetNegativeReturnsNull()
	{
		assertNull(AccountType.get(-1));
	}

	@Test
	public void testGetOutOfBoundsReturnsNull()
	{
		assertNull(AccountType.get(100));
	}

	@Test
	public void testIsHardcoreForHardcoreIronman()
	{
		assertTrue(AccountType.HARDCORE_IRONMAN.isHardcore());
	}

	@Test
	public void testIsHardcoreForHardcoreGroupIronman()
	{
		assertTrue(AccountType.HARDCORE_GROUP_IRONMAN.isHardcore());
	}

	@Test
	public void testIsHardcoreReturnsFalseForNormal()
	{
		assertFalse(AccountType.NORMAL.isHardcore());
	}

	@Test
	public void testIsHardcoreReturnsFalseForIronman()
	{
		assertFalse(AccountType.IRONMAN.isHardcore());
	}

	@Test
	public void testIsHardcoreReturnsFalseForGroupIronman()
	{
		assertFalse(AccountType.GROUP_IRONMAN.isHardcore());
	}

	@Test
	public void testAllValuesAccessible()
	{
		AccountType[] values = AccountType.values();
		assertEquals(7, values.length);
	}
}
