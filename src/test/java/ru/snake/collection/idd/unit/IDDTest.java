package ru.snake.collection.idd.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;

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
}
