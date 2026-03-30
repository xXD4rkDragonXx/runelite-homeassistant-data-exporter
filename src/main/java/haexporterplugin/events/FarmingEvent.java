package haexporterplugin.events;

public class FarmingEvent {
    private final String patchName;
    private final String cropName;
    private final long timePlanted;
    private final long timeEstimatedDone;

    public FarmingEvent(String patchName, String cropName, long timePlanted, long timeEstimatedDone) {
        this.patchName = patchName;
        this.cropName = cropName;
        this.timePlanted = timePlanted;
        this.timeEstimatedDone = timeEstimatedDone;
    }

    public String getPatchName() {
        return patchName;
    }

    public String getCropName() {
        return cropName;
    }

    public long getTimePlanted() {
        return timePlanted;
    }

    public long getTimeEstimatedDone() {
        return timeEstimatedDone;
    }
}
