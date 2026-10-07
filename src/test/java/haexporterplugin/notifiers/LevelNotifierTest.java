package haexporterplugin.notifiers;

import com.google.gson.Gson;
import com.google.inject.Injector;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.TestUtils;
import haexporterplugin.utils.MessageBuilder;
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

	private final TickUtils tickUtils = mock(TickUtils.class);
	private final HAExporterConfig config = mock(HAExporterConfig.class);
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
		when(config.sendRate()).thenReturn(100);
		// Level-up filter defaults: send every level
		when(config.levelMinValue()).thenReturn(1);
		when(config.levelInterval()).thenReturn(1);
		when(config.levelIncludeVirtual()).thenReturn(true);
		when(config.levelIncludeCombat()).thenReturn(true);
		// TickUtils counts ticks since the last send, which is usually well past the init delay
		when(tickUtils.getTickCount()).thenReturn(60);

		Injector injector = TestUtils.injector(Map.of(Client.class, client, HAExporterConfig.class, config, TickUtils.class, tickUtils));
		messageBuilder = injector.getInstance(MessageBuilder.class);
		levelNotifier = injector.getInstance(LevelNotifier.class);
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
		setAllLevels(99); // e.g. a Leagues character
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

	@Test
	public void testIntervalFiltersEventsButStatsStillUpdate()
	{
		when(config.levelInterval()).thenReturn(5);
		tick(5);

		levels.put(Skill.MINING, 51);
		levelNotifier.onTick();
		assertNoLevelUp();
		assertEquals(Integer.valueOf(51), messageBuilder.getPlayer().getStats().getSkills().get("Mining").getLevel());

		levels.put(Skill.MINING, 55);
		levelNotifier.onTick();
		assertEquals(1, messageBuilder.getRoot().getEvents().size());
	}

	@Test
	public void testCombatLevelCanBeTurnedOff()
	{
		when(config.levelIncludeCombat()).thenReturn(false);
		tick(5);

		// Raising the combat skills raises the combat level too
		for (Skill skill : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.PRAYER})
		{
			levels.put(skill, 60);
		}
		levelNotifier.onTick();

		Map<?, ?> event = (Map<?, ?>) messageBuilder.getRoot().getEvents().get(0);
		String json = new Gson().toJson(event.get("data"));
		assertTrue(json.contains("Attack"));
		assertFalse(json.contains("Combat"));
	}

	@Test
	public void testShouldSend()
	{
		// The defaults send every level, virtual levels and the combat level included
		assertTrue(LevelNotifier.shouldSend("Attack", 2, 1, 1, true, true));
		assertTrue(LevelNotifier.shouldSend("Attack", 105, 1, 1, true, true));
		assertTrue(LevelNotifier.shouldSend("Combat", 50, 1, 1, true, true));
		// Minimum level
		assertFalse(LevelNotifier.shouldSend("Attack", 69, 70, 1, true, true));
		assertTrue(LevelNotifier.shouldSend("Attack", 70, 70, 1, true, true));
		// 99 is sent whatever the interval
		assertTrue(LevelNotifier.shouldSend("Mining", 99, 1, 10, true, true));
		// Without virtual levels; the combat level is never virtual
		assertFalse(LevelNotifier.shouldSend("Mining", 100, 1, 1, false, true));
		assertTrue(LevelNotifier.shouldSend("Mining", 99, 1, 1, false, true));
		assertTrue(LevelNotifier.shouldSend("Combat", 100, 1, 1, false, true));
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
