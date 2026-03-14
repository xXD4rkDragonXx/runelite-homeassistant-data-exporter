package haexporterplugin.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class PlayerLocationTest
{
	@Test
	public void testConstructorWithCoordinates()
	{
		PlayerLocation location = new PlayerLocation(3222, 3218, 0, false);
		assertNotNull(location);
	}

	@Test
	public void testConstructorWithBoatFlag()
	{
		PlayerLocation onBoat = new PlayerLocation(3222, 3218, 0, true);
		assertNotNull(onBoat);

		PlayerLocation offBoat = new PlayerLocation(3222, 3218, 0, false);
		assertNotNull(offBoat);
	}

	@Test
	public void testDifferentPlanes()
	{
		PlayerLocation plane0 = new PlayerLocation(3222, 3218, 0, false);
		PlayerLocation plane1 = new PlayerLocation(3222, 3218, 1, false);
		PlayerLocation plane2 = new PlayerLocation(3222, 3218, 2, false);

		assertNotNull(plane0);
		assertNotNull(plane1);
		assertNotNull(plane2);
	}
}
