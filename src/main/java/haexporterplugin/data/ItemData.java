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
    private int gePrice;
    @Getter
    private int haPrice;
    @Getter
    private int quantity;
    @Getter
    private String equipmentSlot;

    public ItemData() {
        this.quantity = 1;
    }

    public ItemData(String name, int id, int gePrice, int haPrice, int quantity) {
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
    }
}
