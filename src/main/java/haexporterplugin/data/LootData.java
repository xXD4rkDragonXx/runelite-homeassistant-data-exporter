package haexporterplugin.data;

import haexporterplugin.utils.Evaluable;
import net.runelite.http.api.loottracker.LootRecordType;

import java.util.Collection;
import java.util.Set;

public record LootData(Collection<ItemData> items, ItemData highestValueItem, long totalValue, Evaluable source,
                       LootRecordType type, Integer npcId, Set<?> criteria) {

    // Legacy constructor for backwards compatibility
    public LootData(ItemData itemData, long totalValue, Evaluable source) {
        this(null, itemData, totalValue, source, null, null, null);
    }
}

