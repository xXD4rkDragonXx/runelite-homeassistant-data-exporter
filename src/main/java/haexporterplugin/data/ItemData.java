package haexporterplugin.data;

import lombok.Getter;
import lombok.Setter;

@Setter
public class ItemData {
    @Getter
    private String name;
    @Getter
    private int id;
    @Getter
    private long gePrice;
    @Getter
    private int haPrice;
    @Getter
    private int quantity;
    @Getter
    private String equipmentSlot;
    // Boxed so Gson omits it for non-inventory items
    @Getter
    private Integer inventorySlot;

    public ItemData() {
        this.quantity = 1;
    }

    public ItemData(String name, int id, long gePrice, int haPrice, int quantity) {
        this.name = name;
        this.id = id;
        this.gePrice = gePrice;
        this.haPrice = haPrice;
        this.quantity = quantity;
    }

    public ItemData(ItemData itemData) {
        this.name = itemData.getName();
        this.id = itemData.getId();
        this.gePrice = itemData.getGePrice();
        this.haPrice = itemData.getHaPrice();
        this.quantity = itemData.getQuantity();
        this.equipmentSlot = itemData.getEquipmentSlot();
        this.inventorySlot = itemData.getInventorySlot();
    }
}
