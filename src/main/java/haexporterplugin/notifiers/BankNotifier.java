package haexporterplugin.notifiers;

import com.google.common.annotations.VisibleForTesting;
import haexporterplugin.data.BankValue;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Keeps the player's "bank" field: the GE value of the bank and when it was read. It is read once when
 * the bank is opened, and again when it is closed if the value changed meanwhile (so withdrawn gear isn't
 * counted twice, once in the bank and once in the inventory). Every message carries the last reading, so
 * a lost message loses nothing. The reading is also kept in the account's RuneLite profile, so the next
 * login starts with it instead of waiting for the bank to be opened again.
 *
 * Everything runs on the client thread from RuneLite events and is cheap: one price lookup (a cache read)
 * per bank slot, at most twice per visit, and an in-memory config write that RuneLite saves later.
 */
@Slf4j
@Singleton
public class BankNotifier extends BaseNotifier {

    static final String CONFIG_GROUP = "HAExporter";
    static final String VALUE_KEY = "bankValue";
    static final String READ_AT_KEY = "bankValueReadAt";

    @Inject
    private ItemManager itemManager;

    @Inject
    private ConfigManager configManager;

    // The bank interface is open
    private boolean open;
    // The bank's items arrived since it opened
    private boolean loaded;
    private int ticksOpen;
    // The value was read during this visit
    private boolean readThisVisit;
    private long lastRead = -1;

    public void onWidgetLoaded(int groupId) {
        if (groupId != InterfaceID.BANKMAIN) return;
        open = true;
        loaded = false;
        ticksOpen = 0;
        readThisVisit = false;
    }

    public void onItemContainerChanged(int containerId) {
        if (containerId == InventoryID.BANK && open) loaded = true;
    }

    // Reads the value once the items are there, or after two ticks at the latest
    public void onTick() {
        if (!open || readThisVisit) return;
        ticksOpen++;
        if (!loaded && ticksOpen < 2) return;
        Long value = readValue();
        if (value == null) return;
        update(value);
        readThisVisit = true;
    }

    public void onWidgetClosed(int groupId) {
        if (groupId != InterfaceID.BANKMAIN || !open) return;
        open = false;
        if (!readThisVisit) return;
        Long value = readValue();
        if (value != null && value != lastRead) update(value);
    }

    /** After logging in: puts the account's last reading back on the player, if there is one. */
    public void restore() {
        if (!config.includeBankValue()) return;
        Long value = configManager.getRSProfileConfiguration(CONFIG_GROUP, VALUE_KEY, Long.class);
        Long readAt = configManager.getRSProfileConfiguration(CONFIG_GROUP, READ_AT_KEY, Long.class);
        if (value == null || readAt == null) return;
        messageBuilder.setData("bank", new BankValue(value, readAt));
        lastRead = value;
    }

    public void reset() {
        open = false;
        loaded = false;
        readThisVisit = false;
        lastRead = -1;
    }

    private void update(long value) {
        if (!config.includeBankValue()) return;
        long readAt = System.currentTimeMillis();
        messageBuilder.setData("bank", new BankValue(value, readAt));
        configManager.setRSProfileConfiguration(CONFIG_GROUP, VALUE_KEY, value);
        configManager.setRSProfileConfiguration(CONFIG_GROUP, READ_AT_KEY, readAt);
        lastRead = value;
    }

    private Long readValue() {
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bank == null) return null;
        return bankValue(bank.getItems(), itemManager);
    }

    @VisibleForTesting
    static long bankValue(Item[] items, ItemManager itemManager) {
        long total = 0;
        for (Item item : items) {
            if (item.getId() <= 0 || item.getQuantity() <= 0) continue;
            total += (long) itemManager.getItemPrice(item.getId()) * item.getQuantity();
        }
        return total;
    }
}
