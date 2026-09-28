package haexporterplugin.notifiers;

import com.google.common.annotations.VisibleForTesting;
import haexporterplugin.data.CollectionData;
import haexporterplugin.utils.KillCountTracker;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.ItemComposition;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CollectionNotifier extends BaseNotifier {

    private @Inject ItemManager itemManager;
    private @Inject ClientThread clientThread;
    private @Inject KillCountTracker killCountTracker;

    private static final String COLLECTION_LOG_TITLE = "Collection log";
    private static final String NEW_ITEM_PREFIX = "New item:";

    // Value of the in-game "Collection log - New addition notification" setting that only uses chat (no popup)
    private static final int NEW_ITEM_NOTIFICATION_CHAT_ONLY = 1;

    // Item definitions scanned per client tick while building the name lookup
    private static final int ITEM_LOOKUP_BATCH_SIZE = 2000;

    private static final Pattern COLLECTION_LOG_PATTERN = Pattern.compile(
            "New item added to your collection log: (?<itemName>.+)"
    );

    private static final Pattern KILL_COUNT_PATTERN = Pattern.compile(
            "Your (?<key>.+) (?:kill|success|harvest|lap|completion) count is: (?<value>[\\d,]+)\\b"
    );

    private static final Pattern KILL_COUNT_PATTERN_SECONDARY = Pattern.compile(
            "Your (?:completed )?(?<key>.+?) count is: (?<value>[\\d,]+)\\b"
    );

    // Lowercase item name -> canonical item id, built in batches after login.
    private Map<String, Integer> itemIdByName = null;
    private Map<String, Integer> partialItemIdByName = null;
    private int nextItemId = 0;

    private boolean notificationStarted = false;

    public void onGameStateChanged(GameStateChanged event) {
        switch (event.getGameState()) {
            case LOGGED_IN:
                startItemNameLookup();
                break;
            case HOPPING:
            case LOGIN_SCREEN:
                killCountTracker.reset();
                break;
        }
    }

    // Popup mode: the collection log popup is driven by the same client scripts that draw the on-screen notification.
    public void onScript(ScriptPreFired event) {
        switch (event.getScriptId()) {
            case ScriptID.NOTIFICATION_START:
                notificationStarted = true;
                break;
            case ScriptID.NOTIFICATION_DELAY:
                if (!notificationStarted) return;
                notificationStarted = false;

                String itemName = parsePopupItemName(
                        client.getVarcStrValue(VarClientID.NOTIFICATION_TITLE),
                        client.getVarcStrValue(VarClientID.NOTIFICATION_MAIN));
                if (itemName != null) {
                    clientThread.invokeAtTickEnd(() -> handleCollectionLog(itemName));
                }
                break;
        }
    }

    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE) return;

        String message = Text.removeTags(event.getMessage());
        Integer killCount = parseKillCount(message);
        if (killCount != null) {
            killCountTracker.onKillCount(killCount, client.getTickCount());
            return;
        }

        // Chat-only mode shows no popup, so the chat message is the only signal; otherwise the popup is used,
        // which keeps an item from being reported twice
        if (client.getVarbitValue(VarbitID.OPTION_COLLECTION_NEW_ITEM) != NEW_ITEM_NOTIFICATION_CHAT_ONLY) return;

        Matcher matcher = COLLECTION_LOG_PATTERN.matcher(message);
        if (matcher.matches()) {
            String itemName = matcher.group("itemName").trim();
            // At tick end, so a kill count or loot from the same tick is seen first
            clientThread.invokeAtTickEnd(() -> handleCollectionLog(itemName));
        }
    }

    /**
     * @return the item name from a "Collection log" popup, or null for any other popup
     */
    @Nullable
    @VisibleForTesting
    static String parsePopupItemName(@Nullable String title, @Nullable String body) {
        if (!COLLECTION_LOG_TITLE.equalsIgnoreCase(title) || body == null) return null;

        String text = Text.removeTags(body);
        if (!text.startsWith(NEW_ITEM_PREFIX)) return null;

        String itemName = text.substring(NEW_ITEM_PREFIX.length()).trim();
        return itemName.isEmpty() ? null : itemName;
    }

    @Nullable
    @VisibleForTesting
    static Integer parseKillCount(String message) {
        Matcher matcher = KILL_COUNT_PATTERN.matcher(message);
        if (!matcher.find()) {
            matcher = KILL_COUNT_PATTERN_SECONDARY.matcher(message);
            if (!matcher.find()) return null;
        }
        try {
            return Integer.parseInt(matcher.group("value").replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void handleCollectionLog(String itemName) {
        int itemId = resolveItemId(itemName);
        long value = itemId > 0 ? itemManager.getItemPrice(itemId) : 0L;

        // Items without a price (pets, untradeables, unresolved names) are always sent.
        if (value > 0 && value < config.clogMinValue()) {
            log.debug("Skipping collection log item below minimum value: {} ({} gp)", itemName, value);
            return;
        }

        Integer killCount = killCountTracker.getKillCount(client.getTickCount());

        log.debug("Collection log item obtained: {} (id: {}, value: {}, kc: {})", itemName, itemId, value, killCount);

        messageBuilder.addEvent("collectionLog", new CollectionData(itemName, itemId, value, killCount));
        tickUtils.sendNow();
    }

    private int resolveItemId(String itemName) {
        // Normally already built after login; finish it now if an item shows up before that
        while (!buildItemNameLookupBatch()) {
            // keep scanning
        }
        return itemIdByName.getOrDefault(itemName.toLowerCase(), -1);
    }

    // Scanning every item definition at once stalls the client, so it's spread over client ticks
    private void startItemNameLookup() {
        if (itemIdByName != null || partialItemIdByName != null) return;
        clientThread.invokeLater(this::buildItemNameLookupBatch);
    }

    /**
     * @return true once the lookup is complete
     */
    private boolean buildItemNameLookupBatch() {
        if (itemIdByName != null) return true;
        if (partialItemIdByName == null) {
            partialItemIdByName = new HashMap<>();
            nextItemId = 0;
        }

        int count = client.getItemCount();
        int end = Math.min(count, nextItemId + ITEM_LOOKUP_BATCH_SIZE);
        for (int id = nextItemId; id < end; id++) {
            int canonical = itemManager.canonicalize(id);
            if (canonical != id) continue;

            ItemComposition composition = itemManager.getItemComposition(canonical);
            if (composition.getNote() != -1 || composition.getPlaceholderTemplateId() != -1) continue;

            String name = composition.getName();
            if (name == null || name.isEmpty() || "null".equals(name)) continue;

            partialItemIdByName.putIfAbsent(name.toLowerCase(), canonical);
        }
        nextItemId = end;
        if (end < count) return false;

        itemIdByName = partialItemIdByName;
        partialItemIdByName = null;
        return true;
    }
}
