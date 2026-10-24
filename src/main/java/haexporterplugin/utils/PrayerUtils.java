package haexporterplugin.utils;

import lombok.experimental.UtilityClass;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.VarbitID;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PrayerUtils {

    public List<String> getActivePrayers(Client client)
    {
        // Deadeye and Mystic Vigour replace Eagle Eye and Mystic Might once unlocked, except in LMS.
        // Same rule as the core Prayer plugin; Client#isPrayerActive does not handle this.
        boolean inLms = client.getVarbitValue(VarbitID.BR_INGAME) != 0;
        boolean deadeye = !inLms && client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) != 0;
        boolean mysticVigour = !inLms && client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) != 0;
        // Zeal replaces Piety once unlocked and is always used in LMS (inverse of the above).
        boolean zeal = inLms || client.getVarbitValue(VarbitID.PRAYER_ZEAL_UNLOCKED) != 0;

        List<String> activePrayers = new ArrayList<>();
        for (Prayer prayer : Prayer.values())
        {
            // Zeal is handled below via its varbit, since Prayer.ZEAL is not in every RuneLite release yet.
            if (prayer.getVarbit() == VarbitID.PRAYER_ZEAL || client.getVarbitValue(prayer.getVarbit()) == 0)
            {
                continue;
            }

            if ((prayer == Prayer.EAGLE_EYE && deadeye)
                    || (prayer == Prayer.DEADEYE && !deadeye)
                    || (prayer == Prayer.MYSTIC_MIGHT && mysticVigour)
                    || (prayer == Prayer.MYSTIC_VIGOUR && !mysticVigour)
                    || (prayer == Prayer.PIETY && zeal))
            {
                continue;
            }

            activePrayers.add(prayer.name());
        }

        if (zeal && client.getVarbitValue(VarbitID.PRAYER_ZEAL) != 0)
        {
            activePrayers.add("ZEAL");
        }
        return activePrayers;
    }
}