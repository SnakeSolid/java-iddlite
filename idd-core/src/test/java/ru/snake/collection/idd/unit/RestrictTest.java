package ru.snake.collection.idd.unit;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.operation.Restrict;
import ru.snake.collection.idd.util.VariableRange;

class RestrictTest {

	private VariableOrder order;
	private IDDFactory factory;

	@BeforeEach
	void setUp() {
		order = new VariableOrder("x", "y", "z");
		factory = new IDDFactory(order);
	}

	@Test
	@DisplayName("Restrict terminal returns itself")
	void testRestrictTerminal() {
		assertSame(IDD.TRUE, Restrict.restrict(factory, IDD.TRUE, "x", 5));
		assertSame(IDD.FALSE, Restrict.restrict(factory, IDD.FALSE, "y", 10));
	}

	@Test
	@DisplayName("Restrict picks the correct child")
	void testRestrictCorrectChild() {
		IDD node = factory.getNode(0, List.of(new Edge(1, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE)));

		assertSame(IDD.TRUE, Restrict.restrict(factory, node, "x", 3));
		assertSame(IDD.FALSE, Restrict.restrict(factory, node, "x", 8));
	}

	@Test
	@DisplayName("Restrict on a non-present variable recurses but returns equivalent structure")
	void testRestrictNonPresent() {
		IDD node = factory.getNode(0, List.of(new Edge(1, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE)));
		// Restricting "z" (index 2) on a node with variable "x" (index 0)
		// recurses
		// into children (TRUE/FALSE), rebuilds, and should get the same node
		// back.
		IDD result = Restrict.restrict(factory, node, "z", 100);
		assertSame(node, result);
	}

	@Test
	@DisplayName("Restrict recurses through lower variables")
	void testRestrictRecurse() {
		// Build: x -> [1,5]->(y -> [1,3]->TRUE, [4,6]->FALSE), [6,10]->FALSE
		IDD yNode = factory.getNode(1, List.of(new Edge(1, 3, IDD.TRUE), new Edge(4, 6, IDD.FALSE)));
		IDD xNode = factory.getNode(0, List.of(new Edge(1, 5, yNode), new Edge(6, 10, IDD.FALSE)));

		// Restrict x=3, y=2 -> should pick yNode from x=3, then TRUE from y=2.
		IDD afterX = Restrict.restrict(factory, xNode, "x", 3);
		IDD result = Restrict.restrict(factory, afterX, "y", 2);
		assertSame(IDD.TRUE, result);
	}

	// ---- Tests with custom ranges ----

	@Test
	@DisplayName("Restrict with custom range picks correct child")
	void testRestrictCustomRange() {
		Map<String, VariableRange> ranges = Map.of("port", VariableRange.of(0, 65535));
		VariableOrder rangedOrder = new VariableOrder(ranges, "port");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		IDD node = rangedFactory.getNode(0, List.of(new Edge(0, 1024, IDD.TRUE), new Edge(1025, 65535, IDD.FALSE)));

		assertSame(IDD.TRUE, Restrict.restrict(rangedFactory, node, "port", 80));
		assertSame(IDD.FALSE, Restrict.restrict(rangedFactory, node, "port", 8080));
	}

	@Test
	@DisplayName("Restrict with custom range at boundaries")
	void testRestrictCustomRangeBoundaries() {
		Map<String, VariableRange> ranges = Map.of("proto", VariableRange.of(0, 255));
		VariableOrder rangedOrder = new VariableOrder(ranges, "proto");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		IDD node = rangedFactory.getNode(0, List.of(new Edge(0, 127, IDD.TRUE), new Edge(128, 255, IDD.FALSE)));

		assertSame(IDD.TRUE, Restrict.restrict(rangedFactory, node, "proto", 0));
		assertSame(IDD.TRUE, Restrict.restrict(rangedFactory, node, "proto", 127));
		assertSame(IDD.FALSE, Restrict.restrict(rangedFactory, node, "proto", 128));
		assertSame(IDD.FALSE, Restrict.restrict(rangedFactory, node, "proto", 255));
	}
}
