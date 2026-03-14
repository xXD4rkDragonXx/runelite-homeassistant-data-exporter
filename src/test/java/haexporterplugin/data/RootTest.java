package haexporterplugin.data;

import net.runelite.api.GameState;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RootTest
{
	private Root root;

	@Before
	public void setUp()
	{
		root = new Root();
	}

	@Test
	public void testEventsListInitializedEmpty()
	{
		assertNotNull(root.getEvents());
		assertTrue(root.getEvents().isEmpty());
	}

	@Test
	public void testAddEvent()
	{
		root.addEvent("test-event");
		assertEquals(1, root.getEvents().size());
		assertEquals("test-event", root.getEvents().get(0));
	}

	@Test
	public void testAddMultipleEvents()
	{
		root.addEvent("event1");
		root.addEvent("event2");
		root.addEvent("event3");
		assertEquals(3, root.getEvents().size());
	}

	@Test
	public void testResetEvents()
	{
		root.addEvent("event1");
		root.addEvent("event2");
		root.resetEvents();
		assertTrue(root.getEvents().isEmpty());
	}

	@Test
	public void testSetAndGetPlayer()
	{
		Player player = new Player();
		player.setName("TestPlayer");
		root.setPlayer(player);

		assertNotNull(root.getPlayer());
		assertEquals("TestPlayer", root.getPlayer().getName());
	}

	@Test
	public void testPlayerDefaultsToNull()
	{
		assertNull(root.getPlayer());
	}

	@Test
	public void testSetAndGetState()
	{
		root.setState(GameState.LOGGED_IN);
		assertEquals(GameState.LOGGED_IN, root.getState());
	}

	@Test
	public void testSetTickDelay()
	{
		root.setTickDelay(100);
		// tickDelay has a setter but no getter exposed, so we just ensure no exception is thrown
	}
}
