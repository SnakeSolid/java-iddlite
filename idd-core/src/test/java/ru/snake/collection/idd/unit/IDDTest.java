package ru.snake.collection.idd.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.util.VariableRange;

class IDDTest {

	private VariableOrder order;
	private IDDFactory factory;

	@BeforeEach
	void setUp() {
		order = new VariableOrder("x", "y", "z");
		factory = new IDDFactory(order);
	}

	@Test
	@DisplayName("TRUE and FALSE are singletons")
	void testSingletons() {
		assertSame(IDD.TRUE, IDD.TRUE);
		assertSame(IDD.FALSE, IDD.FALSE);
	}

	@Test
	@DisplayName("Terminals have correct properties")
	void testTerminalProperties() {
		assertTrue(IDD.TRUE.isTrue());
		assertFalse(IDD.TRUE.isFalse());
		assertFalse(IDD.FALSE.isTrue());
		assertTrue(IDD.FALSE.isFalse());
		assertTrue(IDD.TRUE.isTerminal());
		assertTrue(IDD.FALSE.isTerminal());
		assertEquals(-1, IDD.TRUE.variable());
	}

	@Test
	@DisplayName("Hash-consing: identical nodes return the same object")
	void testInterning() {
		IDD a = factory.getNode(0, List.of(new Edge(1, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE)));
		IDD b = factory.getNode(0, List.of(new Edge(1, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE)));
		assertSame(a, b);
	}

	@Test
	@DisplayName("Reduction: single full-domain edge eliminates the node")
	void testReduction() {
		IDD node = factory.getNode(0, List.of(new Edge(Integer.MIN_VALUE, Integer.MAX_VALUE, IDD.TRUE)));
		assertSame(IDD.TRUE, node);
	}

	@Test
	@DisplayName("Different nodes are different objects")
	void testDistinctNodes() {
		IDD a = factory.getNode(0, List.of(new Edge(1, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE)));
		IDD b = factory.getNode(0, List.of(new Edge(1, 4, IDD.TRUE), new Edge(5, 10, IDD.FALSE)));
		assertNotSame(a, b);
	}

	@Test
	@DisplayName("buildFromIntervals constructs an IDD")
	void testBuildFromIntervals() {
		IDD node = factory.buildFromIntervals("x", List.of(new Edge(1, 10, factory.trueNode())));
		assertFalse(node.isTerminal());
		assertEquals(0, node.variable());
	}

	@Test
	@DisplayName("Edge merging in normaliseEdges")
	void testEdgeMerging() {
		// [1,5]->TRUE, [6,10]->TRUE merges to [1,10]->TRUE.
		// Then [11,15]->FALSE.
		// Gap-filling: [MIN,0]->FALSE, [1,10]->TRUE, [11,15]->FALSE,
		// [16,MAX]->FALSE.
		// [11,15]->FALSE and [16,MAX]->FALSE should be adjacent and merge to
		// [11,MAX]->FALSE.
		IDD a = factory
			.getNode(0, List.of(new Edge(1, 5, IDD.TRUE), new Edge(6, 10, IDD.TRUE), new Edge(11, 15, IDD.FALSE)));
		// After merging: [MIN,0]->FALSE, [1,10]->TRUE, [11,MAX]->FALSE => 3
		// edges.
		assertEquals(3, a.edges().size());
		assertEquals(Integer.MIN_VALUE, a.edges().get(0).low());
		assertEquals(0, a.edges().get(0).high());
		assertSame(IDD.FALSE, a.edges().get(0).child());
		assertEquals(1, a.edges().get(1).low());
		assertEquals(10, a.edges().get(1).high());
		assertSame(IDD.TRUE, a.edges().get(1).child());
	}

	@Test
	@DisplayName("Gaps are filled with FALSE edges")
	void testGapFilling() {
		IDD node = factory.getNode(0, List.of(new Edge(5, 10, IDD.TRUE)));
		// Should have: [MIN,4]->FALSE, [5,10]->TRUE, [11,MAX]->FALSE => 3
		// edges.
		assertEquals(3, node.edges().size());
		assertSame(IDD.FALSE, node.edges().get(0).child());
		assertSame(IDD.TRUE, node.edges().get(1).child());
		assertSame(IDD.FALSE, node.edges().get(2).child());
	}

	@Test
	@DisplayName("Reduction uses variable's range, not full integer range")
	void testReductionWithCustomRange() {
		Map<String, VariableRange> ranges = Map.of("port", VariableRange.of(0, 65535));
		VariableOrder rangedOrder = new VariableOrder(ranges, "port");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		// Single edge covering the full variable range should be reduced.
		IDD node = rangedFactory.getNode(0, List.of(new Edge(0, 65535, IDD.TRUE)));
		assertSame(IDD.TRUE, node);
	}

