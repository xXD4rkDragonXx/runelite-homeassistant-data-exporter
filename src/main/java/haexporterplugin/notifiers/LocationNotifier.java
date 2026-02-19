package haexporterplugin.notifiers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public class LocationNotifier extends BaseNotifier{
    public void onTick()
    {
        WorldPoint location = client.getLocalPlayer().getWorldLocation();
        messageBuilder.setData("location", location);
    }
}
