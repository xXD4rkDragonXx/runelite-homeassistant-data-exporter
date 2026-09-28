package haexporterplugin.utils;

import javax.annotation.Nullable;
import javax.inject.Singleton;

/**
 * Pairs kill count chat messages with the loot drop they belong to, so a collection log item can be
 * reported with the kill count of the drop it came from.
 */
@Singleton
public class KillCountTracker {
    // A kill count message and its loot arrive within a few ticks of each other
    static final int MAX_KILL_COUNT_DROP_GAP = 5;
    // Collection log popups are shown one at a time, so an item can be reported well after its drop (~30s)
    static final int MAX_DROP_AGE = 50;

    // Kill count not yet paired with a drop
    private Integer pendingKillCount;
    private int pendingKillCountTick;

    private boolean hasDrop;
    private int dropTick;
    private Integer dropKillCount;

    public void onKillCount(int killCount, int tick) {
        if (hasDrop && dropKillCount == null && isWithin(dropTick, tick, MAX_KILL_COUNT_DROP_GAP)) {
            dropKillCount = killCount;
            pendingKillCount = null;
            return;
        }
        pendingKillCount = killCount;
        pendingKillCountTick = tick;
    }

    public void onDrop(int tick) {
        hasDrop = true;
        dropTick = tick;
        dropKillCount = null;
        if (pendingKillCount != null && isWithin(pendingKillCountTick, tick, MAX_KILL_COUNT_DROP_GAP)) {
            // Used up, so a later drop (e.g. a clue casket) can't claim it
            dropKillCount = pendingKillCount;
            pendingKillCount = null;
        }
    }

    /**
     * @return the kill count of the most recent drop, or of a kill count seen just now when no loot event
     *         was received (e.g. a chest without the loot tracker); null when there is none
     */
    @Nullable
    public Integer getKillCount(int tick) {
        if (hasDrop && isWithin(dropTick, tick, MAX_DROP_AGE)) {
            return dropKillCount;
        }
        if (pendingKillCount != null && isWithin(pendingKillCountTick, tick, MAX_KILL_COUNT_DROP_GAP)) {
            return pendingKillCount;
        }
        return null;
    }

    public void reset() {
        pendingKillCount = null;
        hasDrop = false;
        dropKillCount = null;
    }

    // A negative gap means the tick counter restarted, so the earlier state is stale
    private static boolean isWithin(int fromTick, int toTick, int maxGap) {
        int gap = toTick - fromTick;
        return gap >= 0 && gap <= maxGap;
    }
}
