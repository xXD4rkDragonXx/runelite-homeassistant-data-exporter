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
public class CombatTaskNotifier extends BaseNotifier {

    private static final Pattern COMBAT_TASK_PATTERN = Pattern.compile(
            "Congratulations, you've completed an? (?<tier>easy|medium|hard|elite|master|grandmaster) combat task:(?<task>.+?)",
            Pattern.CASE_INSENSITIVE
    );

    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE) return;

        String msg = Text.removeTags(event.getMessage());
        Matcher matcher = COMBAT_TASK_PATTERN.matcher(msg);

        if (matcher.matches()) {
            String task = matcher.group("task");
            String tier = matcher.group("tier");

            log.debug("Detected combat task completion: {} (tier: {})", task, tier);

            Map<String, Object> thisEvent = new HashMap<>();
            thisEvent.put("taskName", task);
            thisEvent.put("tier", tier);
            messageBuilder.addEvent("combatTask", thisEvent);
        }
    }

}
