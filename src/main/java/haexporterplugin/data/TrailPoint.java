package haexporterplugin.data;

import net.runelite.api.coords.WorldPoint;

/**
 * One tile the player stood on, as sent in the location trail.
 */
public class TrailPoint {

    private final int x;

    private final int y;

    private final int plane;

    private final boolean isOnBoat;

    // Epoch millis (UTC) at which the player was seen on this tile
    private final long timestamp;

    // True when the player jumped here (teleport, cave entrance, ...) instead of walking
    private final boolean teleport;

    public TrailPoint(WorldPoint worldPoint, boolean isOnBoat, long timestamp, boolean teleport) {
        this.x = worldPoint.getX();
        this.y = worldPoint.getY();
        this.plane = worldPoint.getPlane();
        this.isOnBoat = isOnBoat;
        this.timestamp = timestamp;
        this.teleport = teleport;
    }

}
