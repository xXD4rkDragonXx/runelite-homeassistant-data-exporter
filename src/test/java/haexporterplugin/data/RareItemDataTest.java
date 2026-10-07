package haexporterplugin.data;

import com.google.gson.Gson;
import org.junit.Test;

import static haexporterplugin.TestUtils.assertJsonEquals;

public class RareItemDataTest
{
	@Test
	public void testOfKeepsTheItemAndAddsItsRarity()
	{
		RareItemData rare = RareItemData.of(new ItemData("Dragon warhammer", 13576, 30000000, 72000, 2), 0.002);

		assertJsonEquals("{'rarity':0.002,'name':'Dragon warhammer','id':13576,'gePrice':30000000,'haPrice':72000,'quantity':2}",
			new Gson().toJson(rare));
	}
}
