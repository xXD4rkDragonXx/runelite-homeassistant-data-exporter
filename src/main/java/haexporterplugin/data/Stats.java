package haexporterplugin.data;

import java.util.Map;

public class Stats {
    Map<String, Integer> xp;
    Map<String, Integer> levels;

    public Stats(Map<String, Integer> xp, Map<String, Integer> levels) {
        this.xp = xp;
        this.levels = levels;
    }
}
