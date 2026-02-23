package haexporterplugin.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Equipment {
    private List<ItemData> items;

    public Equipment() {
    }

    public Equipment(List<ItemData> items) {
        this.items = items;
    }

}
