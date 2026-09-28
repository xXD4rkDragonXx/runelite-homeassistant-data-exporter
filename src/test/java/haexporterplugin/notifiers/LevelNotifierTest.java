package haexporterplugin.notifiers;

import org.junit.Test;

import static org.junit.Assert.*;

public class LevelNotifierTest
{
	@Test
	public void testDefaultsSendEverything()
	{
		assertTrue(LevelNotifier.shouldSend("Attack", 2, 1, 1, true, true));
		assertTrue(LevelNotifier.shouldSend("Attack", 105, 1, 1, true, true));
		assertTrue(LevelNotifier.shouldSend("Combat", 50, 1, 1, true, true));
	}

	@Test
	public void testMinLevel()
	{
		assertFalse(LevelNotifier.shouldSend("Attack", 69, 70, 1, true, true));
		assertTrue(LevelNotifier.shouldSend("Attack", 70, 70, 1, true, true));
	}

	@Test
	public void testInterval()
	{
		assertFalse(LevelNotifier.shouldSend("Mining", 42, 1, 5, true, true));
		assertTrue(LevelNotifier.shouldSend("Mining", 45, 1, 5, true, true));
	}

	@Test
	public void testLevel99AlwaysSent()
	{
		assertTrue(LevelNotifier.shouldSend("Mining", 99, 1, 10, true, true));
	}

	@Test
	public void testVirtualLevels()
	{
		assertFalse(LevelNotifier.shouldSend("Mining", 100, 1, 1, false, true));
		assertTrue(LevelNotifier.shouldSend("Mining", 99, 1, 1, false, true));
		// combat level is never "virtual"
		assertTrue(LevelNotifier.shouldSend("Combat", 100, 1, 1, false, true));
	}

	@Test
	public void testCombatToggle()
	{
		assertFalse(LevelNotifier.shouldSend("Combat", 100, 1, 1, true, false));
		assertTrue(LevelNotifier.shouldSend("Attack", 60, 1, 1, true, false));
	}
}
