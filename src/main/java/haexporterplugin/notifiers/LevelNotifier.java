package haexporterplugin.notifiers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;

import javax.inject.Singleton;
import java.util.*;

import static net.runelite.api.Experience.MAX_REAL_LEVEL;

@Slf4j
@Singleton
public class LevelNotifier extends BaseNotifier {
    private static final Set<WorldType> SPECIAL_WORLDS = EnumSet.of(WorldType.PVP_ARENA, WorldType.QUEST_SPEEDRUNNING, WorldType.BETA_WORLD, WorldType.NOSAVE_MODE, WorldType.TOURNAMENT_WORLD, WorldType.DEADMAN, WorldType.SEASONAL);
    public static final int LEVEL_FOR_MAX_XP = Experience.MAX_VIRT_LEVEL + 1; // 127
    private static final int SKILL_COUNT = Skill.values().length;
    private static final String COMBAT_NAME = "Combat";
    private final Map<String, Integer> currentLevels = new HashMap<>();
    private final Map<Skill, Integer> currentXp = new EnumMap<>(Skill.class);
    private int tickCount = 0;
    static final int INIT_GAME_TICKS = 16; // ~10s
    private Set<WorldType> specialWorldType = null;

    private void initLevels() {
        for (Skill skill : Skill.values()) {
            int xp = client.getSkillExperience(skill);
            int level = client.getRealSkillLevel(skill);
            if (level >= MAX_REAL_LEVEL) {
                level = getLevel(xp);
            }
            currentLevels.put(skill.getName(), level);
            currentXp.put(skill, xp);
            currentLevels.put(COMBAT_NAME, calculateCombatLevel());
            this.specialWorldType = getSpecialWorldTypes();
            log.debug("Initialized current skill levels: {}", currentLevels);
        }
        this.tickCount = 0;
    }

    private int getLevel(int xp) {
        // treat 200M XP as level 127
        if (xp >= Experience.MAX_SKILL_XP)
            return LEVEL_FOR_MAX_XP;

        // log(n) operation to support virtual levels
        return Experience.getLevelForXp(xp);
    }

    private int calculateCombatLevel() {
        return Experience.getCombatLevel(
                getRealLevel(Skill.ATTACK),
                getRealLevel(Skill.STRENGTH),
                getRealLevel(Skill.DEFENCE),
                getRealLevel(Skill.HITPOINTS),
                getRealLevel(Skill.MAGIC),
                getRealLevel(Skill.RANGED),
                getRealLevel(Skill.PRAYER)
        );
    }

    private int getRealLevel(Skill skill) {
        Integer cachedLevel = currentLevels.get(skill.getName());
        return cachedLevel != null
                ? Math.min(cachedLevel, MAX_REAL_LEVEL)
                : client.getRealSkillLevel(skill);
    }

    private Set<WorldType> getSpecialWorldTypes() {
        var world = client.getWorldType().clone();
        world.retainAll(SPECIAL_WORLDS); // O(1)
        return world;
    }

    public void onTick(){
        // Don't do anything if not on send tick
        this.tickCount++;
        log.debug(String.valueOf(tickCount));

        if ((this.tickCount > INIT_GAME_TICKS || this.tickCount >= config.sendRate()) && currentLevels.size() < SKILL_COUNT) {
            initLevels();
            return;
        }

        if (this.tickCount % config.sendRate() != 0){
            return;
        }

        this.tickCount = 0;
        log.debug(currentXp.toString());
        log.debug(currentLevels.toString());
        messageBuilder.setData("exp", currentXp);
        messageBuilder.setData("levels", currentLevels);
        String json = messageBuilder.build();
        homeAssistUtils.sendMessage(json);
    }
}
