package haexporterplugin.events;

import com.google.gson.Gson;
import haexporterplugin.data.ItemData;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class DeathEventTest {

    private final Gson gson = new Gson();

    @Test
    public void testDeathEventImplementsInterface() {
        List<ItemData> kept = new ArrayList<>();
        List<ItemData> lost = new ArrayList<>();
        WorldPoint location = new WorldPoint(3200, 3200, 0);

        DeathEvent event = new DeathEvent(0, false, null, null, null, kept, lost, location);
        assertTrue(event instanceof HAExporterEvent);
    }

    @Test
    public void testPvpDeathEvent() {
        List<ItemData> kept = new ArrayList<>();
        kept.add(new ItemData("Abyssal whip", 4151, 2500000, 120001, 1));

        List<ItemData> lost = new ArrayList<>();
        lost.add(new ItemData("Dragon boots", 11840, 200000, 90000, 1));
        lost.add(new ItemData("Rune platebody", 1127, 38000, 25000, 1));

        WorldPoint location = new WorldPoint(3090, 3500, 0);

        DeathEvent event = new DeathEvent(
                238000, true, "PKer123", "PKer123", null,
                kept, lost, location
        );

        String json = gson.toJson(event);
        assertTrue(json.contains("\"valueLost\":238000"));
        assertTrue(json.contains("\"isPvp\":true"));
        assertTrue(json.contains("\"killerName\":\"PKer123\""));
    }

    @Test
    public void testNpcDeathEvent() {
        List<ItemData> kept = new ArrayList<>();
        List<ItemData> lost = new ArrayList<>();
        lost.add(new ItemData("Shark", 385, 800, 0, 10));

        WorldPoint location = new WorldPoint(2840, 3540, 0);

        DeathEvent event = new DeathEvent(
                8000, false, null, "General Graardor", 2215,
                kept, lost, location
        );

        String json = gson.toJson(event);
        assertTrue(json.contains("\"valueLost\":8000"));
        assertTrue(json.contains("\"isPvp\":false"));
        assertTrue(json.contains("\"killerName\":\"General Graardor\""));
        assertTrue(json.contains("\"killerNpcId\":2215"));
    }

    @Test
    public void testSafeDeathEvent() {
        List<ItemData> kept = new ArrayList<>();
        kept.add(new ItemData("Rune scimitar", 1333, 15000, 10000, 1));

        List<ItemData> lost = new ArrayList<>();
        WorldPoint location = new WorldPoint(2520, 5175, 0);

        DeathEvent event = new DeathEvent(
                0, false, null, null, null,
                kept, lost, location
        );

        String json = gson.toJson(event);
        assertTrue(json.contains("\"valueLost\":0"));
        assertTrue(json.contains("\"isPvp\":false"));
    }

    @Test
    public void testDeathEventLocationSerialization() {
        WorldPoint location = new WorldPoint(3222, 3218, 0);
        DeathEvent event = new DeathEvent(
                0, false, null, null, null,
                new ArrayList<>(), new ArrayList<>(), location
        );

        String json = gson.toJson(event);
        assertTrue(json.contains("\"x\":3222"));
        assertTrue(json.contains("\"y\":3218"));
        assertTrue(json.contains("\"plane\":0"));
    }

    @Test
    public void testDeathEventWithKeptAndLostItems() {
        List<ItemData> kept = new ArrayList<>();
        kept.add(new ItemData("Abyssal whip", 4151, 2500000, 120001, 1));
        kept.add(new ItemData("Dragon defender", 12954, 500000, 100000, 1));
        kept.add(new ItemData("Berserker ring (i)", 11773, 3000000, 150000, 1));

        List<ItemData> lost = new ArrayList<>();
        lost.add(new ItemData("Shark", 385, 800, 0, 15));
        lost.add(new ItemData("Super combat potion(4)", 12695, 12000, 200, 2));

        WorldPoint location = new WorldPoint(3100, 3550, 0);

        DeathEvent event = new DeathEvent(
                36000, false, null, "Venenatis", 6610,
                kept, lost, location
        );

        String json = gson.toJson(event);
        assertTrue(json.contains("\"keptItems\""));
        assertTrue(json.contains("\"lostItems\""));
        assertTrue(json.contains("\"Abyssal whip\""));
        assertTrue(json.contains("\"Shark\""));
    }
}
