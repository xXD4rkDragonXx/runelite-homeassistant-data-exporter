package haexporterplugin.events;

import net.runelite.api.coords.WorldPoint;

public class SuperiorEvent implements HAExporterEvent {
    private final String name;
    private final int npcId;
    private final WorldPoint location;

    public SuperiorEvent(String name, int npcId, WorldPoint location) {
        this.name = name;
        this.npcId = npcId;
        this.location = location;
    }
}
