package haexporterplugin.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class ItemDataTest {

    @Test
    public void testDefaultConstructorSetsQuantityToOne() {
        ItemData item = new ItemData();
        assertEquals(1, item.getQuantity());
    }

    @Test
    public void testDefaultConstructorOtherFieldsNull() {
        ItemData item = new ItemData();
        assertNull(item.getName());
        assertEquals(0, item.getId());
        assertEquals(0, item.getGePrice());
        assertEquals(0, item.getHaPrice());
        assertNull(item.getEquipmentSlot());
    }

    @Test
    public void testParameterizedConstructor() {
        ItemData item = new ItemData("Abyssal whip", 4151, 2500000, 120001, 1);
        assertEquals("Abyssal whip", item.getName());
        assertEquals(4151, item.getId());
        assertEquals(2500000, item.getGePrice());
        assertEquals(120001, item.getHaPrice());
        assertEquals(1, item.getQuantity());
    }

    @Test
    public void testStackableItem() {
        ItemData item = new ItemData("Cannonball", 2, 200, 5, 500);
        assertEquals("Cannonball", item.getName());
        assertEquals(500, item.getQuantity());
    }

    @Test
    public void testSetEquipmentSlot() {
        ItemData item = new ItemData("Dragon boots", 11840, 200000, 90000, 1);
        item.setEquipmentSlot("BOOTS");
        assertEquals("BOOTS", item.getEquipmentSlot());
    }

    @Test
    public void testSetters() {
        ItemData item = new ItemData();
        item.setName("Bronze sword");
        item.setId(1277);
        item.setGePrice(50);
        item.setHaPrice(20);
        item.setQuantity(3);

        assertEquals("Bronze sword", item.getName());
        assertEquals(1277, item.getId());
        assertEquals(50, item.getGePrice());
        assertEquals(20, item.getHaPrice());
        assertEquals(3, item.getQuantity());
    }

    @Test
    public void testZeroPriceItem() {
        ItemData item = new ItemData("Coins", 995, 0, 0, 1000000);
        assertEquals(0, item.getGePrice());
        assertEquals(0, item.getHaPrice());
        assertEquals(1000000, item.getQuantity());
    }
}
