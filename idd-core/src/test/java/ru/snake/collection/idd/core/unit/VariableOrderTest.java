package ru.snake.collection.idd.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.snake.collection.idd.core.VariableOrder;

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
		assertThrows(IllegalArgumentException.class, () -> new VariableOrder("x", "x"));
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
	@DisplayName("atOrBefore checks ordering")
	void testAtOrBefore() {
		VariableOrder order = new VariableOrder("a", "b", "c");
		assertEquals(true, order.atOrBefore(0, 1));
		assertEquals(true, order.atOrBefore(1, 1));
		assertEquals(false, order.atOrBefore(2, 0));
	}

	@Test
	@DisplayName("contains checks variable presence")
	void testContains() {
		VariableOrder order = new VariableOrder("x", "y");
		assertEquals(true, order.contains("x"));
		assertEquals(true, order.contains("y"));
		assertEquals(false, order.contains("z"));
	}

	@Test
	@DisplayName("toString format")
	void testToString() {
		VariableOrder order = new VariableOrder("a", "b", "c");
		assertEquals("VariableOrder[a, b, c]", order.toString());
	}
}
