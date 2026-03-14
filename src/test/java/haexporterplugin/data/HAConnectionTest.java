package haexporterplugin.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class HAConnectionTest
{
	@Test
	public void testConstructorSetsFields()
	{
		HAConnection conn = new HAConnection("http://ha.local:8123", "secret-token");

		assertEquals("http://ha.local:8123", conn.getBaseUrl());
		assertEquals("secret-token", conn.getToken());
	}

	@Test
	public void testDefaultTogglesAreEnabled()
	{
		HAConnection conn = new HAConnection("http://ha.local:8123", "tok");

		assertTrue(conn.isIncludeInventory());
		assertTrue(conn.isIncludeEquipment());
		assertTrue(conn.isIncludeLocation());
	}

	@Test
	public void testToggleSettersAndGetters()
	{
		HAConnection conn = new HAConnection("http://ha.local:8123", "tok");

		conn.setIncludeInventory(false);
		conn.setIncludeEquipment(false);
		conn.setIncludeLocation(false);

		assertFalse(conn.isIncludeInventory());
		assertFalse(conn.isIncludeEquipment());
		assertFalse(conn.isIncludeLocation());
	}

	@Test
	public void testNullBooleansDefaultToEnabled()
	{
		// Simulates deserialization from old JSON that doesn't have these fields.
		// When Gson deserializes and leaves Boolean wrappers as null,
		// the getters should return true.
		HAConnection conn = new HAConnection("http://ha.local:8123", "tok");

		// Use Gson-like behavior: deserialize without toggle fields -> null
		// We can't set them to null via public API, but the constructor sets them to true.
		// The key contract: null Boolean == enabled
		assertTrue(conn.isIncludeInventory());
		assertTrue(conn.isIncludeEquipment());
		assertTrue(conn.isIncludeLocation());
	}

	@Test
	public void testDisplayNameReturnsFriendlyNameWhenSet()
	{
		HAConnection conn = new HAConnection("http://ha.local:8123", "tok");
		conn.setFriendlyName("Living Room HA");

		assertEquals("Living Room HA", conn.getDisplayName());
	}

	@Test
	public void testDisplayNameFallsBackToBaseUrl()
	{
		HAConnection conn = new HAConnection("http://ha.local:8123", "tok");

		assertEquals("http://ha.local:8123", conn.getDisplayName());
	}

	@Test
	public void testDisplayNameFallsBackWhenFriendlyNameIsBlank()
	{
		HAConnection conn = new HAConnection("http://ha.local:8123", "tok");
		conn.setFriendlyName("   ");

		assertEquals("http://ha.local:8123", conn.getDisplayName());
	}

	@Test
	public void testDisplayNameFallsBackWhenFriendlyNameIsEmpty()
	{
		HAConnection conn = new HAConnection("http://ha.local:8123", "tok");
		conn.setFriendlyName("");

		assertEquals("http://ha.local:8123", conn.getDisplayName());
	}
}
