package haexporterplugin.data;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.*;

public class SpellbookDataTest {

    @Test
    public void testGetSpellbookNameStandard() {
        assertEquals("standard", SpellbookData.getSpellbookName(0));
    }

    @Test
    public void testGetSpellbookNameAncient() {
        assertEquals("ancient", SpellbookData.getSpellbookName(1));
    }

    @Test
    public void testGetSpellbookNameLunar() {
        assertEquals("lunar", SpellbookData.getSpellbookName(2));
    }

    @Test
    public void testGetSpellbookNameArceuus() {
        assertEquals("arceuus", SpellbookData.getSpellbookName(3));
    }

    @Test
    public void testGetSpellbookNameUnknown() {
        assertEquals("unknown", SpellbookData.getSpellbookName(4));
        assertEquals("unknown", SpellbookData.getSpellbookName(-1));
        assertEquals("unknown", SpellbookData.getSpellbookName(100));
    }

    @Test
    public void testConstructorSetsFields() {
        Gson gson = new Gson();
        SpellbookData spellbook = new SpellbookData(1);
        String json = gson.toJson(spellbook);

        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"name\":\"ancient\""));
    }

    @Test
    public void testConstructorStandardSpellbook() {
        Gson gson = new Gson();
        SpellbookData spellbook = new SpellbookData(0);
        String json = gson.toJson(spellbook);

        assertTrue(json.contains("\"id\":0"));
        assertTrue(json.contains("\"name\":\"standard\""));
    }

    @Test
    public void testConstructorUnknownSpellbook() {
        Gson gson = new Gson();
        SpellbookData spellbook = new SpellbookData(99);
        String json = gson.toJson(spellbook);

        assertTrue(json.contains("\"id\":99"));
        assertTrue(json.contains("\"name\":\"unknown\""));
    }
}
