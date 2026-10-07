package haexporterplugin.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilsTest
{
	@Test
	public void testAccountHash()
	{
		// Pinned so an accidental salt or algorithm change fails the build
		assertEquals("de731bc0f710567a6a0e852bbe79eb5fa8daf37d4140440a774591f0", Utils.accountHash(1234567890123456789L));
		// Not logged in
		assertNull(Utils.accountHash(-1L));
	}
}
