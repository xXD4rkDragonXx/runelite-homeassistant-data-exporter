package haexporterplugin.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class MathUtilsTest
{
	@Test
	public void testLessThanOrEqual()
	{
		assertTrue(MathUtils.lessThanOrEqual(1.0, 2.0));
		assertTrue(MathUtils.lessThanOrEqual(1.0, 1.0));
		assertFalse(MathUtils.lessThanOrEqual(2.0, 1.0));
		// Within EPSILON above still counts as equal
		assertTrue(MathUtils.lessThanOrEqual(1.0 + MathUtils.EPSILON / 2, 1.0));
	}

	@Test
	public void testBinomialProbability()
	{
		// P(X=k) for p=0.5 and n=2
		assertEquals(0.25, MathUtils.binomialProbability(0.5, 2, 0), 0.0001);
		assertEquals(0.5, MathUtils.binomialProbability(0.5, 2, 1), 0.0001);
		assertEquals(0.25, MathUtils.binomialProbability(0.5, 2, 2), 0.0001);
	}
}
