package haexporterplugin.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class AccountTypeTest
{
	// The game sets the varbit value, so the order of the constants matters
	@Test
	public void testGetMapsTheVarbitValue()
	{
		AccountType[] expected = {
			AccountType.NORMAL, AccountType.IRONMAN, AccountType.ULTIMATE_IRONMAN, AccountType.HARDCORE_IRONMAN,
			AccountType.GROUP_IRONMAN, AccountType.HARDCORE_GROUP_IRONMAN, AccountType.UNRANKED_GROUP_IRONMAN
		};
		for (int value = 0; value < expected.length; value++)
		{
			assertEquals(expected[value], AccountType.get(value));
		}
		assertNull(AccountType.get(-1));
		assertNull(AccountType.get(expected.length));
	}

	@Test
	public void testOnlyHardcoreTypesAreHardcore()
	{
		for (AccountType type : AccountType.values())
		{
			assertEquals(type.name(), type.name().startsWith("HARDCORE_"), type.isHardcore());
		}
	}
}
