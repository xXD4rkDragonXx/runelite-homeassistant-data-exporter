package haexporterplugin.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RareItemData extends ItemData {
    private final double rarity;

    public RareItemData(String name, int id, int gePrice, int haPrice, int quantity, double rarity) {
        this.setName(name);
        this.setId(id);
        this.setGePrice(gePrice);
        this.setHaPrice(haPrice);
        this.setQuantity(quantity);
        this.rarity = rarity;
    }

    public static RareItemData of(ItemData itemData, double rarity){
        return new RareItemData(itemData.getName(), itemData.getId(), itemData.getGePrice(), itemData.getHaPrice(), itemData.getQuantity(), rarity);
    }
}
