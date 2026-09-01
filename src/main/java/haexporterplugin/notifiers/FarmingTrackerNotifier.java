package haexporterplugin.notifiers;

import haexporterplugin.events.BirdhouseEvent;
import haexporterplugin.events.FarmingEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Slf4j
@Singleton
public class FarmingTrackerNotifier extends BaseNotifier {

    @Inject
    private ConfigManager configManager;

    private static class PatchInfo {
        final int varbit;
        final String name;
        final int growthMinutes;
        final boolean isBirdhouse;

        PatchInfo(int varbit, String name, int growthMinutes, boolean isBirdhouse) {
            this.varbit = varbit;
            this.name = name;
            this.growthMinutes = growthMinutes;
            this.isBirdhouse = isBirdhouse;
        }
    }

    // All patches with complete info - maps varbit to patch details
    private static final Map<Integer, PatchInfo> PATCHES = new LinkedHashMap<>();

    // Cache for region lookups to avoid repeated searches
    private final Map<Integer, String> regionCache = new HashMap<>();

    // Track previous states to detect transitions
    private final Map<Integer, String> previousStates = new HashMap<>();

    static {
        // Herb patches (80 minutes)
        PATCHES.put(8395, new PatchInfo(8395, "The Troll Stronghold", 80, false));
        PATCHES.put(8396, new PatchInfo(8396, "East Ardougne", 80, false));
        PATCHES.put(8397, new PatchInfo(8397, "Morytania", 80, false));
        PATCHES.put(23968, new PatchInfo(23968, "Hosidious", 80, false));

        // Allotment patches (160 minutes)
        PATCHES.put(8391, new PatchInfo(8391, "Lumbridge", 160, false));
        PATCHES.put(8392, new PatchInfo(8392, "Varrock", 160, false));
        PATCHES.put(8393, new PatchInfo(8393, "Morytania", 160, false));
        PATCHES.put(8394, new PatchInfo(8394, "Hosidious", 160, false));

        // Flower patches (80 minutes)
        PATCHES.put(8402, new PatchInfo(8402, "Falador Park", 80, false));
        PATCHES.put(8403, new PatchInfo(8403, "Lumbridge", 80, false));
        PATCHES.put(8404, new PatchInfo(8404, "Ardougne", 80, false));
        PATCHES.put(8405, new PatchInfo(8405, "Morytania", 80, false));

        // Fruit tree patches (960 minutes)
        PATCHES.put(8408, new PatchInfo(8408, "Ardougne", 960, false));
        PATCHES.put(8409, new PatchInfo(8409, "Gnome Stronghold", 960, false));
        PATCHES.put(8410, new PatchInfo(8410, "Morytania", 960, false));
        PATCHES.put(23969, new PatchInfo(23969, "Hosidious", 960, false));

        // Bush patches (320 minutes)
        PATCHES.put(8411, new PatchInfo(8411, "Falador Park", 320, false));
        PATCHES.put(8412, new PatchInfo(8412, "Ardougne", 320, false));
        PATCHES.put(8413, new PatchInfo(8413, "Morytania", 320, false));
        PATCHES.put(23970, new PatchInfo(23970, "Hosidious", 320, false));

        // Cactus patches (160 minutes)
        PATCHES.put(8399, new PatchInfo(8399, "Al Kharid", 160, false));
        PATCHES.put(8400, new PatchInfo(8400, "Morytania", 160, false));
        PATCHES.put(23967, new PatchInfo(23967, "Hosidious", 160, false));

        // Hops patches (80 minutes)
        PATCHES.put(8414, new PatchInfo(8414, "Lumbridge", 80, false));
        PATCHES.put(8415, new PatchInfo(8415, "Wildblood", 80, false));
        PATCHES.put(8416, new PatchInfo(8416, "Morytania", 80, false));
        PATCHES.put(23971, new PatchInfo(23971, "Hosidious", 80, false));

        // Birdhouses (60 minutes)
        PATCHES.put(8401, new PatchInfo(8401, "North Ardougne", 60, true));
        PATCHES.put(8406, new PatchInfo(8406, "South Ardougne", 60, true));
        PATCHES.put(8417, new PatchInfo(8417, "Yanille", 60, true));
        PATCHES.put(23972, new PatchInfo(23972, "Hosidious", 60, true));
    }

    public void onTick() {
        checkFarmingTracking();
    }

