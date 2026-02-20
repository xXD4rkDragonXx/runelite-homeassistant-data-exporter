package haexporterplugin.events;

import haexporterplugin.data.ItemData;
import net.runelite.api.coords.WorldPoint;


import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class DeathEvent implements HAExporterEvent{
    private long valueLost;
    boolean isPvp;
    @Nullable
    private String killerName;
    @Nullable
    Integer killerNpcId;

    Collection<ItemData> keptItems;

    Collection<ItemData> lostItems;

    WorldPoint location;

    public DeathEvent(Integer valueLost, boolean isPvp, String pkerName, String killerName, Integer killerNpcId, List<ItemData> keptItems, List<ItemData> lostItems, WorldPoint location) {
        this.valueLost = valueLost;
        this.isPvp = isPvp;
        this.killerName = killerName;
        this.killerNpcId = killerNpcId;
        this.keptItems = keptItems;
        this.lostItems = lostItems;
        this.location = location;
    }
}
