package haexporterplugin.notifiers;

import com.google.common.annotations.VisibleForTesting;
import haexporterplugin.data.ItemData;
import haexporterplugin.enums.AccountType;
import haexporterplugin.enums.Danger;
import haexporterplugin.enums.ExceptionalDeath;
import haexporterplugin.events.DeathEvent;
import haexporterplugin.utils.ItemUtils;
import haexporterplugin.utils.Utils;
import haexporterplugin.utils.WorldUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ParamID;
import net.runelite.api.Player;
import net.runelite.api.SkullIcon;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Code based on DeathNotifier in Dink Plugin
 * source: <a href="https://github.com/pajlads/DinkPlugin/blob/master/src/main/java/dinkplugin/notifiers/DeathNotifier.java"></a>
 */

@Slf4j
@Singleton
public class DeathNotifier extends BaseNotifier {

    private static final String ATTACK_OPTION = "Attack";

    private static final String TOA_DEATH_MSG = "You failed to survive the Tombs of Amascut";

    private static final String TOB_DEATH_MSG = "Your party has failed";

    private static final String FORTIS_DOOM_MSG = "You have been doomed!";

    /**
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bclientscript,tob_hud_portal%5D.cs2">CS2 Reference</a>
     */
    private static final int TOB_HUB_PORTAL_SCRIPT = 2307;

    /**
     * Checks whether the actor is alive and interacting with the specified player.
     */
    private static final BiPredicate<Player, Actor> INTERACTING;

    /**
     * Checks whether a NPC is a valid candidate to be our killer.
     */
    private static final Predicate<NPCComposition> NPC_VALID;

    /**
     * Orders NPCs by their likelihood of being our killer.
     */
    private static final BiFunction<NPCManager, Player, Comparator<NPC>> NPC_COMPARATOR;

    /**
     * Orders actors by their likelihood of being the killer of the specified player.
     */
    private static final Function<Player, Comparator<Player>> PK_COMPARATOR;

    @Inject
    private ItemManager itemManager;

    @Inject
    private NPCManager npcManager;

    /**
     * Tracks the last {@link Actor} our local player interacted with,
     * for the purposes of attributing deaths to particular {@link Player}'s.
     * <p>
     * Note: this is wrapped in a weak reference to allow garbage collection,
     * for example if the {@link Actor} despawns.
     * As a result, the underlying reference can be null.
     *
     * @see #identifyKiller()
     */
    private WeakReference<Actor> lastTarget = new WeakReference<>(null);

    public void onActorDeath(ActorDeath actor) {
        boolean self = client.getLocalPlayer() == actor.getActor();

        if (self)
            handleNotify(null);

        if (self || actor.getActor() == lastTarget.get())
            lastTarget = new WeakReference<>(null);
    }

    public void onGameMessage(String message) {
        var player = client.getLocalPlayer();
        if (message.equals(FORTIS_DOOM_MSG) && player.getHealthRatio() > 0 && Objects.requireNonNull(WorldUtils.getLocation(client, player)).getRegionID() == WorldUtils.FORTIS_REGION) {
            handleNotify(Danger.DANGEROUS);
            return;
        }

        if (message.contains(TOA_DEATH_MSG)) {
            handleNotify(Danger.DANGEROUS);
        }
    }

    public void onScript(ScriptPreFired event) {
        if (event.getScriptId() == TOB_HUB_PORTAL_SCRIPT && event.getScriptEvent() != null) {
            Object[] args = event.getScriptEvent().getArguments();
            if (args != null && args.length > 1) {
                Object text = args[1];
                if (text instanceof String && ((String) text).contains(TOB_DEATH_MSG)) {
                    // https://oldschool.runescape.wiki/w/Theatre_of_Blood#Death_within_the_Theatre
                    handleNotify(Danger.DANGEROUS);
                }
            }
        }
    }

    public void onInteraction(InteractingChanged event) {
        if (event.getSource() == client.getLocalPlayer() && event.getTarget() != null && event.getTarget().getCombatLevel() > 0) {
            lastTarget = new WeakReference<>(event.getTarget());
        }
    }

