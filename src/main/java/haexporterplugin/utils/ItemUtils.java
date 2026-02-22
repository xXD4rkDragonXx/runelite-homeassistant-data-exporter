package haexporterplugin.utils;

import com.google.common.collect.ImmutableSet;
import haexporterplugin.data.ItemData;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import static net.runelite.api.gameval.ItemID.*;

@Slf4j
@UtilityClass
public class ItemUtils {

    private final Set<Integer> NEVER_KEPT_ITEMS = ImmutableSet.of(
            BH_CLUE_BOX, LOOTING_BAG, FLAMTAER_BAG, II_JAR_GENERATOR,
            DAMNED_AMULET_DEGRADED, RING_OF_CHAROS, RING_OF_CHAROS_UNLOCKED,
            WILD_CAVE_BRACELET_CHARGED, WILD_CAVE_BRACELET_UNCHARGED,
            ANMA_50_REWARD, ANMA_30_REWARD, FAIRY_ENCHANTED_SECATEURS, II_MAGIC_BUTTERFLY_NET,
            GAUNTLETS_OF_COOKING, GAUNTLETS_OF_GOLDSMITHING, GAUNTLETS_OF_CHAOS, STEEL_GAUNTLETS,
            FRISD_JESTER_HAT, FRISD_JESTER_TOP, FRISD_JESTER_LEGS, FRISD_JESTER_BOOTS,
            LUNAR_HELMET, LUNAR_TORSO, LUNAR_LEGS, LUNAR_GLOVES, LUNAR_BOOTS,
            LUNAR_CAPE, LUNAR_AMULET, LUNAR_RING, LUNAR_MOONCLAN_LIMINAL_STAFF,
            LEAGUE_3_ADAMANT_TROPHY, LEAGUE_3_BRONZE_TROPHY, LEAGUE_3_DRAGON_TROPHY,
            LEAGUE_3_IRON_TROPHY, LEAGUE_3_MITHRIL_TROPHY, LEAGUE_3_RUNE_TROPHY, LEAGUE_3_STEEL_TROPHY,
            LEAGUE_5_ADAMANT_TROPHY, LEAGUE_5_BRONZE_TROPHY, LEAGUE_5_DRAGON_TROPHY,
            LEAGUE_5_IRON_TROPHY, LEAGUE_5_MITHRIL_TROPHY, LEAGUE_5_RUNE_TROPHY, LEAGUE_5_STEEL_TROPHY,
            TRAILBLAZER_ADAMANT_TROPHY, TRAILBLAZER_BRONZE_TROPHY, TRAILBLAZER_DRAGON_TROPHY, TRAILBLAZER_IRON_TROPHY,
            TRAILBLAZER_MITHRIL_TROPHY, TRAILBLAZER_RUNE_TROPHY, TRAILBLAZER_STEEL_TROPHY,
            TWISTED_ADAMANT_TROPHY, TWISTED_BRONZE_TROPHY, TWISTED_DRAGON_TROPHY, TWISTED_IRON_TROPHY,
            TWISTED_MITHRIL_TROPHY, TWISTED_RUNE_TROPHY, TWISTED_STEEL_TROPHY
    );

    private final BinaryOperator<ItemData> SUM_ITEM_QUANTITIES = (a, b) -> new ItemData(a.getName(), a.getId(), a.getGePrice(), a.getHaPrice(), a.getQuantity() + b.getQuantity());
    private final BinaryOperator<ItemData> SUM_ITEM_STACK_QUANTITIES = (a, b) -> new ItemData(a.getName(), a.getId(), a.getGePrice(), a.getHaPrice(), a.getQuantity() + b.getQuantity());

    public List<ItemData> getInventoryItems(Client client, ItemManager itemManager)
    {
        List<ItemData> inventoryItems = new ArrayList<>();

        var invContainer = client.getItemContainer(InventoryID.INV);
        if (invContainer != null) {
            for (var item : invContainer.getItems()) {
                if (item.getId() <= 0) continue;
                ItemComposition ic = itemManager.getItemComposition(item.getId());
                ItemData itemData = createItemData(ic, item.getQuantity(), itemManager);
                inventoryItems.add(itemData);
            }
        }

        return inventoryItems;
    }

    public List<ItemData> getEquippedItems(Client client, ItemManager itemManager){
        List<ItemData> equipmentItems = new ArrayList<>();

        var equipContainer = client.getItemContainer(InventoryID.WORN);
        if (equipContainer != null) {
            var items = equipContainer.getItems();
            for (int slotIndex = 0; slotIndex < items.length; slotIndex++) {
                var item = items[slotIndex];
                if (item.getId() <= 0) continue; // Skip empty slots
                ItemComposition ic = itemManager.getItemComposition(item.getId());
                ItemData itemData = createItemData(ic, item.getQuantity(), itemManager);

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

    public ItemData createItemData(ItemComposition ic, int quantity, ItemManager itemManager){
        return new ItemData(
                ic.getName(),
                ic.getId(),
                itemManager.getItemPrice(ic.getId()),
                ic.getHaPrice(),
                quantity
        );
    }

    public boolean isItemNeverKeptOnDeath(int itemId) {
        // https://oldschool.runescape.wiki/w/Items_Kept_on_Death#Items_that_are_never_kept
        // https://oldschoolrunescape.fandom.com/wiki/Items_Kept_on_Death#Items_that_are_never_kept
        return NEVER_KEPT_ITEMS.contains(itemId);
    }

    public int getPrice(int id, ItemManager itemManager){
        return itemManager.getItemPrice(id);
    }

    public long getStackGePrice(ItemData items){
        List<ItemData> itemList = new ArrayList<>(1);
        itemList.add(items);
        return getStackGePrice(itemList);
    }

    public long getStackGePrice(List<ItemData> items){
        long totalPrice = 0;
        for (ItemData item : items){
            int stackPrice = item.getGePrice() * item.getQuantity();
            totalPrice += stackPrice;
        }
        return totalPrice;
    }

    public <K, V> Map<K, V> reduce(Iterable<V> items, Function<V, K> deriveKey, BinaryOperator<V> aggregate) {
        final Map<K, V> map = new LinkedHashMap<>();
        items.forEach(v -> map.merge(deriveKey.apply(v), v, aggregate));
        return map;
    }

    public Map<Integer, ItemData> reduceItems(Iterable<ItemData> items) {
        return reduce(items, ItemData::getId, SUM_ITEM_QUANTITIES);
    }

    public List<ItemData> itemsToItemDataList(Collection<ItemStack> itemStacks, ItemManager itemManager){
        List<ItemData> itemDataList = new ArrayList<>(itemStacks.size());
        for (ItemStack itemStack: itemStacks ){
            itemDataList.add(itemStackToItemData(itemStack, itemManager));
        }
        return itemDataList;
    }

    public ItemData itemStackToItemData(ItemStack itemStack, ItemManager itemManager){
        ItemComposition ic = itemManager.getItemComposition(itemStack.getId());
        return createItemData(ic, itemStack.getQuantity(), itemManager);
    }

    public Collection<ItemData> reduceItemStack(Iterable<ItemData> items) {
        return reduce(items, ItemData::getId, SUM_ITEM_STACK_QUANTITIES).values();
    }
}
