package haexporterplugin.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

/**
 * The player's "status" field (StatusEffectsNotifier): a few meters that always have one value, and a list of
 * the effects that are active right now, each with its own end time. Null fields are left out of the JSON, so
 * wilderness is absent outside the wilderness.
 */
@Getter
public class StatusEffectsData {
    // Whole percent, 0..100
    private final int runEnergy;
    private final boolean running;
    // Whole percent, 0..100
    private final int specialAttack;
    private final Poison poison;
    private final Wilderness wilderness;
    private final List<Effect> effects;

    public StatusEffectsData(int runEnergy, boolean running, int specialAttack, Poison poison, Wilderness wilderness, List<Effect> effects) {
        this.runEnergy = runEnergy;
        this.running = running;
        this.specialAttack = specialAttack;
        this.poison = poison;
        this.wilderness = wilderness;
        this.effects = effects;
    }

    @Getter
    @EqualsAndHashCode
    public static class Poison {
        public static final Poison NONE = new Poison("none", null);

        // "none", "poison" or "venom"
        private final String type;
        // The next hit; absent when not poisoned
        private final Integer damage;

        public Poison(String type, Integer damage) {
            this.type = type;
            this.damage = damage;
        }
    }

    @Getter
    @EqualsAndHashCode
    public static class Wilderness {
        private final int level;

        public Wilderness(int level) {
            this.level = level;
        }
    }

    @Getter
    @EqualsAndHashCode
    public static class Effect {
        // Fixed, a-z 0-9 _: RuneLite's GameTimer name in lower case
        private final String id;
        private final String name;
        // Epoch millis
        private final Long endsAt;
        // How coarse endsAt is, in millis
        private final int precision;

        public Effect(String id, String name, Long endsAt, int precision) {
            this.id = id;
            this.name = name;
            this.endsAt = endsAt;
            this.precision = precision;
        }
    }
}
