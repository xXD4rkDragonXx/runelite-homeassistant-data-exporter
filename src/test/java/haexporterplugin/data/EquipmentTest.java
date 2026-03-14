package haexporterplugin.data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class EquipmentTest
{
	@Test
	public void testDefaultConstructor()
	{
		Equipment equipment = new Equipment();
		assertNull(equipment.getItems());
	}

	@Test
	public void testParameterizedConstructor()
	{
		ItemData item = new ItemData("Dragon defender", 12954, 500000, 68000, 1);
		item.setEquipmentSlot("SHIELD");
		List<ItemData> items = Arrays.asList(item);

		Equipment equipment = new Equipment(items);

		assertNotNull(equipment.getItems());
		assertEquals(1, equipment.getItems().size());
		assertEquals("SHIELD", equipment.getItems().get(0).getEquipmentSlot());
	}

	@Test
	public void testSetItems()
	{
		Equipment equipment = new Equipment();
		List<ItemData> items = new ArrayList<>();
		items.add(new ItemData("Abyssal whip", 4151, 1650000, 72000, 1));
		equipment.setItems(items);

		assertEquals(1, equipment.getItems().size());
	}

	@Test
	public void testEmptyEquipment()
	{
		Equipment equipment = new Equipment(new ArrayList<>());
		assertNotNull(equipment.getItems());
		assertTrue(equipment.getItems().isEmpty());
	}
}
