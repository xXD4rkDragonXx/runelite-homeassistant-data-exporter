package haexporterplugin.data;

import haexporterplugin.utils.Evaluable;
import lombok.Getter;
import net.runelite.http.api.loottracker.LootRecordType;

import java.util.Collection;
import java.util.Set;

@Getter
public class LootData {
    private final Collection<ItemData> items;
    private final ItemData highestValueItem;
    private final long totalValue;
    private final Evaluable source;
    private final LootRecordType type;
    private final Integer npcId;
    private final Set<?> criteria;

    public LootData(Collection<ItemData> items, ItemData highestValueItem, long totalValue, Evaluable source, LootRecordType type, Integer npcId, Set<?> criteria) {
        this.items = items;
        this.highestValueItem = highestValueItem;
        this.totalValue = totalValue;
        this.source = source;
        this.type = type;
        this.npcId = npcId;
        this.criteria = criteria;
    }

    // Legacy constructor for backwards compatibility
    public LootData(ItemData itemData, long totalValue, Evaluable source) {
        this.items = null;
        this.highestValueItem = itemData;
        this.totalValue = totalValue;
        this.source = source;
        this.type = null;
        this.npcId = null;
        this.criteria = null;
    }
}

