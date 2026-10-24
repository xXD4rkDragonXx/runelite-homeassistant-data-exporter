package haexporterplugin.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ActivePrayers {
    // Names of the active prayers (net.runelite.api.Prayer constants), in prayer book order
    private List<String> prayers;

    public ActivePrayers() {
    }

    public ActivePrayers(List<String> prayers) {
        this.prayers = prayers;
    }

}
