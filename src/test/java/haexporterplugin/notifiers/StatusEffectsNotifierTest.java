package haexporterplugin.notifiers;

import com.google.inject.Injector;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.TestUtils;
import haexporterplugin.data.StatusEffectsData;
import haexporterplugin.data.StatusEffectsData.Effect;
import haexporterplugin.data.StatusEffectsData.Poison;
import haexporterplugin.data.StatusEffectsData.Wilderness;
import haexporterplugin.utils.MessageBuilder;
import haexporterplugin.utils.TickUtils;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static haexporterplugin.notifiers.StatusEffectsNotifier.TICK_MILLIS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class StatusEffectsNotifierTest
{
	private static final long START = 1_735_689_600_000L;

	private final Map<Integer, Integer> varbits = new HashMap<>();
	private final Map<Integer, Integer> varps = new HashMap<>();
	private final StatusEffectsNotifier.Vars vars = new StatusEffectsNotifier.Vars()
	{
		@Override
		public int varbit(int id)
		{
			return varbits.getOrDefault(id, 0);
		}

		@Override
		public int varp(int id)
		{
			return varps.getOrDefault(id, 0);
		}
	};
	private final TickUtils tickUtils = mock(TickUtils.class);
	private MessageBuilder messageBuilder;
	private StatusEffectsNotifier notifier;

	@Before
	public void setUp()
	{
		Injector injector = TestUtils.injector(Map.of(HAExporterConfig.class, TestUtils.configWithEverythingOn(), TickUtils.class, tickUtils));
		messageBuilder = injector.getInstance(MessageBuilder.class);
		notifier = injector.getInstance(StatusEffectsNotifier.class);
	}

	@Test
	public void testUnitsOfTheRegistry()
	{
		varbits.put(VarbitID.STAMINA_ACTIVE, 1);
		varbits.put(VarbitID.STAMINA_DURATION, 12);
		varbits.put(VarbitID.GOADING_POTION_TIMER, 100);
		varbits.put(VarbitID.PRAYER_REGENERATION_POTION_TIMER, 3);
		varbits.put(VarbitID.TELEBLOCK_CYCLES, 600);
		varps.put(VarPlayerID.POISON, -50);

		Map<String, Effect> effects = byId(notifier.effects(vars, 0, START));

		assertEquals(List.of("stamina", "antivenom", "goading", "prayer_regeneration", "teleblock"), List.copyOf(effects.keySet()));
		assertEnds(120, effects.get("stamina"));
		assertEquals(10 * TICK_MILLIS, effects.get("stamina").getPrecision());
		assertEnds(600, effects.get("goading"));
		assertEnds(36, effects.get("prayer_regeneration"));
		assertEnds(500, effects.get("teleblock"));
		// A full step until the next change, then 11 more steps until the immunity is down to antipoison
		assertEnds(30 + 11 * 30, effects.get("antivenom"));

		// Stamina only counts while it is active
		varbits.put(VarbitID.STAMINA_ACTIVE, 0);
		assertEquals(List.of("antivenom", "goading", "prayer_regeneration", "teleblock"),
			List.copyOf(byId(notifier.effects(vars, 1, START + TICK_MILLIS)).keySet()));
	}

	@Test
	public void testAntifireFollowsItsStepsAndAnotherDoseExtendsIt()
	{
		// 6 steps of 30 ticks, the first one taken as a full step
		varbits.put(VarbitID.ANTIFIRE_POTION, 6);
		long first = endsAt("antifire", 0);
		assertEquals(START + 180 * TICK_MILLIS, first);
		assertEquals(first, endsAt("antifire", 10));
		varbits.put(VarbitID.ANTIFIRE_POTION, 5);
		assertEquals(first, endsAt("antifire", 30));
		assertEquals(first, endsAt("antifire", 45));

		// Another dose at tick 45 resets it to 6 steps; the step started at tick 30 still ends at tick 60
		varbits.put(VarbitID.ANTIFIRE_POTION, 6);
		assertEquals(START + (60 + 150) * TICK_MILLIS, endsAt("antifire", 46));

		varbits.put(VarbitID.ANTIFIRE_POTION, 0);
		assertEquals(List.of(), notifier.effects(vars, 47, START + 47 * TICK_MILLIS));
	}

	@Test
	public void testEndTimeOnlyMovesByMoreThanItsPrecision()
	{
		varbits.put(VarbitID.STAMINA_ACTIVE, 1);
		varbits.put(VarbitID.STAMINA_DURATION, 10);
		long first = endsAt("stamina", 0);
		// Ticks pass while the varbit stays the same: the estimate drops but the end time stays
		assertEquals(first, endsAt("stamina", 9));
		varbits.put(VarbitID.STAMINA_DURATION, 9);
		assertEquals(first, endsAt("stamina", 10));
		// Another dose adds 20 units
		varbits.put(VarbitID.STAMINA_DURATION, 29);
		assertEquals(START + (11 + 290) * TICK_MILLIS, endsAt("stamina", 11));
	}

	@Test
	public void testCombinedDivinesReplaceTheSingleOnes()
	{
		for (int varbit : List.of(VarbitID.DIVINECOMBAT_POTION_TIME, VarbitID.DIVINEATTACK_POTION_TIME,
			VarbitID.DIVINESTRENGTH_POTION_TIME, VarbitID.DIVINEDEFENCE_POTION_TIME))
		{
			varbits.put(varbit, 500);
		}
		varbits.put(VarbitID.DIVINERANGE_POTION_TIME, 300);
		assertEquals(List.of("divine_ranging", "divine_super_combat"), List.copyOf(byId(notifier.effects(vars, 0, START)).keySet()));

		// A divine super attack drunk later lasts longer than the combined one
		varbits.put(VarbitID.DIVINEATTACK_POTION_TIME, 520);
		assertEquals(List.of("divine_super_attack", "divine_ranging", "divine_super_combat"),
			List.copyOf(byId(notifier.effects(vars, 1, START + TICK_MILLIS)).keySet()));
	}

	@Test
	public void testMeters()
	{
		assertEquals(Poison.NONE, StatusEffectsNotifier.poison(0));
		assertEquals(Poison.NONE, StatusEffectsNotifier.poison(-20));
		assertEquals(new Poison("poison", 6), StatusEffectsNotifier.poison(28));
		assertEquals(new Poison("venom", 6), StatusEffectsNotifier.poison(1_000_000));
		assertEquals(new Poison("venom", 20), StatusEffectsNotifier.poison(1_000_020));

		assertEquals(Integer.valueOf(23), StatusEffectsNotifier.parseWildernessLevel("Level: 23"));
		assertEquals(Integer.valueOf(5), StatusEffectsNotifier.parseWildernessLevel("Level: 5<br>Multi"));
		assertNull(StatusEffectsNotifier.parseWildernessLevel(""));
		assertEquals(Integer.valueOf(1), StatusEffectsNotifier.levelAt(new WorldPoint(3100, 3523, 0)));
		assertEquals(Integer.valueOf(56), StatusEffectsNotifier.levelAt(new WorldPoint(3100, 3960, 0)));
		assertEquals(Integer.valueOf(3), StatusEffectsNotifier.levelAt(new WorldPoint(3100, 9937, 0)));
	}

	@Test
	public void testSendsRightAwayOnlyWhenSomethingStartsOrEnds()
	{
		Effect stamina = new Effect("stamina", "Stamina", START + 60_000, 6000);
		// The first ticks after logging in only set the state
		for (int i = 0; i < 6; i++) notifier.update(status(73, Poison.NONE, null, List.of()));
		notifier.update(status(73, Poison.NONE, null, List.of(stamina)));
		// Run energy and the next poison hit go with the regular update
		notifier.update(status(60, Poison.NONE, null, List.of(stamina)));
		notifier.update(status(60, new Poison("poison", 6), null, List.of(stamina)));
		notifier.update(status(60, new Poison("poison", 5), null, List.of(stamina)));
		notifier.update(status(60, new Poison("poison", 5), new Wilderness(12), List.of(stamina)));
		notifier.update(status(60, new Poison("poison", 5), new Wilderness(12), List.of()));
		verify(tickUtils, times(4)).sendNow();

		TestUtils.assertJsonEquals("{'runEnergy':60,'running':true,'specialAttack':100,'poison':{'type':'poison','damage':5},'wilderness':{'level':12},'effects':[]}",
			TestUtils.json(messageBuilder.build()).getAsJsonObject().getAsJsonObject("player").get("status").toString());
	}

	private static StatusEffectsData status(int runEnergy, Poison poison, Wilderness wilderness, List<Effect> effects)
	{
		return new StatusEffectsData(runEnergy, true, 100, poison, wilderness, effects);
	}

	private long endsAt(String id, int tick)
	{
		return byId(notifier.effects(vars, tick, START + (long) tick * TICK_MILLIS)).get(id).getEndsAt();
	}

	private static void assertEnds(int ticks, Effect effect)
	{
		assertEquals(Long.valueOf(START + (long) ticks * TICK_MILLIS), effect.getEndsAt());
	}

	private static Map<String, Effect> byId(List<Effect> effects)
	{
		return effects.stream().collect(Collectors.toMap(Effect::getId, e -> e, (a, b) -> a, java.util.LinkedHashMap::new));
	}
}
