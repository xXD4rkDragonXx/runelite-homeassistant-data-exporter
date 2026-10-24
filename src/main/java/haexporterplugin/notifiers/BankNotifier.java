package haexporterplugin.notifiers;

import com.google.common.annotations.VisibleForTesting;
import haexporterplugin.events.BankValueEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Sends a "bankValue" event with the GE value of the bank: once when the bank is opened, and again
 * when it is closed if the value changed meanwhile (so withdrawn gear isn't counted twice, once in the
 * bank and once in the inventory). Everything runs on the client thread from RuneLite events and is
 * cheap: one price lookup (a cache read) per bank slot, at most twice per visit. Sending is the
 * regular asynchronous delivery of the next update.
 */
@Slf4j
@Singleton
public class BankNotifier extends BaseNotifier {

    @Inject
    private ItemManager itemManager;

    // The bank interface is open
    private boolean open;
    // The server sent the bank's items since it was opened
    private boolean loaded;
    // Ticks since the bank was opened, for a client that already had the items
    private int ticksOpen;
    // A value was sent during this visit
    private boolean sentThisVisit;
    // The last value sent, -1 for none
    private long lastSent = -1;

    public void onWidgetLoaded(int groupId) {
        if (groupId != InterfaceID.BANKMAIN) return;
        open = true;
        loaded = false;
        ticksOpen = 0;
        sentThisVisit = false;
    }

    public void onItemContainerChanged(int containerId) {
        if (containerId == InventoryID.BANK && open) loaded = true;
    }

    public void onTick() {
        if (!open || sentThisVisit) return;
        ticksOpen++;
        // The server sends the items with the interface; two ticks covers a client that had them already
        if (!loaded && ticksOpen < 2) return;
        Long value = readValue();
        if (value == null) return;
        send(value);
        sentThisVisit = true;
    }

    public void onWidgetClosed(int groupId) {
        if (groupId != InterfaceID.BANKMAIN || !open) return;
        open = false;
        if (!sentThisVisit) return;
        Long value = readValue();
        if (value != null && value != lastSent) send(value);
    }

    // Forget the visit (logout, world hop): the next bank opening sends again
    public void reset() {
        open = false;
        loaded = false;
        sentThisVisit = false;
        lastSent = -1;
    }

    private void send(long value) {
        if (!config.includeBankValue()) return;
        messageBuilder.addEvent("bankValue", new BankValueEvent(value));
        lastSent = value;
    }

    private Long readValue() {
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bank == null) return null;
        return bankValue(bank.getItems(), itemManager);
    }

    /** Σ GE price × quantity over the bank's items. Placeholders have quantity 0 and add nothing. */
    @VisibleForTesting
    static long bankValue(Item[] items, ItemManager itemManager) {
        long total = 0;
        for (Item item : items) {
            if (item.getId() <= 0 || item.getQuantity() <= 0) continue;
            total += itemManager.getItemPrice(item.getId()) * item.getQuantity();
        }
        return total;
    }
}
