package haexporterplugin.notifiers;

import haexporterplugin.data.Equipment;
import haexporterplugin.data.Inventory;
import haexporterplugin.data.ItemData;
import haexporterplugin.utils.ItemUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.util.List;

@Slf4j
public class ItemNotifier extends BaseNotifier
{
    private @Inject ItemUtils itemUtils;
    private @Inject Client client;
    private @Inject ItemManager itemManager;

    public void onTick(){
        updateInventory();
    }

    private void updateInventory(){
        try {
            // Get current inventory items
            List<ItemData> inventoryItems = itemUtils.getInventoryItems(client, itemManager);
            List<ItemData> equipmentItems = itemUtils.getEquippedItems(client, itemManager);

            // Update messageBuilder with new inventory and equipment
            messageBuilder.setData("inventory", new Inventory(inventoryItems));
            messageBuilder.setData("equipment", new Equipment(equipmentItems));
        } catch (Exception e) {
            log.error("Error updating inventory/equipment", e);
        }
    }

}
