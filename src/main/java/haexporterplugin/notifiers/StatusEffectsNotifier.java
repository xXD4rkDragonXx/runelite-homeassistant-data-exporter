package haexporterplugin.notifiers;

import com.google.common.annotations.VisibleForTesting;
import haexporterplugin.data.StatusEffectsData;
import haexporterplugin.data.StatusEffectsData.Effect;
import haexporterplugin.data.StatusEffectsData.Poison;
import haexporterplugin.data.StatusEffectsData.Wilderness;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps the player's "status" field: run energy, special attack, poison, the wilderness level, and the list of
 * effects that are active (potions, teleblock, ...). Everything is read from varbits and varps every tick, so
 * it works without RuneLite's Timers & Buffs plugin. The units come from that plugin.
 * <p>
 * Each effect has an end time. Most varbits count down in steps of several ticks; for some (antifire, overload,
 * poison immunity) the step isn't in a varbit, so it is followed from when the value changes, like Timers & Buffs
 * does. An end time is only replaced when it moves by more than its precision, so it doesn't jump back and forth
 * between messages.
 * <p>
 * To add an effect, add a line to the registry in {@link #registry()}.
 */
@Slf4j
@Singleton
public class StatusEffectsNotifier extends BaseNotifier {

    static final int TICK_MILLIS = 600;
    // Varbits arrive in the first ticks after logging in; changes until then aren't sent right away
    private static final int SETTLE_TICKS = 5;
    private static final int VENOM_THRESHOLD = 1_000_000;
    private static final int VENOM_MAX_DAMAGE = 20;
    // Negative POISON values are immunity: antivenom below this, antipoison from this up to 0
    private static final int VENOM_IMMUNITY_CUTOFF = -38;
    private static final int TELEBLOCK_OFFSET = 100;
    private static final Pattern WILDERNESS_LEVEL = Pattern.compile("Level:\\s*(\\d+)");

    /** Reads varbits and varps, so tests can feed values without a client. */
    @VisibleForTesting
    interface Vars {
        int varbit(int id);

        int varp(int id);
    }

    @FunctionalInterface
    private interface Remaining {
        // Ticks left, 0 or less when the effect isn't active
        int ticks(Vars vars, int tick);
    }

    private static final class Definition {
        final String id;
        final String name;
        final int stepTicks;
        final Remaining remaining;

        Definition(String id, String name, int stepTicks, Remaining remaining) {
            this.id = id;
            this.name = name;
            this.stepTicks = stepTicks;
            this.remaining = remaining;
        }
    }

    /**
     * Follows a value that goes down one step every {@code step} ticks without telling when the next step is,
     * the same way Timers & Buffs does: when the value changes and no step is pending, the next one is a full
     * step away.
     */
    @VisibleForTesting
    static final class Stepped {
        private final int step;
        private boolean seen;
        private int last;
        private int next = -1;

        Stepped(int step) {
            this.step = step;
        }

        void observe(int value, int tick) {
            if (seen && value == last) return;
            seen = true;
            last = value;
            if (value == 0) {
                next = -1;
            } else if (next - tick <= 0) {
                next = tick + step;
            }
        }

        // Ticks until the value goes down a step
        int untilStep(int tick) {
            return next > tick ? next - tick : step;
        }
    }

    private final Stepped antifire = new Stepped(30);
    private final Stepped superAntifire = new Stepped(20);
    private final Stepped poisonVarp = new Stepped(30);
    private final Stepped nzOverload = new Stepped(25);
    private final Stepped raidsOverload = new Stepped(25);
    private final Stepped deadmanOverload = new Stepped(25);
    private final List<Definition> registry = registry();

    private final Map<String, Effect> active = new HashMap<>();
    private StatusEffectsData previous;
    private int ticksSinceReset;

    private List<Definition> registry() {
        List<Definition> r = new ArrayList<>();
        r.add(new Definition("stamina", "Stamina", 10, (v, t) ->
                v.varbit(VarbitID.STAMINA_ACTIVE) == 1 ? v.varbit(VarbitID.STAMINA_DURATION) * 10 : 0));
        r.add(new Definition("antifire", "Antifire", 30, (v, t) ->
                stepped(v.varbit(VarbitID.ANTIFIRE_POTION), 30, antifire, t)));
        r.add(new Definition("superantifire", "Super antifire", 20, (v, t) ->
                stepped(v.varbit(VarbitID.SUPER_ANTIFIRE_POTION), 20, superAntifire, t)));
        r.add(new Definition("antipoison", "Antipoison", 30, (v, t) -> {
            int value = v.varp(VarPlayerID.POISON);
            return value < 0 && value >= VENOM_IMMUNITY_CUTOFF ? poisonVarp.untilStep(t) + Math.abs((value + 1) * 30) : 0;
        }));
        r.add(new Definition("antivenom", "Anti-venom", 30, (v, t) -> {
            int value = v.varp(VarPlayerID.POISON);
            return value < VENOM_IMMUNITY_CUTOFF ? poisonVarp.untilStep(t) + Math.abs((value + 1 - VENOM_IMMUNITY_CUTOFF) * 30) : 0;
        }));
        // A combined divine potion covers the single ones it contains, so those are only listed when they last longer
        r.add(new Definition("divine_super_attack", "Divine super attack", 1, (v, t) ->
                over(v.varbit(VarbitID.DIVINEATTACK_POTION_TIME), v.varbit(VarbitID.DIVINECOMBAT_POTION_TIME))));
        r.add(new Definition("divine_super_strength", "Divine super strength", 1, (v, t) ->
                over(v.varbit(VarbitID.DIVINESTRENGTH_POTION_TIME), v.varbit(VarbitID.DIVINECOMBAT_POTION_TIME))));
        r.add(new Definition("divine_super_defence", "Divine super defence", 1, (v, t) -> {
            int moonlight = v.varbit(VarbitID.MOONLIGHT_POTION_TIME);
            int cover = Math.max(v.varbit(VarbitID.DIVINECOMBAT_POTION_TIME),
                    Math.max(v.varbit(VarbitID.DIVINEBASTION_POTION_TIME), v.varbit(VarbitID.DIVINEBATTLEMAGE_POTION_TIME)));
            // A moonlight potion also boosts defence, and can run one tick ahead of it
            if (moonlight > 0) cover = Math.max(cover, moonlight + 1);
            return over(v.varbit(VarbitID.DIVINEDEFENCE_POTION_TIME), cover);
        }));
        r.add(new Definition("divine_ranging", "Divine ranging", 1, (v, t) ->
                over(v.varbit(VarbitID.DIVINERANGE_POTION_TIME), v.varbit(VarbitID.DIVINEBASTION_POTION_TIME))));
        r.add(new Definition("divine_magic", "Divine magic", 1, (v, t) ->
                over(v.varbit(VarbitID.DIVINEMAGIC_POTION_TIME), v.varbit(VarbitID.DIVINEBATTLEMAGE_POTION_TIME))));
        r.add(new Definition("divine_super_combat", "Divine super combat", 1, (v, t) -> v.varbit(VarbitID.DIVINECOMBAT_POTION_TIME)));
        r.add(new Definition("divine_bastion", "Divine bastion", 1, (v, t) -> v.varbit(VarbitID.DIVINEBASTION_POTION_TIME)));
        r.add(new Definition("divine_battlemage", "Divine battlemage", 1, (v, t) -> v.varbit(VarbitID.DIVINEBATTLEMAGE_POTION_TIME)));
        r.add(new Definition("moonlight_potion", "Moonlight potion", 1, (v, t) -> {
            int moonlight = v.varbit(VarbitID.MOONLIGHT_POTION_TIME);
            if (moonlight <= 0) return 0;
            return v.varbit(VarbitID.DIVINEDEFENCE_POTION_TIME) == moonlight + 1 ? moonlight + 1 : moonlight;
        }));
        r.add(new Definition("goading", "Goading", 6, (v, t) -> v.varbit(VarbitID.GOADING_POTION_TIMER) * 6));
        r.add(new Definition("prayer_regeneration", "Prayer regeneration", 12, (v, t) -> v.varbit(VarbitID.PRAYER_REGENERATION_POTION_TIMER) * 12));
        r.add(new Definition("surge_potion", "Surge potion", 10, (v, t) -> v.varbit(VarbitID.SURGE_POTION_TIMER) * 10));
        r.add(new Definition("menaphite_remedy", "Menaphite remedy", 25, (v, t) -> v.varbit(VarbitID.STATRENEWAL_POTION_TIMER) * 25));
        r.add(new Definition("imbuedheart", "Imbued heart", 10, (v, t) -> v.varbit(VarbitID.IMBUED_HEART_TIMER) * 10));
        r.add(new Definition("magicimbue", "Magic imbue", 10, (v, t) -> v.varbit(VarbitID.MAGIC_IMBUE_ACTIVE) * 10));
        r.add(new Definition("overload", "Overload", 25, (v, t) -> Math.max(
                stepped(v.varbit(VarbitID.NZONE_OVERLOAD_POTION_EFFECTS), 25, nzOverload, t),
                Math.max(stepped(v.varbit(VarbitID.RAIDS_OVERLOAD_TIMER), 25, raidsOverload, t),
                        stepped(v.varbit(VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS), 25, deadmanOverload, t)))));
        r.add(new Definition("teleblock", "Teleblock", 1, (v, t) -> v.varbit(VarbitID.TELEBLOCK_CYCLES) - TELEBLOCK_OFFSET));
        return r;
    }

    private static int stepped(int value, int step, Stepped tracker, int tick) {
        return value > 0 ? tracker.untilStep(tick) + (value - 1) * step : 0;
    }

    private static int over(int value, int cover) {
        return value > cover ? value : 0;
    }

    public void onTick() {
        Vars vars = new Vars() {
            @Override
            public int varbit(int id) {
                return client.getVarbitValue(id);
            }

            @Override
            public int varp(int id) {
                return client.getVarpValue(id);
            }
        };
        long now = System.currentTimeMillis();
        int poison = client.getVarpValue(VarPlayerID.POISON);
        update(new StatusEffectsData(
                client.getEnergy() / 100,
                client.getVarpValue(VarPlayerID.OPTION_RUN) == 1,
                client.getVarpValue(VarPlayerID.SA_ENERGY) / 10,
                poison(poison),
                wilderness(),
                effects(vars, client.getTickCount(), now)));
    }

    public void reset() {
        for (Stepped tracker : List.of(antifire, superAntifire, poisonVarp, nzOverload, raidsOverload, deadmanOverload)) {
            tracker.seen = false;
            tracker.next = -1;
        }
        active.clear();
        previous = null;
        ticksSinceReset = 0;
    }

    @VisibleForTesting
    void update(StatusEffectsData current) {
        messageBuilder.setData("status", current);
        boolean settled = ++ticksSinceReset > SETTLE_TICKS;
        StatusEffectsData before = previous;
        previous = current;
        if (!settled || before == null || !config.includeStatusEffects() || !config.sendStatusInstantly()) return;

        if (!before.getEffects().equals(current.getEffects())
                || !before.getPoison().getType().equals(current.getPoison().getType())
                || !Objects.equals(before.getWilderness(), current.getWilderness())) {
            tickUtils.sendNow();
        }
    }

    /** The active effects in registry order, with end times that only move by more than their precision. */
    @VisibleForTesting
    List<Effect> effects(Vars vars, int tick, long now) {
        antifire.observe(vars.varbit(VarbitID.ANTIFIRE_POTION), tick);
        superAntifire.observe(vars.varbit(VarbitID.SUPER_ANTIFIRE_POTION), tick);
        poisonVarp.observe(vars.varp(VarPlayerID.POISON), tick);
        nzOverload.observe(vars.varbit(VarbitID.NZONE_OVERLOAD_POTION_EFFECTS), tick);
        raidsOverload.observe(vars.varbit(VarbitID.RAIDS_OVERLOAD_TIMER), tick);
        deadmanOverload.observe(vars.varbit(VarbitID.DEADMAN_OVERLOAD_POTION_EFFECTS), tick);

        List<Effect> effects = new ArrayList<>();
        for (Definition definition : registry) {
            int ticks = definition.remaining.ticks(vars, tick);
            if (ticks <= 0) {
                active.remove(definition.id);
                continue;
            }
            int precision = definition.stepTicks * TICK_MILLIS;
            long endsAt = now + (long) ticks * TICK_MILLIS;
            Effect kept = active.get(definition.id);
            // An estimate moves by less than a step (plus some clock jitter); a new dose moves it by a step or more
            if (kept == null || Math.abs(kept.getEndsAt() - endsAt) > precision - TICK_MILLIS / 2) {
                kept = new Effect(definition.id, definition.name, endsAt, precision);
                active.put(definition.id, kept);
            }
            effects.add(kept);
        }
        return effects;
    }

    @VisibleForTesting
    static Poison poison(int value) {
        if (value >= VENOM_THRESHOLD) {
            return new Poison("venom", Math.min(VENOM_MAX_DAMAGE, (value - (VENOM_THRESHOLD - 3)) * 2));
        }
        if (value > 0) {
            return new Poison("poison", (int) Math.ceil(value / 5.0));
        }
        return Poison.NONE;
    }

    private Wilderness wilderness() {
        if (client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) != 1) return null;
        Widget widget = client.getWidget(InterfaceID.PvpIcons.WILDERNESSLEVEL);
        Integer level = widget == null || widget.isHidden() ? null : parseWildernessLevel(widget.getText());
        if (level == null) {
            Player player = client.getLocalPlayer();
            level = player == null ? null : levelAt(player.getWorldLocation());
        }
        return level == null ? null : new Wilderness(level);
    }

    // The widget reads "Level: 23"
    @VisibleForTesting
    static Integer parseWildernessLevel(String text) {
        if (text == null) return null;
        Matcher matcher = WILDERNESS_LEVEL.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    // Each 8 tiles north of the wilderness ditch is a level, on the surface and in the caves below it
    @VisibleForTesting
    static Integer levelAt(WorldPoint point) {
        if (point == null) return null;
        int y = point.getY();
        int base = y >= 9920 ? 9920 : 3520;
        return y < base ? null : (y - base) / 8 + 1;
    }
}
