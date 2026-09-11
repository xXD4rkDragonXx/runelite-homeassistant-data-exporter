package haexporterplugin.notifiers;

import haexporterplugin.data.CollectionData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.ItemComposition;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.VarClientID;
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

    private static final String COLLECTION_LOG_TITLE = "Collection log";
    private static final String NEW_ITEM_PREFIX = "New item:";

    // Attach a kill count parsed from chat only if it was seen within this many ticks of the popup.
    private static final int KILL_COUNT_MAX_TICK_AGE = 5;

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

    private boolean notificationStarted = false;

    // The collection log popup is driven by the same client scripts that draw the on-screen notification.
    public void onScript(ScriptPreFired event) {
        switch (event.getScriptId()) {
            case ScriptID.NOTIFICATION_START:
                notificationStarted = true;
                break;
            case ScriptID.NOTIFICATION_DELAY:
                if (!notificationStarted) return;
                notificationStarted = false;

                String title = client.getVarcStrValue(VarClientID.NOTIFICATION_TITLE);
                if (!COLLECTION_LOG_TITLE.equalsIgnoreCase(title)) return;

                String body = Text.removeTags(client.getVarcStrValue(VarClientID.NOTIFICATION_MAIN));
                if (body.startsWith(NEW_ITEM_PREFIX)) {
                    body = body.substring(NEW_ITEM_PREFIX.length());
                }
                handleCollectionLog(body.trim());
                break;
        }
    }

    // Kill count is not carried by the popup, so it is still scraped from chat and paired by tick.
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE) return;
        tryParseKillCount(Text.removeTags(event.getMessage()));
    }

    private void tryParseKillCount(String message) {
        Matcher matcher = KILL_COUNT_PATTERN.matcher(message);
        if (!matcher.find()) {
            matcher = KILL_COUNT_PATTERN_SECONDARY.matcher(message);
            if (!matcher.find()) return;
        }
        try {
            lastKillCount = Integer.parseInt(matcher.group("value").replace(",", ""));
            lastKillCountTick = client.getTickCount();
        } catch (NumberFormatException ignored) {
            // ignore malformed counts
        }
    }

    private void handleCollectionLog(String itemName) {
        if (itemName.isEmpty()) return;

        int itemId = resolveItemId(itemName);
        long value = itemId > 0 ? (long) itemManager.getItemPrice(itemId) : 0L;

        Integer killCount = (client.getTickCount() - lastKillCountTick) <= KILL_COUNT_MAX_TICK_AGE ? lastKillCount : null;

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
