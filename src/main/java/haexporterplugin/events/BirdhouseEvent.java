package haexporterplugin.events;

public class BirdhouseEvent {
    private final String houseName;
    private final String seedName;
    private final long timeSeeded;
    private final long timeEstimatedDone;

    public BirdhouseEvent(String houseName, String seedName, long timeSeeded, long timeEstimatedDone) {
        this.houseName = houseName;
        this.seedName = seedName;
        this.timeSeeded = timeSeeded;
        this.timeEstimatedDone = timeEstimatedDone;
    }

    public String getHouseName() {
        return houseName;
    }

    public String getSeedName() {
        return seedName;
    }

    public long getTimeSeeded() {
        return timeSeeded;
    }

    public long getTimeEstimatedDone() {
        return timeEstimatedDone;
    }
}
