package haexporterplugin.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExceptionalDeathTest {

    @Test
    public void testCoxDisplayName() {
        assertEquals("Chambers of Xeric", ExceptionalDeath.COX.toString());
    }

    @Test
    public void testDiaryResurrectionDisplayName() {
        assertEquals("Diary Resurrection", ExceptionalDeath.DIARY_RESURRECTION.toString());
    }

    @Test
    public void testFightCaveDisplayName() {
        assertEquals("Fight Caves", ExceptionalDeath.FIGHT_CAVE.toString());
    }

    @Test
    public void testGauntletDisplayName() {
        assertEquals("Gauntlet", ExceptionalDeath.GAUNTLET.toString());
    }

    @Test
    public void testInfernoDisplayName() {
        assertEquals("Inferno", ExceptionalDeath.INFERNO.toString());
    }

    @Test
    public void testJadChallengesDisplayName() {
        assertEquals("Jad challenges", ExceptionalDeath.JAD_CHALLENGES.toString());
    }

    @Test
    public void testTobDisplayName() {
        assertEquals("Theatre of Blood", ExceptionalDeath.TOB.toString());
    }

    @Test
    public void testToaDisplayName() {
        assertEquals("Tombs of Amascut", ExceptionalDeath.TOA.toString());
    }

    @Test
    public void testAllValuesCount() {
        assertEquals(8, ExceptionalDeath.values().length);
    }

    @Test
    public void testDangerEnumValues() {
        assertEquals(3, Danger.values().length);
        assertNotNull(Danger.SAFE);
        assertNotNull(Danger.DANGEROUS);
        assertNotNull(Danger.EXCEPTIONAL);
    }
}
