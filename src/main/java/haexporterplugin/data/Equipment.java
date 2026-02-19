package haexporterplugin.data;

import java.util.List;

public class Equipment {
    private List<ItemData> items;

    public Equipment() {
    }

    public Equipment(List<ItemData> items) {
        this.items = items;
    }

    public List<ItemData> getItems() {
        return items;
    }

    public void setItems(List<ItemData> items) {
        this.items = items;
    }
}
