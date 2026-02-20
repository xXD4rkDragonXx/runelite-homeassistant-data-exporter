package haexporterplugin.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class SkillInfoTest {

    @Test
    public void testDefaultConstructor() {
        SkillInfo skill = new SkillInfo();
        assertNull(skill.getXp());
        assertNull(skill.getLevel());
    }

    @Test
    public void testParameterizedConstructor() {
        SkillInfo skill = new SkillInfo(1000000, 75);
        assertEquals(Integer.valueOf(1000000), skill.getXp());
        assertEquals(Integer.valueOf(75), skill.getLevel());
    }

    @Test
    public void testSetAndGetXp() {
        SkillInfo skill = new SkillInfo();
        skill.setXp(5000000);
        assertEquals(Integer.valueOf(5000000), skill.getXp());
    }

    @Test
    public void testSetAndGetLevel() {
        SkillInfo skill = new SkillInfo();
        skill.setLevel(99);
        assertEquals(Integer.valueOf(99), skill.getLevel());
    }

    @Test
    public void testMaxXp() {
        SkillInfo skill = new SkillInfo(200000000, 99);
        assertEquals(Integer.valueOf(200000000), skill.getXp());
    }

    @Test
    public void testZeroValues() {
        SkillInfo skill = new SkillInfo(0, 1);
        assertEquals(Integer.valueOf(0), skill.getXp());
        assertEquals(Integer.valueOf(1), skill.getLevel());
    }

    @Test
    public void testUpdateValues() {
        SkillInfo skill = new SkillInfo(0, 1);
        skill.setXp(83);
        skill.setLevel(2);
        assertEquals(Integer.valueOf(83), skill.getXp());
        assertEquals(Integer.valueOf(2), skill.getLevel());
    }
}
