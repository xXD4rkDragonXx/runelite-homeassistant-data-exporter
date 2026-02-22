package haexporterplugin.utils;

import com.google.gson.Gson;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RarityUtils extends AbstractRarityUtils {
    @Inject
    RarityUtils(Gson gson, ItemManager itemManager) {
        super("/npc_drops.json", 1024, gson, itemManager);
    }
}