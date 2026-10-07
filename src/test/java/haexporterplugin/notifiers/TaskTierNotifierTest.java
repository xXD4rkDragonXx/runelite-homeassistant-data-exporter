package haexporterplugin.notifiers;

import com.google.inject.Injector;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.TestUtils;
import haexporterplugin.enums.CombatTaskTier;
import haexporterplugin.enums.DiaryTier;
import haexporterplugin.utils.MessageBuilder;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TaskTierNotifierTest
{
	private static final String HARD_DIARY = "Well done! You have completed a hard task in the Varrock area. Your Achievement Diary has been updated.";
	private static final String ELITE_DIARY = "Well done! You have completed an elite task in the Varrock area. Your Achievement Diary has been updated.";
	private static final String MEDIUM_TASK = "Congratulations, you've completed a medium combat task: Hitting Them Where It Hurts.";
	private static final String MASTER_TASK = "Congratulations, you've completed a master combat task: Perfect Zulrah.";

	private final HAExporterConfig config = mock(HAExporterConfig.class);
	private MessageBuilder messageBuilder;
	private AchievementDiaryNotifier diaryNotifier;
	private CombatTaskNotifier combatTaskNotifier;

	@Before
	public void setUp()
	{
		when(config.diaryMinTier()).thenReturn(DiaryTier.EASY);
		when(config.combatTaskMinTier()).thenReturn(CombatTaskTier.EASY);

		Injector injector = TestUtils.injector(Map.of(HAExporterConfig.class, config));
		messageBuilder = injector.getInstance(MessageBuilder.class);
		diaryNotifier = injector.getInstance(AchievementDiaryNotifier.class);
		combatTaskNotifier = injector.getInstance(CombatTaskNotifier.class);
	}

	@Test
	public void testDiaryMinimumTierSkipsEasierTasks()
	{
		diaryNotifier.onChatMessage(chat(HARD_DIARY));
		assertEquals(1, eventCount());

		when(config.diaryMinTier()).thenReturn(DiaryTier.ELITE);
		diaryNotifier.onChatMessage(chat(HARD_DIARY));
		assertEquals(1, eventCount());

		diaryNotifier.onChatMessage(chat(ELITE_DIARY));
		assertEquals(2, eventCount());
	}

	@Test
	public void testCombatTaskMinimumTierSkipsEasierTasks()
	{
		combatTaskNotifier.onChatMessage(chat(MEDIUM_TASK));
		assertEquals(1, eventCount());

		when(config.combatTaskMinTier()).thenReturn(CombatTaskTier.HARD);
		combatTaskNotifier.onChatMessage(chat(MEDIUM_TASK));
		assertEquals(1, eventCount());

		combatTaskNotifier.onChatMessage(chat(MASTER_TASK));
		assertEquals(2, eventCount());
	}

	private int eventCount()
	{
		return messageBuilder.getRoot().getEvents().size();
	}

	private static ChatMessage chat(String message)
	{
		ChatMessage event = new ChatMessage();
		event.setType(ChatMessageType.GAMEMESSAGE);
		event.setMessage(message);
		return event;
	}
}
