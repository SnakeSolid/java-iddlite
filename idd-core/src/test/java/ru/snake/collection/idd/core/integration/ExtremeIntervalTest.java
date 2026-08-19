package ru.snake.collection.idd.core.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import ru.snake.collection.idd.core.operation.Evaluate;

class ExtremeIntervalTest {

	private VariableOrder order;

	private IDDFactory factory;

	@BeforeEach
	void setUp() {
		order = new VariableOrder("x");
		factory = new IDDFactory(order);
	}

	@Test
	@DisplayName("Full domain edge eliminates the node")
	void testFullDomainElimination() {
		IDD node = factory.getNode(0, List.of(new Edge(Integer.MIN_VALUE, Integer.MAX_VALUE, IDD.TRUE)));
		assertSame(IDD.TRUE, node);
	}

	@Test
	@DisplayName("Edge at MIN boundary")
	void testMinBoundary() {
		IDD node = factory
			.getNode(0, List.of(new Edge(Integer.MIN_VALUE, 0, IDD.TRUE), new Edge(1, Integer.MAX_VALUE, IDD.FALSE)));
		assertFalse(node.isTerminal());
		assertTrue(Evaluate.evaluate(node, order, Map.of("x", Integer.MIN_VALUE)));
		assertTrue(Evaluate.evaluate(node, order, Map.of("x", 0)));
		assertFalse(Evaluate.evaluate(node, order, Map.of("x", 1)));
	}

	@Test
	@DisplayName("Edge at MAX boundary")
	void testMaxBoundary() {
		IDD node = factory.getNode(
			0,
			List.of(
				new Edge(Integer.MIN_VALUE, Integer.MAX_VALUE - 1, IDD.FALSE),
				new Edge(Integer.MAX_VALUE, Integer.MAX_VALUE, IDD.TRUE)
			)
		);
		assertFalse(node.isTerminal());
		assertFalse(Evaluate.evaluate(node, order, Map.of("x", Integer.MIN_VALUE)));
		assertTrue(Evaluate.evaluate(node, order, Map.of("x", Integer.MAX_VALUE)));
	}

	@Test
	@DisplayName("NOT at extreme boundaries")
	void testNotExtreme() {
		IDD node = factory.getNode(
			0,
			List.of(
				new Edge(Integer.MIN_VALUE, Integer.MAX_VALUE - 1, IDD.TRUE),
				new Edge(Integer.MAX_VALUE, Integer.MAX_VALUE, IDD.FALSE)
			)
		);
		IDD notNode = factory.not(node);

		assertFalse(Evaluate.evaluate(notNode, order, Map.of("x", Integer.MIN_VALUE)));
		assertTrue(Evaluate.evaluate(notNode, order, Map.of("x", Integer.MAX_VALUE)));
	}

	@Test
	@DisplayName("No off-by-one error at MIN/MAX")
	void testNoOffByOne() {
		IDD node = factory
			.buildFromIntervals("x", List.of(new Edge(Integer.MIN_VALUE, Integer.MAX_VALUE, factory.trueNode())));
		// Should be eliminated to TRUE.
		assertSame(IDD.TRUE, node);
	}

	@Test
	@DisplayName("Edge with single-value interval at MAX")
	void testSingleValueMax() {
		IDD node = factory.getNode(0, List.of(new Edge(Integer.MAX_VALUE, Integer.MAX_VALUE, IDD.TRUE)));
		assertTrue(Evaluate.evaluate(node, order, Map.of("x", Integer.MAX_VALUE)));
		assertFalse(Evaluate.evaluate(node, order, Map.of("x", Integer.MAX_VALUE - 1)));
	}

	@Test
	@DisplayName("AND with extreme intervals")
	void testAndExtreme() {
		IDD a = factory.getNode(
			0,
			List.of(
				new Edge(Integer.MIN_VALUE, Integer.MAX_VALUE / 2, IDD.TRUE),
				new Edge(Integer.MAX_VALUE / 2 + 1, Integer.MAX_VALUE, IDD.FALSE)
			)
		);
		IDD b = factory.getNode(
			0,
			List.of(
				new Edge(Integer.MIN_VALUE, Integer.MAX_VALUE / 2, IDD.FALSE),
				new Edge(Integer.MAX_VALUE / 2 + 1, Integer.MAX_VALUE, IDD.TRUE)
			)
		);
		IDD c = factory.and(a, b);
		assertSame(IDD.FALSE, c);
	}
}
