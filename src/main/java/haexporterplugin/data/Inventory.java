package haexporterplugin.data;

import java.util.List;

public class Inventory {
    private List<ItemData> items;

    public Inventory() {
    }

    public Inventory(List<ItemData> items) {
        this.items = items;
    }

    public List<ItemData> getItems() {
        return items;
    }

    public void setItems(List<ItemData> items) {
        this.items = items;
    }
}
