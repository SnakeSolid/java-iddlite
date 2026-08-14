package ru.snake.collection.idd.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.snake.collection.idd.util.VariableRange;

class VariableRangeTest {

	@Test
	@DisplayName("fullRange returns [MIN_VALUE, MAX_VALUE]")
	void testFullRange() {
		VariableRange range = VariableRange.fullRange();
		assertEquals(Integer.MIN_VALUE, range.min());
		assertEquals(Integer.MAX_VALUE, range.max());
	}

	@Test
	@DisplayName("fullRange returns the same singleton")
	void testFullRangeSingleton() {
		assertSame(VariableRange.fullRange(), VariableRange.fullRange());
	}

	@Test
	@DisplayName("of creates a custom range")
	void testOf() {
		VariableRange range = VariableRange.of(0, 65535);
		assertEquals(0, range.min());
		assertEquals(65535, range.max());
	}

	@Test
	@DisplayName("rejects min > max")
	void testRejectsInvertedRange() {
		assertThrows(IllegalArgumentException.class, () ->
			new VariableRange(10, 5)
		);
	}

	@Test
	@DisplayName("contains checks boundaries")
	void testContains() {
		VariableRange range = VariableRange.of(0, 100);
		assertTrue(range.contains(0));
		assertTrue(range.contains(50));
		assertTrue(range.contains(100));
		assertFalse(range.contains(-1));
		assertFalse(range.contains(101));
	}

	@Test
	@DisplayName("equality and hashCode")
	void testEquality() {
		VariableRange a = VariableRange.of(0, 65535);
		VariableRange b = VariableRange.of(0, 65535);
		VariableRange c = VariableRange.of(0, 100);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
	}

	@Test
	@DisplayName("toString format")
	void testToString() {
		VariableRange range = VariableRange.of(0, 255);
		assertEquals("[0,255]", range.toString());
	}

	@Test
	@DisplayName("single-value range is valid")
	void testSingleValueRange() {
		VariableRange range = VariableRange.of(42, 42);
		assertEquals(42, range.min());
		assertEquals(42, range.max());
		assertTrue(range.contains(42));
		assertFalse(range.contains(41));
		assertFalse(range.contains(43));
	}

	@Test
	@DisplayName("contains works at MIN_VALUE and MAX_VALUE boundaries")
	void testContainsExtremeBoundaries() {
		VariableRange range = VariableRange.of(
			Integer.MIN_VALUE,
			Integer.MAX_VALUE
		);
		assertTrue(range.contains(Integer.MIN_VALUE));
		assertTrue(range.contains(Integer.MAX_VALUE));
		assertTrue(range.contains(0));
	}

	@Test
	@DisplayName("of with full range equals fullRange singleton")
	void testOfFullRangeEqualsSingleton() {
		VariableRange full = VariableRange.of(
			Integer.MIN_VALUE,
			Integer.MAX_VALUE
		);
		assertEquals(VariableRange.fullRange(), full);
		assertEquals(VariableRange.fullRange().hashCode(), full.hashCode());
	}

	@Test
	@DisplayName("range with negative bounds")
	void testNegativeBounds() {
		VariableRange range = VariableRange.of(-100, -1);
		assertEquals(-100, range.min());
		assertEquals(-1, range.max());
		assertTrue(range.contains(-50));
		assertTrue(range.contains(-1));
		assertFalse(range.contains(0));
		assertFalse(range.contains(-101));
	}

	@Test
	@DisplayName("equals is symmetric and reflexive")
	void testEqualsSymmetricReflexive() {
		VariableRange a = VariableRange.of(0, 255);
		assertTrue(a.equals(a));
		VariableRange b = VariableRange.of(0, 255);
		assertTrue(a.equals(b));
		assertTrue(b.equals(a));
	}
}
