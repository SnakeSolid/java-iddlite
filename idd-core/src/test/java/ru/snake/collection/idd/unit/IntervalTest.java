package ru.snake.collection.idd.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.snake.collection.idd.util.Interval;

class IntervalTest {

	@Test
	@DisplayName("Interval invariants: low <= high")
	void testLowAtMostHigh() {
		assertThrows(IllegalArgumentException.class, () -> new Interval(5, 3));
	}

	@Test
	@DisplayName("Interval contains")
	void testContains() {
		Interval iv = new Interval(1, 10);
		assertTrue(iv.contains(1));
		assertTrue(iv.contains(5));
		assertTrue(iv.contains(10));
		assertFalse(iv.contains(0));
		assertFalse(iv.contains(11));
	}

	@Test
	@DisplayName("Interval adjacency")
	void testIsAdjacentTo() {
		Interval iv = new Interval(1, 5);
		assertTrue(iv.isAdjacentTo(6));
		assertFalse(iv.isAdjacentTo(5));
		assertFalse(iv.isAdjacentTo(7));
	}

	@Test
	@DisplayName("Interval nextLow handles overflow")
	void testNextLowOverflow() {
		Interval iv = new Interval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
		assertEquals((long) Integer.MAX_VALUE + 1, iv.nextLow());
	}

	@Test
	@DisplayName("Interval equality")
	void testEquality() {
		assertEquals(new Interval(1, 5), new Interval(1, 5));
		assertNotEquals(new Interval(1, 5), new Interval(1, 6));
	}
}
