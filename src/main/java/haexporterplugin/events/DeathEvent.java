package haexporterplugin.events;

import haexporterplugin.data.ItemData;
import haexporterplugin.enums.Danger;
import net.runelite.api.coords.WorldPoint;


import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class DeathEvent implements HAExporterEvent{
    private final Integer valueLost;
    private final Danger danger;
    @Nullable
    private final String killerName;
    @Nullable
    Integer killerNpcId;

    Collection<ItemData> keptItems;

    Collection<ItemData> lostItems;

    WorldPoint location;

    public DeathEvent(Integer valueLost, Danger danger, @Nullable String killerName, @Nullable Integer killerNpcId, List<ItemData> keptItems, List<ItemData> lostItems, WorldPoint location) {
        this.valueLost = valueLost;
        this.danger = danger;
        this.killerName = killerName;
        this.killerNpcId = killerNpcId;
        this.keptItems = keptItems;
        this.lostItems = lostItems;
        this.location = location;
    }
}
