package haexporterplugin.notifiers;

import haexporterplugin.data.PlayerLocation;
import haexporterplugin.data.TrailPoint;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public class LocationNotifier extends BaseNotifier{
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
            // Forget the previous tile, so the trail starts with the current one when sharing is switched back on
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

        // Teleports aren't marked: receivers see two points that are far apart and decide for themselves
        messageBuilder.addLocationTrailPoint(new TrailPoint(worldPoint, isOnBoat, System.currentTimeMillis()));
    }

    // Called when the player logs out or hops, so the first tile afterwards is always recorded
    public void reset()
    {
        previousPoint = null;
        previousOnBoat = false;
    }
}
