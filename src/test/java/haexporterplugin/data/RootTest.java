package haexporterplugin.data;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RootTest {

    private Root root;

    @Before
    public void setUp() {
        root = new Root();
    }

    @Test
    public void testConstructorInitializesEmptyEvents() {
        assertNotNull(root.getEvents());
        assertTrue(root.getEvents().isEmpty());
    }

    @Test
    public void testConstructorPlayerIsNull() {
        assertNull(root.getPlayer());
    }

    @Test
    public void testSetAndGetPlayer() {
        Player player = new Player();
        player.setName("TestPlayer");
        root.setPlayer(player);

        assertNotNull(root.getPlayer());
        assertEquals("TestPlayer", root.getPlayer().getName());
    }

    @Test
    public void testAddEvent() {
        root.addEvent("event1");
        assertEquals(1, root.getEvents().size());
        assertEquals("event1", root.getEvents().get(0));
    }

    @Test
    public void testAddMultipleEvents() {
        root.addEvent("event1");
        root.addEvent("event2");
        root.addEvent("event3");

        assertEquals(3, root.getEvents().size());
        assertEquals("event1", root.getEvents().get(0));
        assertEquals("event2", root.getEvents().get(1));
        assertEquals("event3", root.getEvents().get(2));
    }

    @Test
    public void testResetEvents() {
        root.addEvent("event1");
        root.addEvent("event2");
        assertEquals(2, root.getEvents().size());

        root.resetEvents();
        assertTrue(root.getEvents().isEmpty());
    }

    @Test
    public void testResetEventsOnEmptyList() {
        root.resetEvents();
        assertTrue(root.getEvents().isEmpty());
    }

    @Test
    public void testSetEvents() {
        java.util.ArrayList<Object> events = new java.util.ArrayList<>();
        events.add("custom1");
        events.add("custom2");

        root.setEvents(events);
        assertEquals(2, root.getEvents().size());
        assertEquals("custom1", root.getEvents().get(0));
    }

    @Test
    public void testPlayerPersistsAcrossGets() {
        Player player = new Player();
        player.setName("Persistent");
        root.setPlayer(player);

        assertSame(root.getPlayer(), root.getPlayer());
    }

    @Test
    public void testAddEventAfterReset() {
        root.addEvent("before");
        root.resetEvents();
        root.addEvent("after");

        assertEquals(1, root.getEvents().size());
        assertEquals("after", root.getEvents().get(0));
    }
}
