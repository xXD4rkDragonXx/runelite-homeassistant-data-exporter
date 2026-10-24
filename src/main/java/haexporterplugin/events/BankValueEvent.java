package haexporterplugin.events;

import lombok.Getter;

/**
 * The GE value of everything in the bank, read when the bank is opened and again when it is closed
 * (if it changed). Bank + inventory + equipment is what a player owns.
 */
@Getter
public class BankValueEvent implements HAExporterEvent {
    private final long value;

    public BankValueEvent(long value) {
        this.value = value;
    }
}
