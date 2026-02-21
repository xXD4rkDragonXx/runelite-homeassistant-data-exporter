package haexporterplugin.utils;

import com.google.gson.Gson;
import haexporterplugin.data.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
public class MessageBuilder {

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
        // TODO: Remove Logging before V1
//        log.debug("Adding data in {}: {}", category, data.toString());
        Player player = getPlayer();
        
        switch(category.toLowerCase()) {
            case "name" -> player.setName((String) data);
            case "accounttype" -> player.setAccountType((String) data);
            case "health" -> player.setHealth((HealthData) data);
            case "prayer" -> player.setPrayerPoints((PrayerData) data);
            case "spellbook" -> player.setSpellbook((SpellbookData) data);
            case "world" -> player.setWorld((String) data);
            case "location" -> player.setLocation((WorldPoint) data);
            case "stats" -> player.setStats(data);
            case "inventory" -> player.setInventory(data);
            case "equipment" -> player.setEquipment(data);
            default -> log.warn("Unknown category: {}", category);
        }
    }

    public void addEvent(String category, Object event)
    {
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put(category, event);
        root.addEvent(eventMap);
    }

    public void resetEvents()
    {
        root.resetEvents();
    }

    public String build()
    {
        log.debug("Converting data");
        log.debug(gson.toJson(root));
        return gson.toJson(root);
    }

    public void resetData(){
        this.root = new Root();
    }
}
