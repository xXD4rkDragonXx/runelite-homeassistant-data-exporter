package haexporterplugin.data;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class StatsTest
{
	@Test
	public void testDefaultConstructor()
	{
		Stats stats = new Stats();
		assertNull(stats.getSkills());
	}

	@Test
	public void testParameterizedConstructor()
	{
		Map<String, SkillInfo> skills = new HashMap<>();
		skills.put("Attack", new SkillInfo(200000000, 99));
		skills.put("Defence", new SkillInfo(13034431, 99));

		Stats stats = new Stats(skills);

		assertNotNull(stats.getSkills());
		assertEquals(2, stats.getSkills().size());
		assertEquals(Integer.valueOf(99), stats.getSkills().get("Attack").getLevel());
	}

	@Test
	public void testSetSkills()
	{
		Stats stats = new Stats();
		Map<String, SkillInfo> skills = new HashMap<>();
		skills.put("Hitpoints", new SkillInfo(1154, 10));
		stats.setSkills(skills);

		assertNotNull(stats.getSkills());
		assertEquals(1, stats.getSkills().size());
	}

	@Test
	public void testEmptySkillsMap()
	{
		Stats stats = new Stats(new HashMap<>());
		assertNotNull(stats.getSkills());
		assertTrue(stats.getSkills().isEmpty());
	}
}
