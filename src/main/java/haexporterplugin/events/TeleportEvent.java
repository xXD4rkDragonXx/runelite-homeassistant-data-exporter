package haexporterplugin.events;

import haexporterplugin.data.PlayerLocation;

public class TeleportEvent implements HAExporterEvent {
    private final PlayerLocation from;
    private final PlayerLocation to;

    public TeleportEvent(PlayerLocation from, PlayerLocation to) {
        this.from = from;
        this.to = to;
    }
}
