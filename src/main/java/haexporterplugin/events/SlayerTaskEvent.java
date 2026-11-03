package haexporterplugin.events;

// slayerTaskAssigned and slayerTaskComplete. pointsEarned, streakType and streak are only set (and sent) on completion
public class SlayerTaskEvent implements HAExporterEvent {
    private final String task;
    private final String location;
    private final int amount;
    private final Integer pointsEarned;
    private final int points;
    // The streak this task counted towards ("normal", "wilderness" or "mortimer") and its new value
    private final String streakType;
    private final Integer streak;

    public SlayerTaskEvent(String task, String location, int amount, Integer pointsEarned, int points, String streakType, Integer streak) {
        this.task = task;
        this.location = location;
        this.amount = amount;
        this.pointsEarned = pointsEarned;
        this.points = points;
        this.streakType = streakType;
        this.streak = streak;
    }
}
