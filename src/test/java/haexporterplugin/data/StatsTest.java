package haexporterplugin.data;

import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class StatsTest {

    @Test
    public void testDefaultConstructor() {
        Stats stats = new Stats();
        assertNull(stats.getSkills());
    }

    @Test
    public void testParameterizedConstructor() {
        Map<String, SkillInfo> skills = new HashMap<>();
        skills.put("Attack", new SkillInfo(100000, 75));
        skills.put("Defence", new SkillInfo(50000, 50));

        Stats stats = new Stats(skills);
        assertNotNull(stats.getSkills());
        assertEquals(2, stats.getSkills().size());
    }

    @Test
    public void testSetAndGetSkills() {
        Stats stats = new Stats();
        Map<String, SkillInfo> skills = new HashMap<>();
        skills.put("Strength", new SkillInfo(200000, 80));

        stats.setSkills(skills);
        assertNotNull(stats.getSkills());
        assertEquals(1, stats.getSkills().size());
        assertTrue(stats.getSkills().containsKey("Strength"));
    }

    @Test
    public void testSkillInfoValues() {
        Map<String, SkillInfo> skills = new HashMap<>();
        skills.put("Hitpoints", new SkillInfo(1234567, 85));

        Stats stats = new Stats(skills);
        SkillInfo hp = stats.getSkills().get("Hitpoints");

        assertNotNull(hp);
        assertEquals(Integer.valueOf(1234567), hp.getXp());
        assertEquals(Integer.valueOf(85), hp.getLevel());
    }

    @Test
    public void testEmptySkillsMap() {
        Stats stats = new Stats(new HashMap<>());
        assertNotNull(stats.getSkills());
        assertTrue(stats.getSkills().isEmpty());
    }

    @Test
    public void testMultipleSkills() {
        Map<String, SkillInfo> skills = new LinkedHashMap<>();
        skills.put("Attack", new SkillInfo(0, 1));
        skills.put("Strength", new SkillInfo(0, 1));
        skills.put("Defence", new SkillInfo(0, 1));
        skills.put("Ranged", new SkillInfo(0, 1));
        skills.put("Prayer", new SkillInfo(0, 1));
        skills.put("Magic", new SkillInfo(0, 1));
        skills.put("Hitpoints", new SkillInfo(1154, 10));

        Stats stats = new Stats(skills);
        assertEquals(7, stats.getSkills().size());
    }
}
