package haexporterplugin.data;

import net.runelite.api.coords.WorldPoint;

public class PlayerLocation {

    /**
     * X-axis coordinate.
     */
    private final int x;

    /**
     * Y-axis coordinate.
     */
    private final int y;

    /**
     * The plane level of the Tile, also referred as z-axis coordinate.
     */
    private final int plane;

    private final boolean isOnBoat;

    public PlayerLocation(int x, int y, int plane, boolean isOnBoat) {
        this.x = x;
        this.y = y;
        this.plane = plane;
        this.isOnBoat = isOnBoat;
    }

    public PlayerLocation(WorldPoint worldPoint, boolean isOnBoat) {
        this.x = worldPoint.getX();
        this.y = worldPoint.getY();
        this.plane = worldPoint.getPlane();
        this.isOnBoat = isOnBoat;
    }

}
