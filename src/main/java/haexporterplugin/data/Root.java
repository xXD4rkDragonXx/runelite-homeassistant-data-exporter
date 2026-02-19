package haexporterplugin.data;

import lombok.Getter;
import lombok.Setter;

public class Root {
    @Setter
    @Getter
    private Player player;
    @Getter
    @Setter
    private Object[] events;

    public Root() {
        this.events = new Object[]{};
    }

}
