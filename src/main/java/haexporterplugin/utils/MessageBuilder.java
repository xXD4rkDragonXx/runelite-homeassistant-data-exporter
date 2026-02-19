package haexporterplugin.utils;

import com.google.gson.Gson;
import haexporterplugin.data.Root;
import haexporterplugin.data.Player;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import javax.inject.Singleton;

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
        log.debug("Adding data in {}: {}", category, data.toString());
        Player player = getPlayer();
        
        switch(category.toLowerCase()) {
            case "name" -> player.setName((String) data);
            case "accounttype" -> player.setAccountType((String) data);
            case "world" -> player.setWorld((String) data);
            case "location" -> player.setLocation((WorldPoint) data);
            case "stats" -> player.setStats(data);
            case "inventory" -> player.setInventory(data);
            case "equipment" -> player.setEquipment(data);
            default -> log.warn("Unknown category: {}", category);
        }
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
