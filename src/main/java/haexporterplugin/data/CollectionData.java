package haexporterplugin.data;

import lombok.Getter;

@Getter
public class CollectionData {
    private final String itemName;
    private final int itemId;
    private final long value;
    private final Integer killCount;

    public CollectionData(String itemName, int itemId, long value, Integer killCount) {
        this.itemName = itemName;
        this.itemId = itemId;
        this.value = value;
        this.killCount = killCount;
    }
}
