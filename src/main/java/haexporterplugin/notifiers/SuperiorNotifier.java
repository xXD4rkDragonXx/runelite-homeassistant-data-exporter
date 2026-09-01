package haexporterplugin.notifiers;

import haexporterplugin.events.SuperiorEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.util.Text;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SuperiorNotifier extends BaseNotifier {

    // Same signal RuneLite's own Slayer plugin uses; only appears at the real spawn moment.
    private static final String SUPERIOR_MESSAGE = "A superior foe has appeared...";

    private final List<NPC> spawnedThisTick = new ArrayList<>();
    private boolean superiorSeenThisTick = false;

    public void onNpcSpawned(NpcSpawned event) {
        spawnedThisTick.add(event.getNpc());
    }

    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE) return;
        if (Text.removeTags(event.getMessage()).contains(SUPERIOR_MESSAGE)) {
            superiorSeenThisTick = true;
        }
    }

    // Resolved on tick end so the message and the spawn are both collected regardless of event order.
    public void onGameTick() {
        if (superiorSeenThisTick) {
            NPC superior = nearestSpawnToPlayer();
            if (superior != null) {
                log.debug("Detected superior spawn: {} (id: {})", superior.getName(), superior.getId());
                messageBuilder.addEvent("superiorSpawn",
                        new SuperiorEvent(superior.getName(), superior.getId(), superior.getWorldLocation()));
                tickUtils.sendNow();
            } else {
                log.debug("Superior foe message seen but no NPC spawned this tick");
            }
        }
        spawnedThisTick.clear();
        superiorSeenThisTick = false;
    }

    // Superiors spawn adjacent to the player, replacing the monster just killed.
    private NPC nearestSpawnToPlayer() {
        Player local = client.getLocalPlayer();
        if (local == null) return null;
        WorldPoint playerLocation = local.getWorldLocation();
        if (playerLocation == null) return null;

        NPC nearest = null;
        int bestDistance = Integer.MAX_VALUE;
        for (NPC npc : spawnedThisTick) {
            WorldPoint location = npc.getWorldLocation();
            if (location == null) continue;
            int distance = location.distanceTo(playerLocation);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = npc;
            }
        }
        return nearest;
    }
}
