package ru.snake.collection.idd.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.util.VariableRange;

class VariableOrderTest {

	@Test
	@DisplayName("Variable order basics")
	void testBasics() {
		VariableOrder order = new VariableOrder("x", "y", "z");
		assertEquals(3, order.size());
		assertEquals("x", order.name(0));
		assertEquals("y", order.name(1));
		assertEquals("z", order.name(2));
	}

	@Test
	@DisplayName("Variable order index lookup")
	void testIndexLookup() {
		VariableOrder order = new VariableOrder("a", "b", "c");
		assertEquals(0, order.index("a"));
		assertEquals(1, order.index("b"));
		assertEquals(2, order.index("c"));
	}

	@Test
	@DisplayName("Variable order rejects duplicate names")
	void testRejectsDuplicates() {
		assertThrows(IllegalArgumentException.class, () ->
			new VariableOrder("x", "x")
		);
	}

	@Test
	@DisplayName("Variable order throws on unknown name")
	void testUnknownName() {
		VariableOrder order = new VariableOrder("x", "y");
		assertThrows(IllegalArgumentException.class, () -> order.index("z"));
	}

	@Test
	@DisplayName("Variable order throws on out-of-range index")
	void testOutOfRangeIndex() {
		VariableOrder order = new VariableOrder("x", "y");
		assertThrows(IllegalArgumentException.class, () -> order.name(-1));
		assertThrows(IllegalArgumentException.class, () -> order.name(2));
	}

	@Test
	@DisplayName("Compare respects order")
	void testCompare() {
		VariableOrder order = new VariableOrder("a", "b", "c");
		assertEquals(-1, order.compare(0, 1));
		assertEquals(0, order.compare(1, 1));
		assertEquals(1, order.compare(2, 0));
	}

	@Test
	@DisplayName("range(name) returns full range by default")
	void testDefaultRange() {
		VariableOrder order = new VariableOrder("x", "y");
		assertEquals(VariableRange.fullRange(), order.range("x"));
		assertEquals(VariableRange.fullRange(), order.range("y"));
	}

	@Test
	@DisplayName("range(index) returns full range by default")
	void testDefaultRangeByIndex() {
		VariableOrder order = new VariableOrder("x", "y");
		assertEquals(VariableRange.fullRange(), order.range(0));
		assertEquals(VariableRange.fullRange(), order.range(1));
	}

	@Test
	@DisplayName("custom ranges are returned by name")
	void testCustomRangeByName() {
		Map<String, VariableRange> ranges = Map.of(
			"port",
			VariableRange.of(0, 65535),
			"proto",
			VariableRange.of(0, 255)
		);
		VariableOrder order = new VariableOrder(ranges, "port", "proto");
		assertEquals(VariableRange.of(0, 65535), order.range("port"));
		assertEquals(VariableRange.of(0, 255), order.range("proto"));
	}

	@Test
	@DisplayName("custom ranges are returned by index")
	void testCustomRangeByIndex() {
		Map<String, VariableRange> ranges = Map.of(
			"port",
			VariableRange.of(0, 65535)
		);
		VariableOrder order = new VariableOrder(ranges, "port", "proto");
		assertEquals(VariableRange.of(0, 65535), order.range(0));
		assertEquals(VariableRange.fullRange(), order.range(1));
	}

	@Test
	@DisplayName("range(name) throws on unknown variable")
	void testRangeUnknownName() {
		VariableOrder order = new VariableOrder("x");
		assertThrows(IllegalArgumentException.class, () -> order.range("z"));
	}

	@Test
	@DisplayName("range(index) throws on out-of-range index")
	void testRangeOutOfRangeIndex() {
		VariableOrder order = new VariableOrder("x", "y");
		assertThrows(IllegalArgumentException.class, () -> order.range(-1));
		assertThrows(IllegalArgumentException.class, () -> order.range(2));
	}
}
