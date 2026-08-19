package ru.snake.collection.idd.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.core.VariableRanges;
import ru.snake.collection.idd.util.VariableRange;

class VariableRangesTest {

	@Test
	@DisplayName("range(name) returns full range by default")
	void testDefaultRange() {
		VariableOrder order = new VariableOrder("x", "y");
		VariableRanges ranges = new VariableRanges(Map.of(), order);
		assertEquals(VariableRange.fullRange(), ranges.range("x", order));
		assertEquals(VariableRange.fullRange(), ranges.range("y", order));
	}

	@Test
	@DisplayName("range(index) returns full range by default")
	void testDefaultRangeByIndex() {
		VariableOrder order = new VariableOrder("x", "y");
		VariableRanges ranges = new VariableRanges(Map.of(), order);
		assertEquals(VariableRange.fullRange(), ranges.range(0));
		assertEquals(VariableRange.fullRange(), ranges.range(1));
	}

	@Test
	@DisplayName("custom ranges are returned by name")
	void testCustomRangeByName() {
		VariableOrder order = new VariableOrder("port", "proto");
		VariableRanges ranges = new VariableRanges(
			Map.of("port", VariableRange.of(0, 65535), "proto", VariableRange.of(0, 255)),
			order
		);
		assertEquals(VariableRange.of(0, 65535), ranges.range("port", order));
		assertEquals(VariableRange.of(0, 255), ranges.range("proto", order));
	}

	@Test
	@DisplayName("custom ranges are returned by index")
	void testCustomRangeByIndex() {
		VariableOrder order = new VariableOrder("port", "proto");
		VariableRanges ranges = new VariableRanges(Map.of("port", VariableRange.of(0, 65535)), order);
		assertEquals(VariableRange.of(0, 65535), ranges.range(0));
		assertEquals(VariableRange.fullRange(), ranges.range(1));
	}

	@Test
	@DisplayName("range(name) throws on unknown variable")
	void testRangeUnknownName() {
		VariableOrder order = new VariableOrder("x");
		VariableRanges ranges = new VariableRanges(Map.of(), order);
		assertThrows(IllegalArgumentException.class, () -> ranges.range("z", order));
	}

	@Test
	@DisplayName("range(index) throws on out-of-range index")
	void testRangeOutOfRangeIndex() {
		VariableOrder order = new VariableOrder("x", "y");
		VariableRanges ranges = new VariableRanges(Map.of(), order);
		assertThrows(IllegalArgumentException.class, () -> ranges.range(-1));
		assertThrows(IllegalArgumentException.class, () -> ranges.range(2));
	}

	@Test
	@DisplayName("partial range map: unmapped variables get full range")
	void testPartialRangeMap() {
		VariableOrder order = new VariableOrder("port", "proto", "flag");
		VariableRanges ranges = new VariableRanges(Map.of("port", VariableRange.of(0, 65535)), order);
		assertEquals(VariableRange.of(0, 65535), ranges.range(0)); // port
		assertEquals(VariableRange.fullRange(), ranges.range(1)); // proto
		assertEquals(VariableRange.fullRange(), ranges.range(2)); // flag
	}
}
