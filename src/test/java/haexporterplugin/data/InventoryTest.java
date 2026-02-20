package haexporterplugin.data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class InventoryTest {

    @Test
    public void testDefaultConstructor() {
        Inventory inventory = new Inventory();
        assertNull(inventory.getItems());
    }

    @Test
    public void testParameterizedConstructor() {
        List<ItemData> items = new ArrayList<>();
        items.add(new ItemData("Shark", 385, 800, 0, 5));
        items.add(new ItemData("Super restore(4)", 3024, 10000, 100, 3));

        Inventory inventory = new Inventory(items);
        assertNotNull(inventory.getItems());
        assertEquals(2, inventory.getItems().size());
    }

    @Test
    public void testSetAndGetItems() {
        Inventory inventory = new Inventory();
        List<ItemData> items = new ArrayList<>();
        items.add(new ItemData("Rune arrow", 892, 100, 50, 200));

        inventory.setItems(items);
        assertNotNull(inventory.getItems());
        assertEquals(1, inventory.getItems().size());
        assertEquals("Rune arrow", inventory.getItems().get(0).getName());
    }

    @Test
    public void testEmptyInventory() {
        Inventory inventory = new Inventory(new ArrayList<>());
        assertNotNull(inventory.getItems());
        assertTrue(inventory.getItems().isEmpty());
    }

    @Test
    public void testFullInventory() {
        List<ItemData> items = new ArrayList<>();
        for (int i = 0; i < 28; i++) {
            items.add(new ItemData("Item " + i, i, i * 100, i * 50, 1));
        }

        Inventory inventory = new Inventory(items);
        assertEquals(28, inventory.getItems().size());
    }

    @Test
    public void testReplaceItems() {
        List<ItemData> original = new ArrayList<>();
        original.add(new ItemData("Old item", 1, 100, 50, 1));
        Inventory inventory = new Inventory(original);

        List<ItemData> replacement = new ArrayList<>();
        replacement.add(new ItemData("New item", 2, 200, 100, 1));
        inventory.setItems(replacement);

        assertEquals(1, inventory.getItems().size());
        assertEquals("New item", inventory.getItems().get(0).getName());
    }
}
