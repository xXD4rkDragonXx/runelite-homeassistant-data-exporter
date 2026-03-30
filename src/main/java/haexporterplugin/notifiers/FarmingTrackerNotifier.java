package haexporterplugin.notifiers;

import haexporterplugin.events.BirdhouseEvent;
import haexporterplugin.events.FarmingEvent;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.*;

@Slf4j
@Singleton
public class FarmingTrackerNotifier extends BaseNotifier {

    // Farming patches - map of varbit to patch name and growth time (in minutes)
    private static final Map<Integer, PatchInfo> FARMING_PATCHES = new LinkedHashMap<>();
    
    // Birdhouse data - map of varbit to house name and growth time (in minutes)
    private static final Map<Integer, BirdhouseInfo> BIRDHOUSES = new LinkedHashMap<>();

    static {
        // Farming patches and their growth times
        // Herb patches (80 minute growth time)
        FARMING_PATCHES.put(8395, new PatchInfo("The Troll Stronghold", 80));
        FARMING_PATCHES.put(8396, new PatchInfo("East Ardougne", 80));
        FARMING_PATCHES.put(8397, new PatchInfo("Morytania", 80));
        FARMING_PATCHES.put(23968, new PatchInfo("Hosidious", 80));

        // Allotment patches (160 minute growth time - 8 growth cycles)
        FARMING_PATCHES.put(8391, new PatchInfo("portalphasmatusportkey.Payment", 160));
        FARMING_PATCHES.put(8392, new PatchInfo("Varrock", 160));
        FARMING_PATCHES.put(8393, new PatchInfo("Morytania", 160));
        FARMING_PATCHES.put(8394, new PatchInfo("Hosidious", 160));

        // Flower patches (80 minute growth time)
        FARMING_PATCHES.put(8402, new PatchInfo("Falador Park", 80));
        FARMING_PATCHES.put(8403, new PatchInfo("Lumbridge", 80));
        FARMING_PATCHES.put(8404, new PatchInfo("Ardougne", 80));
        FARMING_PATCHES.put(8405, new PatchInfo("Morytania", 80));

        // Fruit tree patches (960 minute = 12 hour growth time)
        FARMING_PATCHES.put(8408, new PatchInfo("Ardougne", 960));
        FARMING_PATCHES.put(8409, new PatchInfo("Gnome Stronghold", 960));
        FARMING_PATCHES.put(8410, new PatchInfo("Morytania", 960));
        FARMING_PATCHES.put(23969, new PatchInfo("Hosidious", 960));

        // Bush patches (320 minute = 4 hour growth time)
        FARMING_PATCHES.put(8411, new PatchInfo("Falador Park", 320));
        FARMING_PATCHES.put(8412, new PatchInfo("Ardougne", 320));
        FARMING_PATCHES.put(8413, new PatchInfo("Morytania", 320));
        FARMING_PATCHES.put(23970, new PatchInfo("Hosidious", 320));

        // Cactus patches (160 minute growth time)
        FARMING_PATCHES.put(8399, new PatchInfo("Al Kharid", 160));
        FARMING_PATCHES.put(8400, new PatchInfo("Morytania", 160));
        FARMING_PATCHES.put(23967, new PatchInfo("Hosidious", 160));

        // Hops patches (80 minute growth time)
        FARMING_PATCHES.put(8414, new PatchInfo("Lumbridge", 80));
        FARMING_PATCHES.put(8415, new PatchInfo("Wildblood", 80));
        FARMING_PATCHES.put(8416, new PatchInfo("Morytania", 80));
        FARMING_PATCHES.put(23971, new PatchInfo("Hosidious", 80));

        // Birdhouses (60 minute growth time)
        BIRDHOUSES.put(8401, new BirdhouseInfo("North Ardougne", 60));
        BIRDHOUSES.put(8406, new BirdhouseInfo("South Ardougne", 60));
        BIRDHOUSES.put(8417, new BirdhouseInfo("Yanille", 60));
        BIRDHOUSES.put(23972, new BirdhouseInfo("Hosidious", 60));
    }

    private static class PatchInfo {
        String name;
        int growthTimeMinutes;

        PatchInfo(String name, int growthTimeMinutes) {
            this.name = name;
            this.growthTimeMinutes = growthTimeMinutes;
        }
    }

    private static class BirdhouseInfo {
        String name;
        int growthTimeMinutes;

        BirdhouseInfo(String name, int growthTimeMinutes) {
            this.name = name;
            this.growthTimeMinutes = growthTimeMinutes;
        }
    }

