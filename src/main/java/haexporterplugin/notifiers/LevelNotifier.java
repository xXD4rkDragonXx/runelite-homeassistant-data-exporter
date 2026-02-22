package haexporterplugin.notifiers;

import haexporterplugin.data.SkillInfo;
import haexporterplugin.data.Stats;
import haexporterplugin.events.LevelEvent;
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
    private final Map<String, Integer> previousLevels = new HashMap<>();
    private final Map<Skill, Integer> currentXp = new EnumMap<>(Skill.class);
    static final int INIT_GAME_TICKS = 1; // ~10s
    private Set<WorldType> specialWorldType = null;

    private void updateSkillsFromGameState() {
        for (Skill skill : Skill.values()) {
            int xp = client.getSkillExperience(skill);
            int level = client.getRealSkillLevel(skill);
            if (level >= MAX_REAL_LEVEL) {
                level = getLevel(xp);
            }
            currentLevels.put(skill.getName(), level);
            currentXp.put(skill, xp);
        }
        currentLevels.put(COMBAT_NAME, calculateCombatLevel());
    }

    private void initLevels() {
        updateSkillsFromGameState();
        previousLevels.putAll(currentLevels);
        this.specialWorldType = getSpecialWorldTypes();
        log.debug("Initialized current skill levels: {}", currentLevels);
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

    private List<LevelEvent> detectAndUpdateLevelChanges() {
        List<LevelEvent> changedLevels = new ArrayList<LevelEvent>();
        for (String skillName : currentLevels.keySet()) {
            int currentLevel = currentLevels.get(skillName);
            int previousLevel = previousLevels.getOrDefault(skillName, currentLevel);
            if (currentLevel != previousLevel) {
                changedLevels.add(new LevelEvent(skillName, currentLevel));
                previousLevels.put(skillName, currentLevel);
            }
        }
        return changedLevels;
    }

    private Stats buildStats() {
        Map<String, SkillInfo> skillsMap = new LinkedHashMap<>();
        for (Skill runeLiteSkill : Skill.values()) {
            String skillName = runeLiteSkill.getName();
            Integer xp = currentXp.get(runeLiteSkill);
            Integer level = currentLevels.get(skillName);
            if (xp != null && level != null) {
                skillsMap.put(skillName, new SkillInfo(xp, level));
            }
        }
        return new Stats(skillsMap);
    }

    public void onTick(){
        int tickCount = tickUtils.getTickCount();

        if ((tickCount > INIT_GAME_TICKS || tickCount >= config.sendRate()) && previousLevels.isEmpty()) {
            log.debug("INIT LEVELS");
            initLevels();
            return;
        }

        updateSkillsFromGameState();
        List<LevelEvent> changedLevels = detectAndUpdateLevelChanges();
        
        Stats stats = buildStats();
        messageBuilder.setData("stats", stats);
        
        if (!changedLevels.isEmpty()) {
            log.info("Level changes detected: {}", changedLevels);
            messageBuilder.addEvent("levelUp", changedLevels);
            tickUtils.sendNow();
        }
    }
}
