package haexporterplugin.utils;

import com.google.common.hash.HashCode;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import static org.junit.Assert.*;

public class UtilsTest
{
	private static final long ACCOUNT_HASH = 1234567890123456789L;

	@Test
	public void testAccountHashIsDeterministic()
	{
		assertEquals(Utils.accountHash(ACCOUNT_HASH), Utils.accountHash(ACCOUNT_HASH));
	}

	@Test
	public void testAccountHashIsLowercaseHex()
	{
		String hash = Utils.accountHash(ACCOUNT_HASH);
		assertNotNull(hash);
		assertTrue(hash.matches("[0-9a-f]{56}"));
	}

	@Test
	public void testAccountHashDiffersForDifferentInputs()
	{
		assertNotEquals(Utils.accountHash(ACCOUNT_HASH), Utils.accountHash(ACCOUNT_HASH + 1));
		assertNotEquals(Utils.accountHash(0L), Utils.accountHash(1L));
	}

	@Test
	public void testAccountHashIsSalted() throws Exception
	{
		byte[] unsalted = ByteBuffer.allocate(8).putLong(ACCOUNT_HASH).array();
		String plainHash = HashCode.fromBytes(MessageDigest.getInstance("SHA-224").digest(unsalted)).toString();
		assertNotEquals(plainHash, Utils.accountHash(ACCOUNT_HASH));
	}

	@Test
	public void testAccountHashNotLoggedIn()
	{
		assertNull(Utils.accountHash(-1L));
	}

	@Test
	public void testAccountHashGoldenValue()
	{
		// Pinned so an accidental salt or algorithm change fails the build
		assertEquals("de731bc0f710567a6a0e852bbe79eb5fa8daf37d4140440a774591f0", Utils.accountHash(ACCOUNT_HASH));
	}
}
