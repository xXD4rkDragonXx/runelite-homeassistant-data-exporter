package haexporterplugin.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class MathUtilsTest
{
	@Test
	public void testSumEmptyArray()
	{
		assertEquals(0, MathUtils.sum(new int[]{}));
	}

	@Test
	public void testSumSingleElement()
	{
		assertEquals(5, MathUtils.sum(new int[]{5}));
	}

	@Test
	public void testSumMultipleElements()
	{
		assertEquals(15, MathUtils.sum(new int[]{1, 2, 3, 4, 5}));
	}

	@Test
	public void testSumWithNegativeNumbers()
	{
		assertEquals(0, MathUtils.sum(new int[]{-1, 1}));
	}

	@Test
	public void testLessThanOrEqualWhenLess()
	{
		assertTrue(MathUtils.lessThanOrEqual(1.0, 2.0));
	}

	@Test
	public void testLessThanOrEqualWhenEqual()
	{
		assertTrue(MathUtils.lessThanOrEqual(1.0, 1.0));
	}

	@Test
	public void testLessThanOrEqualWhenGreater()
	{
		assertFalse(MathUtils.lessThanOrEqual(2.0, 1.0));
	}

	@Test
	public void testLessThanOrEqualWithFuzzyEquality()
	{
		assertTrue(MathUtils.lessThanOrEqual(1.0, 1.0 + MathUtils.EPSILON / 2));
	}

	@Test
	public void testFormatPercentageWholeNumber()
	{
		assertEquals("50%", MathUtils.formatPercentage(0.5, 2));
	}

	@Test
	public void testFormatPercentageSmallValue()
	{
		String result = MathUtils.formatPercentage(0.001, 3);
		assertEquals("0.1%", result);
	}

	@Test
	public void testFormatPercentageZero()
	{
		assertEquals("0%", MathUtils.formatPercentage(0.0, 2));
	}

	@Test
	public void testFormatPercentageHundredPercent()
	{
		assertEquals("100%", MathUtils.formatPercentage(1.0, 3));
	}

	@Test
	public void testCumulativeGeometricOneTrial()
	{
		double result = MathUtils.cumulativeGeometric(0.5, 1);
		assertEquals(0.5, result, 0.0001);
	}

	@Test
	public void testCumulativeGeometricMultipleTrials()
	{
		double result = MathUtils.cumulativeGeometric(0.5, 2);
		assertEquals(0.75, result, 0.0001);
	}

	@Test
	public void testCumulativeGeometricSmallProbability()
	{
		double result = MathUtils.cumulativeGeometric(0.01, 100);
		// 1 - (1-0.01)^100 ≈ 0.634
		assertEquals(0.634, result, 0.001);
	}

	@Test
	public void testBinomialProbabilityExactMatch()
	{
		// P(X=1) for p=0.5, n=1: should be 0.5
		double result = MathUtils.binomialProbability(0.5, 1, 1);
		assertEquals(0.5, result, 0.0001);
	}

	@Test
	public void testBinomialProbabilityZeroSuccess()
	{
		// P(X=0) for p=0.5, n=2: (1-0.5)^2 = 0.25
		double result = MathUtils.binomialProbability(0.5, 2, 0);
		assertEquals(0.25, result, 0.0001);
	}

	@Test
	public void testBinomialProbabilityAllSuccess()
	{
		// P(X=2) for p=0.5, n=2: (0.5)^2 = 0.25
		double result = MathUtils.binomialProbability(0.5, 2, 2);
		assertEquals(0.25, result, 0.0001);
	}
}