    // Track state to detect changes
    private Map<Integer, Integer> previousFarmingState = new HashMap<>();
    private Map<Integer, Integer> previousBirdhouseState = new HashMap<>();
    private Map<Integer, Integer> farmingPlantedTick = new HashMap<>();  // varbit -> tick count when planted
    private Map<Integer, Integer> birdhousePlantedTick = new HashMap<>();  // varbit -> tick count when planted

    public void onTick() {
        checkFarmingPatches();
        checkBirdhouses();
    }

    private void checkFarmingPatches() {
        int currentTick = tickUtils.getTickCount();

        for (Map.Entry<Integer, PatchInfo> entry : FARMING_PATCHES.entrySet()) {
            int varbit = entry.getKey();
            PatchInfo patchInfo = entry.getValue();

            int currentState = client.getVarbitValue(varbit);
            int previousState = previousFarmingState.getOrDefault(varbit, -1);

            previousFarmingState.put(varbit, currentState);

            // State 0 = empty, anything else = something planted
            if (previousState == 0 && currentState != 0) {
                // Crop was just planted
                long currentTime = System.currentTimeMillis();
                long doneTime = currentTime + (patchInfo.growthTimeMinutes * 60 * 1000L);
                String cropName = getCropNameFromState(varbit, currentState);

                farmingPlantedTick.put(varbit, currentTick);
                
                FarmingEvent event = new FarmingEvent(patchInfo.name, cropName, currentTime, doneTime);
                messageBuilder.addEvent("farmingPlanted", event);
                tickUtils.sendNow();

                log.info("Farming crop planted at {}: {} (tick: {})", patchInfo.name, cropName, currentTick);
            }

            // Detect when crop is ready (state changed to harvested state)
            if (previousState != currentState && currentState == 0 && farmingPlantedTick.containsKey(varbit)) {
                // Crop is now ready to harvest
                int plantedTick = farmingPlantedTick.remove(varbit);
                int ticksElapsed = currentTick - plantedTick;
                
                messageBuilder.addEvent("farmingReady", patchInfo.name);
                tickUtils.sendNow();

                log.info("Farming crop ready at {} (took {} ticks, expected ~{})", 
                    patchInfo.name, ticksElapsed, patchInfo.growthTimeMinutes * 100);
            }
        }
    }

    private void checkBirdhouses() {
        int currentTick = tickUtils.getTickCount();

        for (Map.Entry<Integer, BirdhouseInfo> entry : BIRDHOUSES.entrySet()) {
            int varbit = entry.getKey();
            BirdhouseInfo houseInfo = entry.getValue();

            int currentState = client.getVarbitValue(varbit);
            int previousState = previousBirdhouseState.getOrDefault(varbit, -1);

            previousBirdhouseState.put(varbit, currentState);

            // State transitions: 0 = empty, 1-3 = growing, 4 = ready
            if (previousState == 0 && currentState != 0) {
                // Birdhouse was just seeded
                long currentTime = System.currentTimeMillis();
                long doneTime = currentTime + (houseInfo.growthTimeMinutes * 60 * 1000L);
                String seedName = getSeedNameFromState(currentState);

                birdhousePlantedTick.put(varbit, currentTick);
                
                BirdhouseEvent event = new BirdhouseEvent(houseInfo.name, seedName, currentTime, doneTime);
                messageBuilder.addEvent("birdhouseSeeded", event);
                tickUtils.sendNow();

                log.info("Birdhouse seeded at {}: {} (tick: {})", houseInfo.name, seedName, currentTick);
            }

            // Detect when birdhouse is ready (state = 4 or similar)
            if (previousState != 0 && currentState == 4 && birdhousePlantedTick.containsKey(varbit)) {
                // Birdhouse is now ready
                int plantedTick = birdhousePlantedTick.remove(varbit);
                int ticksElapsed = currentTick - plantedTick;
                
                messageBuilder.addEvent("birdhouseReady", houseInfo.name);
                tickUtils.sendNow();

                log.info("Birdhouse ready at {} (took {} ticks, expected ~{})", 
                    houseInfo.name, ticksElapsed, houseInfo.growthTimeMinutes * 100);
            }
        }
    }

    private String getCropNameFromState(int varbit, int state) {
        // This is a simple implementation - you may need to expand this based on actual state values
        return "Unknown Crop (State: " + state + ")";
    }

    private String getSeedNameFromState(int state) {
        // This is a simple implementation - you may need to expand this based on actual state values
        return "Unknown Seed (State: " + state + ")";
    }
}
