package haexporterplugin.events;

// slayerTaskAssigned and slayerTaskComplete. Only set (and sent) on completion: pointsEarned, points, streakType and
// streak. On assignment they would repeat player.slayer
public class SlayerTaskEvent implements HAExporterEvent {
    private final String task;
    private final String location;
    private final int amount;
    private final Integer pointsEarned;
    private final Integer points;
    // The streak this task counted towards ("normal", "wilderness" or "mortimer") and its new value
    private final String streakType;
    private final Integer streak;

    public SlayerTaskEvent(String task, String location, int amount, Integer pointsEarned, Integer points, String streakType, Integer streak) {
        this.task = task;
        this.location = location;
        this.amount = amount;
        this.pointsEarned = pointsEarned;
        this.points = points;
        this.streakType = streakType;
        this.streak = streak;
    }
}
