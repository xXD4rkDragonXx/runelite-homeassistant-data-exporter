package haexporterplugin.data;

import com.google.gson.Gson;
import org.junit.Test;

import static haexporterplugin.TestUtils.json;
import static org.junit.Assert.assertEquals;

public class RareItemDataTest
{
	@Test
	public void testOfKeepsTheItemAndAddsItsRarity()
	{
		RareItemData rare = RareItemData.of(new ItemData("Dragon warhammer", 13576, 30000000, 72000, 1), 0.002);

		assertEquals(json("{'rarity':0.002,'name':'Dragon warhammer','id':13576,'gePrice':30000000,'haPrice':72000,'quantity':1}"),
			new Gson().toJsonTree(rare));
	}
}