    private void handleNotify(Danger dangerOverride) {
        int regionId = Objects.requireNonNull(WorldUtils.getLocation(client)).getRegionID();
        Danger danger = dangerOverride != null ? dangerOverride : WorldUtils.getDangerLevel(client, regionId, Set.of(ExceptionalDeath.values()));

        Collection<ItemData> items = ItemUtils.getInventoryItems(client, itemManager);
        List<ItemData> itemsByPrice = getPricedItems(items);

        Pair<List<ItemData>, List<ItemData>> split;
        if (danger == Danger.DANGEROUS) {
            int keepCount = getKeepCount();
            split = splitItemsByKept(itemsByPrice, keepCount);
        } else {
            split = Pair.of(itemsByPrice, Collections.emptyList());
        }
        List<ItemData> keptItems = split.getLeft();
        List<ItemData> lostItems = split.getRight();

        Integer losePrice = lostItems.stream()
                .mapToInt(pair -> pair.getGePrice() * pair.getQuantity())
                .sum();

        Actor killer = identifyKiller();
        boolean npc = killer instanceof NPC;
        String killerName = killer != null ? StringUtils.defaultIfEmpty(killer.getName(), "?") : null;

        List<ItemData> lostStacks = getStacks(lostItems, true);
        List<ItemData> keptStacks = getStacks(keptItems, false);

        DeathEvent deathEvent = new DeathEvent(
                losePrice,
                danger,
                killerName,
                npc ? ((NPC) killer).getId() : null,
                keptStacks,
                lostStacks,
                client.getLocalPlayer().getWorldLocation()
        );

        messageBuilder.addEvent("death", deathEvent);
        tickUtils.sendNow();
    }

    /**
     * @return the number of items the player would keep on an unsafe death
     */
    private int getKeepCount() {
        if (Utils.getAccountType(client) == AccountType.ULTIMATE_IRONMAN)
            return 0;

        var skull = client.getLocalPlayer().getSkullIcon();
        int keepCount = skull == SkullIcon.NONE ? 3 : 0;
        if (client.getVarbitValue(VarbitID.PRAYER_PROTECTITEM) == 1)
            keepCount++;
        return keepCount;
    }

    /**
     * @return the inferred {@link Actor} who killed us, or null if not killed by an external source
     */
    @Nullable
    private Actor identifyKiller() {
        // must be in unsafe wildness or pvp world to be pk'd
        boolean pvpEnabled = !WorldUtils.isPvpSafeZone(client) &&
                (client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) > 0 || WorldUtils.isPvpWorld(client.getWorldType()));

        Player localPlayer = client.getLocalPlayer();
        Predicate<Actor> interacting = a -> INTERACTING.test(localPlayer, a);

        // O(1) fast path based on last outbound interaction
        Actor lastTarget = this.lastTarget.get();
        if (checkLastInteraction(localPlayer, lastTarget, pvpEnabled))
            return lastTarget;

        // find another player interacting with us (that is preferably not a friend or clan member)
        if (pvpEnabled) {
            Optional<? extends Player> pker = client.getTopLevelWorldView().players().stream()
                    .filter(interacting)
                    .min(PK_COMPARATOR.apply(localPlayer)); // O(n)
            if (pker.isPresent())
                return pker.get();
        }

