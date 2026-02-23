package haexporterplugin.notifiers;

import haexporterplugin.data.ItemData;
import haexporterplugin.data.LootData;
import haexporterplugin.data.RareItemData;
import haexporterplugin.enums.LootCriteria;
import haexporterplugin.utils.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class LootNotifier extends BaseNotifier{

    private @Inject ItemManager itemManager;

    public LootData lastDrop = null;


    public static final String GAUNTLET_BOSS = "Crystalline Hunllef", THE_GAUNTLET = "The Gauntlet";
    public static final String CG_NAME = "Corrupted Gauntlet", CG_BOSS = "Corrupted Hunllef";
    public static final String TOA = "Tombs of Amascut";
    public static final String TOB = "Theatre of Blood";
    public static final String COX = "Chambers of Xeric";

    public static final Set<String> SPECIAL_LOOT_NPC_NAMES = Set.of("The Whisperer", "Araxxor",
            "Branda the Fire Queen", "Eldric the Ice King", GAUNTLET_BOSS, CG_BOSS);

    public void init() {
        itemNameAllowlist.clear();
        itemNameAllowlist.addAll(
                Utils.readDelimited(config.lootItemAllowlist())
                        .map(Utils::regexify)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );

        itemNameDenylist.clear();
        itemNameDenylist.addAll(
                Utils.readDelimited(config.lootItemDenylist())
                        .map(Utils::regexify)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );

        sourceDenylist.clear();
        sourceDenylist.addAll(
                Utils.readDelimited(config.lootSourceDenylist())
                        .map(String::toLowerCase)
                        .collect(Collectors.toList())
        );
    }

    private boolean matchesAnyPattern(Collection<Pattern> patterns, String itemName) {
        return patterns.stream().anyMatch(pattern -> pattern.matcher(itemName).matches());
    }

    public void onServerNpcLoot(ServerNpcLoot serverNpcLoot) {
        var comp = serverNpcLoot.getComposition();
        var items = ItemUtils.itemsToItemDataList(serverNpcLoot.getItems(), itemManager);
        this.handleNotify(items, comp.getName(), LootRecordType.NPC, comp.getId());
    }

    public void onNpcLootReceived(NpcLootReceived npcLootReceived) {

        NPC npc = npcLootReceived.getNpc();
        int id = npc.getId();

        var items = ItemUtils.itemsToItemDataList(npcLootReceived.getItems(), itemManager);
        this.handleNotify(items, npc.getName(), LootRecordType.NPC, id);
    }

    public void onPlayerLootReceived(PlayerLootReceived playerLootReceived) {
        if (WorldUtils.isSafeArea(client))
            return;
        var items = ItemUtils.itemsToItemDataList(playerLootReceived.getItems(), itemManager);
        handleNotify(items, playerLootReceived.getPlayer().getName(), LootRecordType.PLAYER, null);
    }

    public void onGameMessage(String message) {
        if ("You have found a Pharaoh's sceptre! It fell on the floor.".equals(message)) {
            var items = ItemUtils.itemStackToItemData(new ItemStack(ItemID.PHARAOHS_SCEPTRE, 1), itemManager);
            this.handleNotify(List.of(items), "Pyramid Plunder", LootRecordType.EVENT, null);
        }
    }

    public void onLootReceived(LootReceived lootReceived) {

        var items = ItemUtils.itemsToItemDataList(lootReceived.getItems(), itemManager);
        // only consider non-NPC and non-PK loot
        if (lootReceived.getType() == LootRecordType.EVENT || lootReceived.getType() == LootRecordType.PICKPOCKET) {
            if ("Barbarian Assault high gamble".equals(lootReceived.getName())) {
                // skip ba gambles
                return;
            }

            String source = getStandardizedSource(lootReceived);
            this.handleNotify(items, source, lootReceived.getType(), null);
        } else if (lootReceived.getType() == LootRecordType.NPC && SPECIAL_LOOT_NPC_NAMES.contains(lootReceived.getName())) {
            // Special case: upstream fires LootReceived for certain NPCs, but not NpcLootReceived
            String source = getStandardizedSource(lootReceived);
            var type = THE_GAUNTLET.equals(source) || CG_NAME.equals(source) ? LootRecordType.EVENT : lootReceived.getType();
            this.handleNotify(items, source, type, null);
        }
    }

    private final Collection<Pattern> itemNameAllowlist = new CopyOnWriteArrayList<>();
    private final Collection<Pattern> itemNameDenylist = new CopyOnWriteArrayList<>();
    private final Collection<String> sourceDenylist = new CopyOnWriteArraySet<>();

    private void handleNotify(Collection<ItemData> items, String dropper, LootRecordType type, Integer npcId) {
        if (type != LootRecordType.PLAYER && sourceDenylist.contains(dropper.toLowerCase())) {
            log.debug("Skipping loot notif for denied loot source: {} ({})", dropper, type);
            return;
        }

        final int minValue = config.minLootValue();

        Collection<ItemData> reduced = ItemUtils.reduceItemStack(items);
        List<ItemData> serializedItems = new ArrayList<>(reduced.size());

        long totalStackValue = 0;
        boolean sendMessage = false;
        boolean onAllowList = false;
        ItemData max = null;
        RareItemData rarest = null;

        final double rarityThreshold = config.lootRarityThreshold() > 0 ? 1.0 / config.lootRarityThreshold() : Double.NaN;
        final boolean intersection = config.lootRarityValueIntersection() && Double.isFinite(rarityThreshold);
        for (ItemData item : reduced) {
            long totalPrice = ItemUtils.getStackGePrice(item);

            OptionalDouble rarity;
            if (type == LootRecordType.NPC) {
                rarity = rarityUtils.getRarity(dropper, item.getId(), item.getQuantity());
            } else if (type == LootRecordType.PICKPOCKET) {
                rarity = thievingUtils.getRarity(dropper, item.getId(), item.getQuantity());
            } else {
                rarity = OptionalDouble.empty();
            }

            boolean shouldSend;
            var criteria = EnumSet.noneOf(LootCriteria.class);
            if (totalPrice >= minValue) {
                criteria.add(LootCriteria.VALUE);
            }
            if (MathUtils.lessThanOrEqual(rarity.orElse(1), rarityThreshold)) {
                criteria.add(LootCriteria.RARITY);
            }
            if (intersection) {
                shouldSend = criteria.contains(LootCriteria.VALUE) && (rarity.isEmpty() || criteria.contains(LootCriteria.RARITY));
            } else {
                shouldSend = criteria.contains(LootCriteria.VALUE) || criteria.contains(LootCriteria.RARITY);
            }

            boolean denied = matchesAnyPattern(itemNameDenylist, item.getName());
            if (denied) {
                shouldSend = false;
                criteria.add(LootCriteria.DENYLIST);
            } else {
                if (matchesAnyPattern(itemNameAllowlist, item.getName())) {
                    shouldSend = true;
                    onAllowList = true;
                    criteria.add(LootCriteria.ALLOWLIST);
                }
                if (max == null || totalPrice > ItemUtils.getStackGePrice(max)) {
                    max = item;
                }
            }

            if (shouldSend) {
                sendMessage = true;
            }

            if (rarity.isPresent()) {
                RareItemData rareItem = RareItemData.of(item, rarity.getAsDouble());
                serializedItems.add(rareItem);
                if (!denied && (rarest == null || rareItem.getRarity() < rarest.getRarity())) {
                    rarest = rareItem;
                }
            } else {
                serializedItems.add(item);
            }
            totalStackValue += totalPrice;
        }

        Evaluable sourceLink = type == LootRecordType.PLAYER
                ? ReplacementUtils.ofLink(dropper, config.playerLookupService().getPlayerUrl(dropper))
                : ReplacementUtils.ofWiki(dropper);

        if (sendMessage || onAllowList) {
            // Send regular loot notification (or allowlisted item)
            LootData lootData = new LootData(
                    serializedItems,
                    max,
                    totalStackValue,
                    sourceLink,
                    type,
                    npcId,
                    Collections.emptySet()
            );
            messageBuilder.addEvent("loot", lootData);
            tickUtils.sendNow();
        } else if (totalStackValue >= minValue && max != null && "Loot Chest".equalsIgnoreCase(dropper)) {
            // Special case: PK loot keys should trigger notification if total value exceeds configured minimum even
            // if no single item itself would exceed the min value config
            LootData lootData = new LootData(
                    serializedItems,
                    max,
                    totalStackValue,
                    sourceLink,
                    type,
                    npcId,
                    Collections.emptySet()
            );
            messageBuilder.addEvent("pkLoot", lootData);
            tickUtils.sendNow();
        }
    }

    public String getStandardizedSource(LootReceived event) {
        if (GAUNTLET_BOSS.equals(event.getName())) {
            return THE_GAUNTLET;
        } else if (CG_BOSS.equals(event.getName())) {
            return CG_NAME;
        } else if (lastDrop != null && shouldUseChatName(event)) {
            return lastDrop.getSource().toString(); // distinguish entry/expert/challenge modes
        }
        return event.getName();
    }

    private boolean shouldUseChatName(LootReceived event) {
        assert lastDrop != null;
        if (event.getType() != LootRecordType.EVENT) return false;
        String lastSource = lastDrop.getSource().toString();
        Predicate<String> coincides = source -> source.equals(event.getName()) && lastSource.startsWith(source);
        return coincides.test(TOA) || coincides.test(TOB) || coincides.test(COX);
    }

    public void onConfigChanged(String key, String value) {
        if ("lootSourceDenylist".equals(key)) {
            sourceDenylist.clear();
            sourceDenylist.addAll(
                    Utils.readDelimited(value)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList())
            );
            return;
        }

        Collection<Pattern> itemNames;
        if ("lootItemAllowlist".equals(key)) {
            itemNames = itemNameAllowlist;
        } else if ("lootItemDenylist".equals(key)) {
            itemNames = itemNameDenylist;
        } else {
            return;
        }

        itemNames.clear();
        itemNames.addAll(
                Utils.readDelimited(value)
                .map(Utils::regexify)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
        );
    }
}
