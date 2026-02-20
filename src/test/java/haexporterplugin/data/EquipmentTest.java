package haexporterplugin.data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EquipmentTest {

    @Test
    public void testDefaultConstructor() {
        Equipment equipment = new Equipment();
        assertNull(equipment.getItems());
    }

    @Test
    public void testParameterizedConstructor() {
        List<ItemData> items = new ArrayList<>();
        ItemData weapon = new ItemData("Abyssal whip", 4151, 2500000, 120001, 1);
        weapon.setEquipmentSlot("WEAPON");
        items.add(weapon);

        Equipment equipment = new Equipment(items);
        assertNotNull(equipment.getItems());
        assertEquals(1, equipment.getItems().size());
    }

    @Test
    public void testSetAndGetItems() {
        Equipment equipment = new Equipment();
        List<ItemData> items = new ArrayList<>();

        ItemData helm = new ItemData("Rune full helm", 1163, 20000, 12000, 1);
        helm.setEquipmentSlot("HEAD");
        items.add(helm);

        equipment.setItems(items);
        assertNotNull(equipment.getItems());
        assertEquals(1, equipment.getItems().size());
        assertEquals("HEAD", equipment.getItems().get(0).getEquipmentSlot());
    }

    @Test
    public void testEmptyEquipment() {
        Equipment equipment = new Equipment(new ArrayList<>());
        assertNotNull(equipment.getItems());
        assertTrue(equipment.getItems().isEmpty());
    }

    @Test
    public void testMultipleEquipmentSlots() {
        List<ItemData> items = new ArrayList<>();

        ItemData head = new ItemData("Helm of neitiznot", 10828, 50000, 35000, 1);
        head.setEquipmentSlot("HEAD");
        items.add(head);

        ItemData body = new ItemData("Fighter torso", 10551, 0, 0, 1);
        body.setEquipmentSlot("BODY");
        items.add(body);

        ItemData legs = new ItemData("Rune platelegs", 1079, 38000, 25000, 1);
        legs.setEquipmentSlot("LEGS");
        items.add(legs);

        Equipment equipment = new Equipment(items);
        assertEquals(3, equipment.getItems().size());
    }
}
