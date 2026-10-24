package haexporterplugin.data;

import lombok.Getter;

/**
 * The GE value of everything in the bank as it was last seen, and when (epoch milliseconds). Sent with
 * every message once known, updated each time the bank is opened, and again on closing it if it changed.
 * Bank + inventory + equipment is what a player owns.
 */
@Getter
public class BankValue {
    private final long value;
    private final long readAt;

    public BankValue(long value, long readAt) {
        this.value = value;
        this.readAt = readAt;
    }
}
