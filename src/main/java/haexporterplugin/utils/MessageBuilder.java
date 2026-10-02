package haexporterplugin.utils;

import com.google.gson.Gson;
import haexporterplugin.data.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Singleton
public class MessageBuilder {
    // Upper bound on the location trail of one message; the oldest points are dropped beyond it
    public static final int MAX_LOCATION_TRAIL_POINTS = 300;

    @Inject
    private Gson gson;

    @Getter
    private Root root;

    public MessageBuilder() {
        this.root = new Root();
    }

    public Player getPlayer() {
        if (root.getPlayer() == null) {
            root.setPlayer(new Player());
        }
        return root.getPlayer();
    }

    public void setData(String category, Object data)
    {
        Player player = getPlayer();

        switch (category.toLowerCase()) {
            case "name":
                player.setName((String) data);
                break;
            case "accounthash":
                player.setAccountHash((String) data);
                break;
            case "accounttype":
                player.setAccountType((String) data);
                break;
            case "health":
                player.setHealth((HealthData) data);
                break;
            case "prayer":
                player.setPrayerPoints((PrayerData) data);
                break;
            case "spellbook":
                player.setSpellbook((SpellbookData) data);
                break;
            case "world":
                player.setWorld((String) data);
                break;
            case "worldtypes":
                player.setWorldTypes((String[]) data);
                break;
            case "location":
                player.setLocation((PlayerLocation) data);
                break;
            case "stats":
                player.setStats(data);
                break;
            case "inventory":
                player.setInventory(data);
                break;
            case "equipment":
                player.setEquipment(data);
                break;
            default:
                log.warn("Unknown category: {}", category);
                break;
        }
    }

    public void addEvent(String category, Object event)
    {
        Map<String, Object> eventMap = new LinkedHashMap<>();
        eventMap.put("type", category);
        eventMap.put("data", event);
        // Identical events can legitimately repeat (e.g. two diary tasks), so receivers dedupe on eventId
        eventMap.put("eventId", UUID.randomUUID().toString());
        eventMap.put("timestamp", System.currentTimeMillis());
        root.addEvent(eventMap);
    }

    public void resetEvents()
    {
        root.resetEvents();
    }

    public void addLocationTrailPoint(TrailPoint point)
    {
        List<TrailPoint> trail = getPlayer().getLocationTrail();
        if (trail.size() >= MAX_LOCATION_TRAIL_POINTS) {
            trail.remove(0);
        }
        trail.add(point);
    }

    public void resetLocationTrail()
    {
        if (root.getPlayer() != null) {
            root.getPlayer().getLocationTrail().clear();
        }
    }

    public void setState(GameState state){
        root.setState(state);
    }

    public String build()
    {
        root.setTimestamp(System.currentTimeMillis());
        return gson.toJson(root);
    }

    public void resetData(){
        this.root = new Root();
    }

    public void setTickDelay(int tickDelay){
        root.setTickDelay(tickDelay);
    }
}
