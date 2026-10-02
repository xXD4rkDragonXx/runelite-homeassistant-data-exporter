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
    private WorldPoint previousScenePoint;
    private boolean previousOnBoat;

    public void onTick()
    {
        Actor player = client.getLocalPlayer();
        LocalPoint localPoint = player.getLocalLocation();
        WorldView worldView = player.getWorldView();
        int worldViewId = worldView.getId();
        boolean isOnBoat = worldViewId != WorldView.TOPLEVEL;
        if (isOnBoat) {
            WorldEntity worldEntity = client.getTopLevelWorldView().worldEntities().byIndex(worldViewId);
            localPoint = worldEntity.getLocalLocation();
        }

        WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
        // Inside an instance (a house, a raid) worldPoint is the tile of the template the room was copied from,
        // which jumps when walking from one room to the next. The tile in the scene doesn't.
        WorldPoint scenePoint = WorldPoint.fromLocal(client, localPoint);

        recordLocation(worldPoint, scenePoint, isOnBoat);
    }

    void recordLocation(WorldPoint worldPoint, WorldPoint scenePoint, boolean isOnBoat)
    {
        PlayerLocation playerLocation = new PlayerLocation(worldPoint, isOnBoat);

        messageBuilder.setData("location", playerLocation);

        if (!config.includeLocation()) {
            // Forget the previous tile, so switching location sharing back on isn't reported as a jump
            reset();
            return;
        }

        WorldPoint previous = previousPoint;
        WorldPoint previousScene = previousScenePoint;
        boolean wasOnBoat = previousOnBoat;
        previousPoint = worldPoint;
        previousScenePoint = scenePoint;
        previousOnBoat = isOnBoat;

        boolean boat = wasOnBoat || isOnBoat;
        boolean teleport = previous != null && isJump(previousScene, scenePoint, boat);

        if (worldPoint.equals(previous) && isOnBoat == wasOnBoat && !teleport) {
            return;
        }

        // Walking between rooms of an instance is no teleport, but the reported tile still jumps, so the trail breaks there
        boolean disconnected = teleport || (previous != null && isJump(previous, worldPoint, boat));
        messageBuilder.addLocationTrailPoint(new TrailPoint(worldPoint, isOnBoat, System.currentTimeMillis(), disconnected));

        if (teleport) {
            messageBuilder.addEvent("teleport", new TeleportEvent(new PlayerLocation(previous, wasOnBoat), playerLocation));
            tickUtils.sendNow();
        }
    }

    // Called when the player logs out or hops, so the first tile afterwards isn't compared to the old one
    public void reset()
    {
        previousPoint = null;
        previousScenePoint = null;
        previousOnBoat = false;
    }

    private static boolean isJump(WorldPoint from, WorldPoint to, boolean boat)
    {
        int distance = Math.max(Math.abs(to.getX() - from.getX()), Math.abs(to.getY() - from.getY()));
        int limit = boat ? BOAT_TELEPORT_DISTANCE : TELEPORT_DISTANCE;
        return distance > limit;
    }
}