	@Test
	@DisplayName("Gap filling uses variable's range min instead of MIN_VALUE")
	void testGapFillingWithCustomRange() {
		Map<String, VariableRange> ranges = Map.of("port", VariableRange.of(0, 65535));
		VariableOrder rangedOrder = new VariableOrder(ranges, "port");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		// Edge starting at 10 should fill gap from 0, not MIN_VALUE.
		IDD node = rangedFactory.getNode(0, List.of(new Edge(10, 20, IDD.TRUE)));
		assertEquals(3, node.edges().size());
		assertEquals(0, node.edges().get(0).low());
		assertEquals(9, node.edges().get(0).high());
		assertSame(IDD.FALSE, node.edges().get(0).child());
	}

	@Test
	@DisplayName("Trailing FALSE ends at variable's range max")
	void testTrailingFalseWithCustomRange() {
		Map<String, VariableRange> ranges = Map.of("port", VariableRange.of(0, 65535));
		VariableOrder rangedOrder = new VariableOrder(ranges, "port");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		IDD node = rangedFactory.getNode(0, List.of(new Edge(0, 10, IDD.TRUE)));
		assertEquals(2, node.edges().size());
		assertEquals(11, node.edges().get(1).low());
		assertEquals(65535, node.edges().get(1).high());
		assertSame(IDD.FALSE, node.edges().get(1).child());
	}

	@Test
	@DisplayName("Edge outside variable's range throws IllegalArgumentException")
	void testEdgeOutOfRange() {
		Map<String, VariableRange> ranges = Map.of("port", VariableRange.of(0, 65535));
		VariableOrder rangedOrder = new VariableOrder(ranges, "port");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		// Low below range.
		assertThrows(
			IllegalArgumentException.class,
			() -> rangedFactory.getNode(0, List.of(new Edge(-1, 10, IDD.TRUE)))
		);

		// High above range.
		assertThrows(
			IllegalArgumentException.class,
			() -> rangedFactory.getNode(0, List.of(new Edge(0, 70000, IDD.TRUE)))
		);
	}

	@Test
	@DisplayName("Hash-consing works with custom ranges")
	void testInterningWithCustomRange() {
		Map<String, VariableRange> ranges = Map.of("port", VariableRange.of(0, 65535));
		VariableOrder rangedOrder = new VariableOrder(ranges, "port");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		IDD a = rangedFactory.getNode(0, List.of(new Edge(1, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE)));
		IDD b = rangedFactory.getNode(0, List.of(new Edge(1, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE)));
		assertSame(a, b);
	}

	@Test
	@DisplayName("buildFromIntervals respects variable's range")
	void testBuildFromIntervalsWithCustomRange() {
		Map<String, VariableRange> ranges = Map.of("port", VariableRange.of(0, 65535));
		VariableOrder rangedOrder = new VariableOrder(ranges, "port");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		IDD node = rangedFactory.buildFromIntervals("port", List.of(new Edge(100, 200, rangedFactory.trueNode())));
		assertFalse(node.isTerminal());
		assertEquals(0, node.variable());
		// Gap-filling starts at range min (0), not Integer.MIN_VALUE.
		assertEquals(0, node.edges().get(0).low());
		assertEquals(99, node.edges().get(0).high());
		assertSame(IDD.FALSE, node.edges().get(0).child());
	}

	@Test
	@DisplayName("Edge merging works within custom range")
	void testEdgeMergingWithCustomRange() {
		Map<String, VariableRange> ranges = Map.of("port", VariableRange.of(0, 65535));
		VariableOrder rangedOrder = new VariableOrder(ranges, "port");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		// [1,5]->TRUE, [6,10]->TRUE merges to [1,10]->TRUE.
		IDD node = rangedFactory.getNode(0, List.of(new Edge(1, 5, IDD.TRUE), new Edge(6, 10, IDD.TRUE)));
		// Gap-filling: [0,0]->FALSE, [1,10]->TRUE, [11,65535]->FALSE => 3
		// edges.
		assertEquals(3, node.edges().size());
		assertEquals(0, node.edges().get(0).low());
		assertEquals(0, node.edges().get(0).high());
		assertSame(IDD.FALSE, node.edges().get(0).child());
	}

	@Test
	@DisplayName("Reduction with non-full-range single edge does not eliminate node")
	void testNoReductionPartialRange() {
		Map<String, VariableRange> ranges = Map.of("port", VariableRange.of(0, 65535));
		VariableOrder rangedOrder = new VariableOrder(ranges, "port");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		// Single edge does NOT cover full range — should NOT be reduced.
		IDD node = rangedFactory.getNode(0, List.of(new Edge(0, 100, IDD.TRUE)));
		assertFalse(node.isTrue());
		assertFalse(node.isFalse());
	}

	@Test
	@DisplayName("Edge at exact range boundaries is accepted")
	void testEdgeAtExactBoundaries() {
		Map<String, VariableRange> ranges = Map.of("proto", VariableRange.of(0, 255));
		VariableOrder rangedOrder = new VariableOrder(ranges, "proto");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		// Edge exactly at range boundaries should be accepted.
		IDD node = rangedFactory.getNode(0, List.of(new Edge(0, 255, IDD.TRUE)));
		// Covers full range, so it reduces to TRUE.
		assertSame(IDD.TRUE, node);
	}
}
