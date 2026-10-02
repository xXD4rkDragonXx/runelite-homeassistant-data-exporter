package haexporterplugin.notifiers;

import haexporterplugin.data.PlayerLocation;
import haexporterplugin.data.TrailPoint;
import haexporterplugin.events.TeleportEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public class LocationNotifier extends BaseNotifier{
    // Running covers 2 tiles per tick; moving further than this in one tick is a jump (teleport, cave entrance, ...)
    private static final int TELEPORT_DISTANCE = 5;
    // A boat is tracked by its centre and can outrun a player on foot, so moves involving one get a wider margin
    private static final int BOAT_TELEPORT_DISTANCE = 20;

    // Where the player was on the previous tick; null until the first tick after a reset
    private WorldPoint previousPoint;
    private boolean previousOnBoat;

    public void onTick()
    {
        Actor player = client.getLocalPlayer();
        LocalPoint localPoint = player.getLocalLocation();
        WorldView worldView = player.getWorldView();
        int worldViewId = worldView.getId();
        boolean isOnBoat = worldViewId != WorldView.TOPLEVEL;
        WorldPoint worldPoint;
        if (isOnBoat) {
            WorldEntity worldEntity = client.getTopLevelWorldView().worldEntities().byIndex(worldViewId);
            worldPoint = WorldPoint.fromLocalInstance(client, worldEntity.getLocalLocation());
        } else {
            worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
        }

        recordLocation(worldPoint, isOnBoat);
    }

    void recordLocation(WorldPoint worldPoint, boolean isOnBoat)
    {
        PlayerLocation playerLocation = new PlayerLocation(worldPoint, isOnBoat);

        messageBuilder.setData("location", playerLocation);

        if (!config.includeLocation()) {
            // Forget the previous tile, so switching location sharing back on isn't reported as a jump
            reset();
            return;
        }

        WorldPoint previous = previousPoint;
        boolean wasOnBoat = previousOnBoat;
        previousPoint = worldPoint;
        previousOnBoat = isOnBoat;

        if (worldPoint.equals(previous) && isOnBoat == wasOnBoat) {
            return;
        }

        boolean teleport = previous != null && isTeleport(previous, wasOnBoat, worldPoint, isOnBoat);
        messageBuilder.addLocationTrailPoint(new TrailPoint(worldPoint, isOnBoat, System.currentTimeMillis(), teleport));

        if (teleport) {
            messageBuilder.addEvent("teleport", new TeleportEvent(new PlayerLocation(previous, wasOnBoat), playerLocation));
            tickUtils.sendNow();
        }
    }

    // Called when the player logs out or hops, so the first tile afterwards isn't compared to the old one
    public void reset()
    {
        previousPoint = null;
        previousOnBoat = false;
    }

    private static boolean isTeleport(WorldPoint from, boolean fromBoat, WorldPoint to, boolean toBoat)
    {
        int distance = Math.max(Math.abs(to.getX() - from.getX()), Math.abs(to.getY() - from.getY()));
        int limit = fromBoat || toBoat ? BOAT_TELEPORT_DISTANCE : TELEPORT_DISTANCE;
        return distance > limit;
    }
}
