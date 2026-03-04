package haexporterplugin.notifiers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.util.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class AchievementDiaryNotifier extends BaseNotifier {

    private static final Pattern DIARY_PATTERN = Pattern.compile(
            "Well done! You have completed an? (?<tier>easy|medium|hard|elite) task in the (?<region>.+?) area\\. Your Achievement Diary has been updated\\.",
            Pattern.CASE_INSENSITIVE
    );

    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE) return;

        String msg = Text.removeTags(event.getMessage());
        Matcher matcher = DIARY_PATTERN.matcher(msg);

        if (matcher.matches()) {
            String region = matcher.group("region");
            String tier = matcher.group("tier");

            log.debug("Detected diary completion: {} (tier: {})", region, tier);

            Map<String, Object> thisEvent = new HashMap<>();
            thisEvent.put("region", region);
            thisEvent.put("tier", tier);
            messageBuilder.addEvent("achievementDiary", thisEvent);
        }
    }
}
