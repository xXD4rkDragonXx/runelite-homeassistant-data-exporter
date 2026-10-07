package haexporterplugin.data;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.*;

public class HAConnectionTest
{
	@Test
	public void testTogglesAreOnUntilTurnedOff()
	{
		HAConnection conn = new HAConnection("http://ha.local:8123", "tok");
		assertTrue(conn.isIncludeInventory());
		assertTrue(conn.isIncludeEquipment());
		assertTrue(conn.isIncludeLocation());

		conn.setIncludeInventory(false);
		conn.setIncludeEquipment(false);
		conn.setIncludeLocation(false);
		assertFalse(conn.isIncludeInventory());
		assertFalse(conn.isIncludeEquipment());
		assertFalse(conn.isIncludeLocation());
	}

	// Connections stored by older versions have no toggle fields, which must then read as on
	@Test
	public void testStoredConnectionWithoutTogglesHasEverythingOn()
	{
		HAConnection conn = new Gson().fromJson("{\"baseUrl\":\"http://ha.local:8123\",\"token\":\"tok\"}", HAConnection.class);

		assertEquals("http://ha.local:8123", conn.getBaseUrl());
		assertEquals("tok", conn.getToken());
		assertTrue(conn.isEnabled());
		assertTrue(conn.isIncludeInventory());
		assertTrue(conn.isIncludeEquipment());
		assertTrue(conn.isIncludeLocation());
		assertTrue(conn.isIncludeLootEvents());
		assertTrue(conn.isIncludeDeathEvents());
		assertTrue(conn.isIncludeLevelUpEvents());
		assertTrue(conn.isIncludeAchievementDiaryEvents());
		assertTrue(conn.isIncludeCombatTaskEvents());
		assertTrue(conn.isIncludeSuperiorEvents());
		assertTrue(conn.isIncludeCollectionLogEvents());
	}

	@Test
	public void testDisplayNameFallsBackToTheUrl()
	{
		HAConnection conn = new HAConnection("http://ha.local:8123", "tok");
		assertEquals("http://ha.local:8123", conn.getDisplayName());

		conn.setFriendlyName("   ");
		assertEquals("http://ha.local:8123", conn.getDisplayName());

		conn.setFriendlyName("Living Room HA");
		assertEquals("Living Room HA", conn.getDisplayName());
	}
}
