package haexporterplugin.notifiers;

import haexporterplugin.data.CollectionData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CollectionNotifier extends BaseNotifier {

    private @Inject ItemManager itemManager;

    private static final Pattern COLLECTION_LOG_PATTERN = Pattern.compile(
            "New item added to your collection log: (?<itemName>.+)"
    );

    private static final Pattern KILL_COUNT_PATTERN = Pattern.compile(
            "Your (?<key>.+) (?:kill|success|harvest|lap|completion) count is: (?<value>[\\d,]+)\\b"
    );

    private static final Pattern KILL_COUNT_PATTERN_SECONDARY = Pattern.compile(
            "Your (?:completed )?(?<key>.+?) count is: (?<value>[\\d,]+)\\b"
    );

    // Lazily-built lowercase item name -> canonical item id lookup.
    private Map<String, Integer> itemIdByName = null;

    // Most recent kill count parsed from chat, paired with the tick it was seen on.
    private Integer lastKillCount = null;
    private int lastKillCountTick = -1;

    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE) return;

        String message = Text.removeTags(event.getMessage());

        if (tryParseKillCount(message)) {
            return;
        }

        Matcher matcher = COLLECTION_LOG_PATTERN.matcher(message);
        if (matcher.matches()) {
            handleCollectionLog(matcher.group("itemName").trim());
        }
    }

    private boolean tryParseKillCount(String message) {
        Matcher matcher = KILL_COUNT_PATTERN.matcher(message);
        if (!matcher.find()) {
            matcher = KILL_COUNT_PATTERN_SECONDARY.matcher(message);
            if (!matcher.find()) {
                return false;
            }
        }
        try {
            lastKillCount = Integer.parseInt(matcher.group("value").replace(",", ""));
            lastKillCountTick = client.getTickCount();
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private void handleCollectionLog(String itemName) {
        int itemId = resolveItemId(itemName);
        long value = itemId > 0 ? (long) itemManager.getItemPrice(itemId) : 0L;

        // Kill count and collection log messages arrive on the same (or adjacent) tick.
        Integer killCount = (client.getTickCount() - lastKillCountTick) <= 1 ? lastKillCount : null;

        log.debug("Collection log item obtained: {} (id: {}, value: {}, kc: {})", itemName, itemId, value, killCount);

        messageBuilder.addEvent("collectionLog", new CollectionData(itemName, itemId, value, killCount));
        tickUtils.sendNow();
    }

    private int resolveItemId(String itemName) {
        if (itemIdByName == null) {
            buildItemNameLookup();
        }
        return itemIdByName.getOrDefault(itemName.toLowerCase(), -1);
    }

    private void buildItemNameLookup() {
        Map<String, Integer> lookup = new HashMap<>();
        int count = client.getItemCount();
        for (int id = 0; id < count; id++) {
            int canonical = itemManager.canonicalize(id);
            if (canonical != id) continue;

            ItemComposition composition = itemManager.getItemComposition(canonical);
            if (composition.getNote() != -1 || composition.getPlaceholderTemplateId() != -1) continue;

            String name = composition.getName();
            if (name == null || name.isEmpty() || "null".equals(name)) continue;

            lookup.putIfAbsent(name.toLowerCase(), canonical);
        }
        itemIdByName = lookup;
    }
}