        // otherwise search through NPCs interacting with us
        return client.getTopLevelWorldView().npcs().stream()
                .filter(interacting)
                .filter(npc -> NPC_VALID.test(npc.getTransformedComposition()))
                .min(NPC_COMPARATOR.apply(npcManager, localPlayer)) // O(n)
                .orElse(null);
    }

    /**
     * @param localPlayer {@link net.runelite.api.Client#getLocalPlayer()}
     * @param actor       the {@link Actor} that is a candidate killer from {@link #lastTarget}
     * @param pvpEnabled  whether a player could be our killer (e.g., in wilderness)
     * @return whether the specified actor is the likely killer of the local player
     */
    private static boolean checkLastInteraction(Player localPlayer, Actor actor, boolean pvpEnabled) {
        if (!INTERACTING.test(localPlayer, actor))
            return false;

        if (actor instanceof Player) {
            Player other = (Player) actor;
            return pvpEnabled
                    && !other.isClanMember()
                    && !other.isFriend()
                    && !other.isFriendsChatMember();
        }

        if (actor instanceof NPC) {
            NPC npcActor = (NPC) actor;
            NPCComposition npc = npcActor.getTransformedComposition();

            if (!NPC_VALID.test(npc)) return false;
            assert npc != null;

            return ArrayUtils.contains(npc.getActions(), ATTACK_OPTION);
        }

        log.warn("Encountered unknown type of Actor; was neither Player nor NPC!");
        return false;
    }

    /**
     * @param items       the items whose prices should be queried
     * @return pairs of the passed items to their price, sorted by most expensive unit price first
     */
    private static List<ItemData> getPricedItems(Collection<ItemData> items) {
        return items.stream()
                .sorted(Comparator.comparingLong(ItemData::getGePrice).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Takes the complete list of items in the player's inventory and assigns them to separate lists,
     * depending on whether they would be kept or lost upon an unsafe death.
     *
     * @param itemsByPrice inventory items transformed by {@link #getPricedItems(Collection)}
     * @param keepCount    the number of items kept on death
     * @return the kept items on death (left) and lost items on death (right), in stable order, in separate lists
     */
    @VisibleForTesting
    static Pair<List<ItemData>, List<ItemData>> splitItemsByKept(List<ItemData> itemsByPrice, int keepCount) {
        List<ItemData> kept = new ArrayList<>();
        List<ItemData> lost = new ArrayList<>();

        int remainingKeeps = keepCount;

        for (ItemData item : itemsByPrice) {
            int id = item.getId();
            int quantity = item.getQuantity();

            // Bonds are always kept
            if (id == ItemID.OSRS_BOND
                    || id == ItemID.BOUGHT_OSRS_BOND
                    || id == ItemID.OSRS_BOND_UNTRADEABLE) {
                kept.add(item);
                continue;
            }

            boolean neverKept = ItemUtils.isItemNeverKeptOnDeath(id);

            if (neverKept || remainingKeeps <= 0) {
                lost.add(item);
                continue;
            }

            int toKeep = Math.min(quantity, remainingKeeps);
            int toLose = quantity - toKeep;

            if (toKeep > 0) {
                kept.add(new ItemData(item.getName(), id, item.getGePrice(), item.getHaPrice(), toKeep));
                remainingKeeps -= toKeep;
            }

            if (toLose > 0) {
                lost.add(new ItemData(item.getName(), id, item.getGePrice(), item.getHaPrice(), toLose));
            }
        }

        return Pair.of(kept, lost);
    }

    private static List<ItemData> getStacks(List<ItemData> items, boolean reduce) {
        if (!reduce) {
            // Preserve exact stacks
            return items.stream().map(ItemData::new).collect(Collectors.toList());
        }

        // Merge stacks with same ID
        Map<Integer, ItemData> merged = new LinkedHashMap<>();
        for (ItemData item : items) {
            merged.merge(item.getId(),
                    new ItemData(item), // copy
                    (existing, incoming) -> {
                        existing.setQuantity(existing.getQuantity() + incoming.getQuantity());
                        return existing;
                    });
        }

        return new ArrayList<>(merged.values());
    }

    static {
        INTERACTING = (localPlayer, a) -> a != null && a != localPlayer && !a.isDead() && a.getInteracting() == localPlayer;

        NPC_VALID = comp -> comp != null && comp.isInteractible() && !comp.isFollower() && comp.getCombatLevel() > 0;

        NPC_COMPARATOR = (npcManager, localPlayer) -> Comparator
                .comparing(
                        NPC::getTransformedComposition,
                        Comparator.nullsFirst(
                                Comparator
                                        .comparing(
                                                (NPCComposition comp) -> comp.getStringValue(ParamID.NPC_HP_NAME),
                                                Comparator.comparing(StringUtils::isNotEmpty) // prefer has name in hit points UI
                                        )
                                        .thenComparing(comp -> ArrayUtils.contains(comp.getActions(), ATTACK_OPTION)) // prefer explicitly attackable
                                        .thenComparingInt(NPCComposition::getCombatLevel) // prefer high level
                                        .thenComparingInt(NPCComposition::getSize) // prefer large
                                        .thenComparing(NPCComposition::isMinimapVisible) // prefer visible on minimap
                                        .thenComparing(
                                                // prefer high max health
                                                comp -> npcManager.getHealth(comp.getId()),
                                                Comparator.nullsFirst(Comparator.naturalOrder())
                                        )
                        )
                )
                .thenComparingInt(p -> -localPlayer.getLocalLocation().distanceTo(p.getLocalLocation())) // prefer nearby
                .reversed(); // for consistency with PK_COMPARATOR such that Stream#min should be used in #identifyKiller

        PK_COMPARATOR = localPlayer -> Comparator
                .comparing(Player::isClanMember) // prefer not in clan
                .thenComparing(Player::isFriend) // prefer not friend
                .thenComparing(Player::isFriendsChatMember) // prefer not fc
                .thenComparingInt(p -> Math.abs(localPlayer.getCombatLevel() - p.getCombatLevel())) // prefer similar level
                .thenComparingInt(p -> -p.getCombatLevel()) // prefer higher level for a given absolute level gap
                .thenComparing(p -> p.getOverheadIcon() == null) // prefer praying
                .thenComparing(p -> p.getTeam() == localPlayer.getTeam()) // prefer different team cape
                .thenComparingInt(p -> localPlayer.getLocalLocation().distanceTo(p.getLocalLocation())); // prefer nearby
    }
}