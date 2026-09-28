package haexporterplugin.notifiers;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Providers;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.utils.HomeAssistUtils;
import haexporterplugin.utils.MessageBuilder;
import haexporterplugin.utils.RarityUtils;
import haexporterplugin.utils.ThievingUtils;
import haexporterplugin.utils.TickUtils;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameStateChanged;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LevelNotifierTest
{
	// Levels of the character the client currently reports
	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);

	private TickUtils tickUtils;
	private MessageBuilder messageBuilder;
	private LevelNotifier levelNotifier;

	@Before
	public void setUp()
	{
		setAllLevels(50);

		Client client = mock(Client.class);
		when(client.getRealSkillLevel(any())).thenAnswer(inv -> levels.get(inv.<Skill>getArgument(0)));
		when(client.getSkillExperience(any())).thenAnswer(inv -> Experience.getXpForLevel(levels.get(inv.<Skill>getArgument(0))));
		when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS));
		HAExporterConfig config = mock(HAExporterConfig.class);
		when(config.sendRate()).thenReturn(100);
		tickUtils = mock(TickUtils.class);
		// TickUtils counts ticks since the last send, which is usually well past the init delay
		when(tickUtils.getTickCount()).thenReturn(60);

		// Providers.of avoids Guice member-injecting the mocks' inherited @Inject fields
		Injector injector = Guice.createInjector(binder ->
		{
			binder.bind(Client.class).toProvider(Providers.of(client));
			binder.bind(HAExporterConfig.class).toProvider(Providers.of(config));
			binder.bind(TickUtils.class).toProvider(Providers.of(tickUtils));
			binder.bind(HomeAssistUtils.class).toProvider(Providers.of(mock(HomeAssistUtils.class)));
			binder.bind(RarityUtils.class).toProvider(Providers.of(mock(RarityUtils.class)));
			binder.bind(ThievingUtils.class).toProvider(Providers.of(mock(ThievingUtils.class)));
			binder.bind(Gson.class).toInstance(new Gson());
		});
		messageBuilder = injector.getInstance(MessageBuilder.class);
		levelNotifier = injector.getInstance(LevelNotifier.class);
	}

	@Test
	public void testNoLevelUpWhenHoppingBackFromSpecialWorld()
	{
		tick(5); // baseline on the normal world

		hop();
		setAllLevels(99); // e.g. a Leagues character
		tick(5);

		hop();
		setAllLevels(50); // back on the main account
		tick(5);

		assertNoLevelUp();
	}

	@Test
	public void testNoLevelUpAfterLoggingIntoAnotherAccount()
	{
		tick(5);

		levelNotifier.onGameStateChanged(gameState(GameState.LOGIN_SCREEN));
		setAllLevels(70);
		tick(5);

		assertNoLevelUp();
	}

	@Test
	public void testBaselineWaitsForTheNewWorldToLoad()
	{
		tick(5);

		hop();
		levelNotifier.onTick(); // first tick after the hop: the old character's stats can still be reported
		setAllLevels(99);
		tick(5);

		assertNoLevelUp();
	}

	@Test
	public void testRealLevelUpAfterHopIsStillReported()
	{
		tick(5);
		hop();
		tick(5);

		levels.put(Skill.ATTACK, 51);
		levelNotifier.onTick();

		verify(tickUtils).sendNow();
		assertEquals(1, messageBuilder.getRoot().getEvents().size());
	}

	private void hop()
	{
		levelNotifier.onGameStateChanged(gameState(GameState.HOPPING));
	}

	private void tick(int count)
	{
		for (int i = 0; i < count; i++)
		{
			levelNotifier.onTick();
		}
	}

	private void setAllLevels(int level)
	{
		for (Skill skill : Skill.values())
		{
			levels.put(skill, level);
		}
	}

	private void assertNoLevelUp()
	{
		verify(tickUtils, never()).sendNow();
		assertTrue(messageBuilder.getRoot().getEvents().isEmpty());
	}

	private static GameStateChanged gameState(GameState state)
	{
		GameStateChanged event = new GameStateChanged();
		event.setGameState(state);
		return event;
	}
}