    private void checkFarmingTracking() {
        for (PatchInfo patch : PATCHES.values()) {
            String configKey = getCachedConfigKey(patch.varbit);
            if (configKey == null) {
                continue;
            }

            String storedData = configManager.getRSProfileConfiguration("timetracking", configKey);
            if (storedData == null || storedData.isEmpty()) {
                previousStates.remove(patch.varbit);
                continue;
            }

            String[] parts = storedData.split(":");
            if (parts.length < 1) {
                continue;
            }

            int varbitValue = Integer.parseInt(parts[0]);
            String currentState = getCropState(varbitValue);
            String previousState = previousStates.getOrDefault(patch.varbit, "EMPTY");

            log.debug("CHECKING PLANTSS");
            // Detect state transitions
            if ("EMPTY".equals(previousState) && ("GROWING".equals(currentState) || "SEEDED".equals(currentState))) {
                sendPlantedEvent(patch);
            }

            if (!"HARVESTABLE".equals(previousState) && "HARVESTABLE".equals(currentState)) {
                sendReadyEvent(patch);
            }

            if (("GROWING".equals(previousState) || "SEEDED".equals(previousState)) && 
                ("DEAD".equals(currentState) || "DISEASED".equals(currentState))) {
                sendFailedEvent(patch);
            }

            previousStates.put(patch.varbit, currentState);
        }
    }

    /**
     * Get cached config key, or look it up if not cached
     */
    private String getCachedConfigKey(int varbit) {
        if (regionCache.containsKey(varbit)) {
            String cachedKey = regionCache.get(varbit);
            return "".equals(cachedKey) ? null : cachedKey;
        }

        String key = findConfigKey(varbit);
        regionCache.put(varbit, key == null ? "" : key);
        return key;
    }

    /**
     * Search for the config key by checking all common region IDs
     */
    private String findConfigKey(int varbit) {
        int[] commonRegions = {9782, 10547, 10548, 10549, 10550, 10551, 10552,
                              11161, 11406, 11407, 12850, 12851, 12852,
                              13104, 13349, 13613, 13614, 13615, 13616, 13617, 13618, 13619, 13620,
                              25109, 25110, 25111, 25112, 25113, 25114, 25115};

        for (int region : commonRegions) {
            String key = region + "." + varbit;
            String value = configManager.getRSProfileConfiguration("timetracking", key);
            if (value != null && !value.isEmpty()) {
                return key;
            }
        }

        return null;
    }

    private void sendPlantedEvent(PatchInfo patch) {
        long now = System.currentTimeMillis();
        long estimatedDone = now + (patch.growthMinutes * 60 * 1000L);
        String cropName = getCropName(patch);

        if (patch.isBirdhouse) {
            messageBuilder.addEvent("birdhouseSeeded", 
                new BirdhouseEvent(patch.name, cropName, now, estimatedDone));
        } else {
            messageBuilder.addEvent("farmingPlanted", 
                new FarmingEvent(patch.name, cropName, now, estimatedDone));
        }

        tickUtils.sendNow();
        log.info("Crop planted at {}: {} - Done at {}", patch.name, cropName, new Date(estimatedDone));
    }

    private void sendReadyEvent(PatchInfo patch) {
        if (patch.isBirdhouse) {
            messageBuilder.addEvent("birdhouseReady", patch.name);
        } else {
            messageBuilder.addEvent("farmingReady", patch.name);
        }
        tickUtils.sendNow();
        log.info("Crop ready at {}", patch.name);
    }

    private void sendFailedEvent(PatchInfo patch) {
        messageBuilder.addEvent("farmingFailed", patch.name);
        tickUtils.sendNow();
        log.warn("Crop failed at {}", patch.name);
    }

    private String getCropState(int varbitValue) {
        switch (varbitValue) {
            case 0:
                return "EMPTY";
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                return "GROWING";
            case 8:
                return "HARVESTABLE";
            default:
                return varbitValue > 100 ? "DISEASED" : "GROWING";
        }
    }

    private String getCropName(PatchInfo patch) {
        if (patch.name.contains("Herb")) return "Herb";
        if (patch.name.contains("Allotment")) return "Vegetable";
        if (patch.name.contains("Flower")) return "Flower";
        if (patch.name.contains("Fruit")) return "Fruit Tree";
        if (patch.name.contains("Bush")) return "Bush";
        if (patch.name.contains("Cactus")) return "Cactus";
        if (patch.name.contains("Hops")) return "Hops";
        if (patch.isBirdhouse) return "Bird seed";
        return "Crop";
    }
}
