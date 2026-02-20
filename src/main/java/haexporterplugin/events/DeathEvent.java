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

    public DeathEvent(Integer losePrice, boolean pk, String s, String killerName, Integer integer, List<ItemData> keptStacks, List<ItemData> lostStacks, WorldPoint worldLocation) {
    }
}
