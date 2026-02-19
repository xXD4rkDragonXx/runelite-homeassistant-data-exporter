package haexporterplugin.notifiers;

import haexporterplugin.data.Equipment;
import haexporterplugin.data.Inventory;
import haexporterplugin.data.ItemData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ItemNotifier extends BaseNotifier
{
    private @Inject ItemManager itemManager;

    public void onTick(){
        updateInventory();
    }

    private void updateInventory(){
        try {
            // Get current inventory items
            List<ItemData> inventoryItems = getInventoryItems();
            List<ItemData> equipmentItems = getEquippedItems();

            // Update messageBuilder with new inventory and equipment
            messageBuilder.setData("inventory", new Inventory(inventoryItems));
            messageBuilder.setData("equipment", new Equipment(equipmentItems));
        } catch (Exception e) {
            log.error("Error updating inventory/equipment", e);
        }
    }

    private List<ItemData> getInventoryItems()
    {
        List<ItemData> inventoryItems = new ArrayList<>();

        var invContainer = client.getItemContainer(InventoryID.INV);
        if (invContainer != null) {
            for (var item : invContainer.getItems()) {
                if (item.getId() <= 0) continue;
                ItemComposition ic = itemManager.getItemComposition(item.getId());
                ItemData itemData = createItemData(ic, item.getQuantity());
                inventoryItems.add(itemData);
            }
        }

        return inventoryItems;
    }

    private List<ItemData> getEquippedItems(){
        List<ItemData> equipmentItems = new ArrayList<>();

        var equipContainer = client.getItemContainer(InventoryID.WORN);
        if (equipContainer != null) {
            var items = equipContainer.getItems();
            for (int slotIndex = 0; slotIndex < items.length; slotIndex++) {
                var item = items[slotIndex];
                if (item.getId() <= 0) continue; // Skip empty slots
                ItemComposition ic = itemManager.getItemComposition(item.getId());
                ItemData itemData = createItemData(ic, item.getQuantity());

                // Map slot index to EquipmentInventorySlot enum and get its name
                if (slotIndex < EquipmentInventorySlot.values().length) {
                    String slotName = String.valueOf(slotIndex);
                    try {
                        slotName = net.runelite.api.EquipmentInventorySlot.values()[slotIndex].name();
                    } catch (Exception e) {
                        log.warn("Could not map slot index {} to EquipmentInventorySlot", slotIndex);
                    }
                    itemData.setEquipmentSlot(slotName);
                }
                equipmentItems.add(itemData);
            }
        }
        return equipmentItems;
    }

    private ItemData createItemData(ItemComposition ic, int quantity){
        return new ItemData(
            ic.getName(),
            ic.getId(),
            itemManager.getItemPrice(ic.getId()),
            ic.getHaPrice(),
            quantity
        );
    }
}
