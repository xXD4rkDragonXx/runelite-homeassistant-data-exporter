package haexporterplugin.notifiers;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Providers;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.enums.CombatTaskTier;
import haexporterplugin.enums.DiaryTier;
import haexporterplugin.utils.HomeAssistUtils;
import haexporterplugin.utils.MessageBuilder;
import haexporterplugin.utils.RarityUtils;
import haexporterplugin.utils.ThievingUtils;
import haexporterplugin.utils.TickUtils;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TaskTierNotifierTest
{
	private static final String HARD_DIARY = "Well done! You have completed a hard task in the Varrock area. Your Achievement Diary has been updated.";
	private static final String ELITE_DIARY = "Well done! You have completed an elite task in the Varrock area. Your Achievement Diary has been updated.";
	private static final String MEDIUM_TASK = "Congratulations, you've completed a medium combat task: Hitting Them Where It Hurts.";
	private static final String MASTER_TASK = "Congratulations, you've completed a master combat task: Perfect Zulrah.";

	private HAExporterConfig config;
	private MessageBuilder messageBuilder;
	private AchievementDiaryNotifier diaryNotifier;
	private CombatTaskNotifier combatTaskNotifier;

	@Before
	public void setUp()
	{
		config = mock(HAExporterConfig.class);
		when(config.diaryMinTier()).thenReturn(DiaryTier.EASY);
		when(config.combatTaskMinTier()).thenReturn(CombatTaskTier.EASY);

		// Providers.of avoids Guice member-injecting the mocks' inherited @Inject fields
		Injector injector = Guice.createInjector(binder ->
		{
			binder.bind(Client.class).toProvider(Providers.of(mock(Client.class)));
			binder.bind(HAExporterConfig.class).toProvider(Providers.of(config));
			binder.bind(TickUtils.class).toProvider(Providers.of(mock(TickUtils.class)));
			binder.bind(HomeAssistUtils.class).toProvider(Providers.of(mock(HomeAssistUtils.class)));
			binder.bind(RarityUtils.class).toProvider(Providers.of(mock(RarityUtils.class)));
			binder.bind(ThievingUtils.class).toProvider(Providers.of(mock(ThievingUtils.class)));
			binder.bind(Gson.class).toInstance(new Gson());
		});
		messageBuilder = injector.getInstance(MessageBuilder.class);
		diaryNotifier = injector.getInstance(AchievementDiaryNotifier.class);
		combatTaskNotifier = injector.getInstance(CombatTaskNotifier.class);
	}

	@Test
	public void testDiaryDefaultSendsEveryTier()
	{
		diaryNotifier.onChatMessage(chat(HARD_DIARY));

		assertEquals(1, eventCount());
	}

	@Test
	public void testDiaryBelowMinimumTierIsSkipped()
	{
		when(config.diaryMinTier()).thenReturn(DiaryTier.ELITE);

		diaryNotifier.onChatMessage(chat(HARD_DIARY));
		assertEquals(0, eventCount());

		diaryNotifier.onChatMessage(chat(ELITE_DIARY));
		assertEquals(1, eventCount());
	}

	@Test
	public void testCombatTaskDefaultSendsEveryTier()
	{
		combatTaskNotifier.onChatMessage(chat(MEDIUM_TASK));

		assertEquals(1, eventCount());
	}

	@Test
	public void testCombatTaskBelowMinimumTierIsSkipped()
	{
		when(config.combatTaskMinTier()).thenReturn(CombatTaskTier.HARD);

		combatTaskNotifier.onChatMessage(chat(MEDIUM_TASK));
		assertEquals(0, eventCount());

		combatTaskNotifier.onChatMessage(chat(MASTER_TASK));
		assertEquals(1, eventCount());
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
