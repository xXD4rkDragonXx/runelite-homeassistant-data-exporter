package haexporterplugin.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class HealthDataTest
{
	@Test
	public void testConstructor()
	{
		HealthData health = new HealthData(85, 99);
		assertNotNull(health);
	}

	@Test
	public void testFullHealth()
	{
		HealthData health = new HealthData(99, 99);
		assertNotNull(health);
	}

	@Test
	public void testZeroHealth()
	{
		HealthData health = new HealthData(0, 99);
		assertNotNull(health);
	}
}
