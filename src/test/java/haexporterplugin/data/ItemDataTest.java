package haexporterplugin.data;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.*;

public class ItemDataTest
{
	@Test
	public void testDefaultQuantityIsOne()
	{
		assertEquals(1, new ItemData().getQuantity());
	}

	@Test
	public void testCopyConstructorCopiesEveryField()
	{
		ItemData original = new ItemData("Dragon defender", 12954, 3_000_000_000L, 68000, 2);
		original.setEquipmentSlot("SHIELD");
		original.setInventorySlot(5);

		Gson gson = new Gson();
		assertEquals(gson.toJson(original), gson.toJson(new ItemData(original)));
	}
}
