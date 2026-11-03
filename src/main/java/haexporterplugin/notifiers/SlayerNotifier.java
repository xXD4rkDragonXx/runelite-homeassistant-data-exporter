package haexporterplugin.notifiers;

import com.google.common.annotations.VisibleForTesting;
import haexporterplugin.data.SlayerTask;
import haexporterplugin.events.SlayerTaskEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

import javax.inject.Singleton;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps the player's "slayer" field and sends slayerTaskAssigned / slayerTaskComplete events. The state is read
 * from varps every tick and compared with the previous tick: kills only lower the remaining amount, so a higher
 * one is a new assignment, and a task streak only goes up when a task is completed (not when it is cancelled).
 * Krystilia's (wilderness) and Mortimer's tasks count towards their own streak, so a completion is whichever
 * streak went up.
 * The task name and location are looked up the same way RuneLite's own Slayer plugin does.
 */
@Slf4j
@Singleton
public class SlayerNotifier extends BaseNotifier {

    // SLAYER_TARGET for boss tasks; the boss is then in SLAYER_TARGET_BOSSID
    private static final int BOSS_TASK = 98;
    // SLAYER_MODIFIER_ID of a modifier that changes the size of the task
    private static final int AMOUNT_MODIFIER = 2;
    // Varps arrive in the first ticks after logging in; changes until then are not events
    private static final int SETTLE_TICKS = 5;

    private SlayerTask previous;
    private int ticksSinceReset;
    private int cachedTarget = -1;
    private int cachedBoss = -1;
    private int cachedArea = -1;
    private String cachedName;
    private String cachedLocation;

    public void onTick() {
        update(read());
    }

    public void reset() {
        previous = null;
        ticksSinceReset = 0;
    }

    @VisibleForTesting
    void update(SlayerTask current) {
        messageBuilder.setData("slayer", current);
        boolean settled = ++ticksSinceReset > SETTLE_TICKS;
        SlayerTask before = previous;
        previous = current;
        if (!settled || before == null || !config.includeSlayerEvents()) return;

        String streakType = raisedStreak(before, current);
        if (streakType != null && before.hasTask()) {
            log.debug("Slayer task complete: {} ({} streak)", before.getTask(), streakType);
            messageBuilder.addEvent("slayerTaskComplete", new SlayerTaskEvent(before.getTask(), before.getLocation(),
                    before.getAmount(), current.getPoints() - before.getPoints(), current.getPoints(),
                    streakType, current.getStreaks().get(streakType)));
            tickUtils.sendNow();
        }
        if (current.hasTask() && current.getRemaining() > (before.hasTask() ? before.getRemaining() : 0)) {
            log.debug("Slayer task assigned: {}x {}", current.getAmount(), current.getTask());
            messageBuilder.addEvent("slayerTaskAssigned", new SlayerTaskEvent(current.getTask(), current.getLocation(),
                    current.getAmount(), null, current.getPoints(), null, null));
            tickUtils.sendNow();
        }
    }

    private static String raisedStreak(SlayerTask before, SlayerTask current) {
        for (Map.Entry<String, Integer> streak : current.getStreaks().entrySet()) {
            Integer old = before.getStreaks().get(streak.getKey());
            if (old != null && streak.getValue() > old) return streak.getKey();
        }
        return null;
    }

    private SlayerTask read() {
        int points = client.getVarbitValue(VarbitID.SLAYER_POINTS);
        Map<String, Integer> streaks = new LinkedHashMap<>();
        streaks.put("normal", client.getVarbitValue(VarbitID.SLAYER_TASKS_COMPLETED));
        streaks.put("wilderness", client.getVarbitValue(VarbitID.SLAYER_WILDERNESS_TASKS_COMPLETED));
        streaks.put("mortimer", client.getVarpValue(VarPlayerID.SLAYER_MORTIMER_TASKS_COMPLETED));
        int remaining = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
        if (remaining <= 0) return new SlayerTask(null, null, null, null, points, streaks);

        int amount = client.getVarpValue(VarPlayerID.SLAYER_COUNT_ORIGINAL);
        if (client.getVarbitValue(VarbitID.SLAYER_MODIFIER_ID) == AMOUNT_MODIFIER) {
            int change = client.getVarbitValue(VarbitID.SLAYER_MODIFIER_VALUE);
            amount += client.getVarbitValue(VarbitID.SLAYER_MODIFIER_NEGATIVE) == 1 ? -change : change;
        }

        // The DB lookups only run when the task changes
        int target = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
        int boss = target == BOSS_TASK ? client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID) : 0;
        int area = client.getVarpValue(VarPlayerID.SLAYER_AREA);
        if (target != cachedTarget || boss != cachedBoss || area != cachedArea) {
            cachedTarget = target;
            cachedBoss = boss;
            cachedArea = area;
            cachedName = taskName(target, boss);
            cachedLocation = taskLocation(area);
        }
        return new SlayerTask(cachedName, cachedLocation, remaining, Math.max(amount, remaining), points, streaks);
    }

    private String taskName(int target, int boss) {
        Integer taskRow;
        if (target == BOSS_TASK) {
            Integer bossRow = firstRow(DBTableID.SlayerTaskSublist.ID, DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID, boss);
            taskRow = bossRow == null ? null : (Integer) field(bossRow, DBTableID.SlayerTaskSublist.COL_TASK);
        } else {
            taskRow = firstRow(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, target);
        }
        return taskRow == null ? null : (String) field(taskRow, DBTableID.SlayerTask.COL_NAME_UPPERCASE);
    }

    private String taskLocation(int area) {
        if (area <= 0) return null;
        Integer row = firstRow(DBTableID.SlayerArea.ID, DBTableID.SlayerArea.COL_AREA_ID, area);
        return row == null ? null : (String) field(row, DBTableID.SlayerArea.COL_AREA_NAME_IN_HELPER);
    }

    private Integer firstRow(int table, int column, int value) {
        List<Integer> rows = client.getDBRowsByValue(table, column, 0, value);
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    private Object field(int row, int column) {
        Object[] values = client.getDBTableField(row, column, 0);
        return values == null || values.length == 0 ? null : values[0];
    }
}
