package haexporterplugin.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class SkillInfoTest
{
	@Test
	public void testDefaultConstructor()
	{
		SkillInfo skillInfo = new SkillInfo();
		assertNull(skillInfo.getXp());
		assertNull(skillInfo.getLevel());
	}

	@Test
	public void testParameterizedConstructor()
	{
		SkillInfo skillInfo = new SkillInfo(200000000, 99);
		assertEquals(Integer.valueOf(200000000), skillInfo.getXp());
		assertEquals(Integer.valueOf(99), skillInfo.getLevel());
	}

	@Test
	public void testSetters()
	{
		SkillInfo skillInfo = new SkillInfo();
		skillInfo.setXp(13034431);
		skillInfo.setLevel(99);

		assertEquals(Integer.valueOf(13034431), skillInfo.getXp());
		assertEquals(Integer.valueOf(99), skillInfo.getLevel());
	}

	@Test
	public void testLowLevelSkill()
	{
		SkillInfo skillInfo = new SkillInfo(0, 1);
		assertEquals(Integer.valueOf(0), skillInfo.getXp());
		assertEquals(Integer.valueOf(1), skillInfo.getLevel());
	}
}
