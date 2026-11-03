package haexporterplugin.data;

import lombok.Getter;

import java.util.Map;

/**
 * The player's slayer state, sent with every message (SlayerNotifier). Without a task, only the points
 * and the streaks are set; null fields are left out of the JSON.
 */
@Getter
public class SlayerTask {
    private final String task;
    // Only set for Konar's tasks, which come with a location
    private final String location;
    private final Integer remaining;
    // The task's size when it was assigned
    private final Integer amount;
    private final int points;
    // Krystilia's and Mortimer's tasks have their own streak: "normal", "wilderness" and "mortimer"
    private final Map<String, Integer> streaks;

    public SlayerTask(String task, String location, Integer remaining, Integer amount, int points, Map<String, Integer> streaks) {
        this.task = task;
        this.location = location;
        this.remaining = remaining;
        this.amount = amount;
        this.points = points;
        this.streaks = streaks;
    }

    public boolean hasTask() {
        return remaining != null && remaining > 0;
    }
}
