package ru.snake.collection.idd.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import ru.snake.collection.idd.core.VariableRanges;
import ru.snake.collection.idd.core.operation.Evaluate;
import ru.snake.collection.idd.core.operation.Quantify;
import ru.snake.collection.idd.core.util.VariableRange;

class QuantifyTest {

	private VariableOrder order;

	private IDDFactory factory;

	@BeforeEach
	void setUp() {
		order = new VariableOrder("x", "y");
		factory = new IDDFactory(order);
	}

	@Test
	@DisplayName("Exists on terminal returns itself")
	void testExistsTerminal() {
		assertSame(IDD.TRUE, Quantify.exists(factory, IDD.TRUE, "x"));
		assertSame(IDD.FALSE, Quantify.exists(factory, IDD.FALSE, "y"));
	}

	@Test
	@DisplayName("Exists eliminates the variable by OR-ing children")
	void testExistsEliminates() {
		// x in [1,10] AND y in [5,6]
		// Build y node first.
		IDD yNode = factory.buildFromIntervals(
			"y",
			List.of(new Edge(5, 6, factory.trueNode()))
		);
		// Build x node with yNode and FALSE children.
		IDD xNode = factory.getNode(
			0,
			List.of(new Edge(1, 10, yNode), new Edge(11, 20, IDD.FALSE))
		);

		// exists x: OR of children {yNode, FALSE} = yNode.
		IDD result = Quantify.exists(factory, xNode, "x");
		assertSame(yNode, result);
	}

	@Test
	@DisplayName("Exists on non-present variable returns the same IDD")
	void testExistsNonPresent() {
		IDD node = factory.buildFromIntervals(
			"x",
			List.of(new Edge(1, 5, factory.trueNode()))
		);
		assertSame(node, Quantify.exists(factory, node, "y"));
	}

	@Test
	@DisplayName("Forall with full-TRUE range returns TRUE")
	void testForallFullTrue() {
		// Every edge points to TRUE => AND of {TRUE} = TRUE.
		IDD node = factory.getNode(
			0,
			List.of(new Edge(Integer.MIN_VALUE, Integer.MAX_VALUE, IDD.TRUE))
		);
		// This should be eliminated to TRUE.
		assertSame(IDD.TRUE, node);
	}

	@Test
	@DisplayName("Forall with mixed children returns AND of children")
	void testForallMixedChildren() {
		IDD yNode = factory.buildFromIntervals(
			"y",
			List.of(new Edge(5, 6, factory.trueNode()))
		);
		IDD node = factory.getNode(
			0,
			List.of(new Edge(1, 5, yNode), new Edge(6, 10, IDD.FALSE))
		);

		// The gap-filling means children include FALSE for the gap edges.
		// AND of {yNode, FALSE, FALSE(gaps)} = FALSE.
		IDD result = Quantify.forall(factory, node, "x");
		assertSame(IDD.FALSE, result);
	}

	@Test
	@DisplayName(
		"Forall evaluation: universal quantification produces correct results"
	)
	void testForallEvaluation() {
		// f = (x in [1,5]) AND (y in [10,20])
		IDD xPart = factory.buildFromIntervals(
			"x",
			List.of(new Edge(1, 5, factory.trueNode()))
		);
		IDD yPart = factory.buildFromIntervals(
			"y",
			List.of(new Edge(10, 20, factory.trueNode()))
		);
		IDD f = factory.and(xPart, yPart);

		// forall x. f = AND over x's children = FALSE (because gaps are FALSE).
		IDD result = Quantify.forall(factory, f, "x");
		assertSame(IDD.FALSE, result);
	}

	@Test
	@DisplayName(
		"Exists evaluation: existential quantification produces correct results"
	)
	void testExistsEvaluation() {
		// f = (x in [1,5]) AND (y in [10,20])
		IDD xPart = factory.buildFromIntervals(
			"x",
			List.of(new Edge(1, 5, factory.trueNode()))
		);
		IDD yPart = factory.buildFromIntervals(
			"y",
			List.of(new Edge(10, 20, factory.trueNode()))
		);
		IDD f = factory.and(xPart, yPart);

		// exists x. f should give a y-based result.
		IDD result = Quantify.exists(factory, f, "x");
		assertTrue(Evaluate.evaluate(result, order, Map.of("x", 0, "y", 15)));
		assertFalse(Evaluate.evaluate(result, order, Map.of("x", 0, "y", 5)));
	}

	// ---- Tests with custom ranges ----

	@Test
	@DisplayName("Exists with custom range: eliminates ranged variable")
	void testExistsCustomRange() {
		VariableOrder rangedOrder = new VariableOrder("port", "proto");
		VariableRanges rangedRanges = new VariableRanges(
			Map.of(
				"port",
				VariableRange.of(0, 65535),
				"proto",
				VariableRange.of(0, 255)
			),
			rangedOrder
		);
		IDDFactory rangedFactory = new IDDFactory(rangedOrder, rangedRanges);

		// Build: proto in [6,17] as inner node
		IDD protoNode = rangedFactory.buildFromIntervals(
			"proto",
			List.of(new Edge(6, 17, rangedFactory.trueNode()))
		);
		// Build: port in [80,443] -> protoNode, port in [444,65535] -> FALSE
		IDD portNode = rangedFactory.getNode(
			0,
			List.of(
				new Edge(80, 443, protoNode),
				new Edge(444, 65535, IDD.FALSE)
			)
		);

		// exists port: OR of children {protoNode, FALSE, FALSE(gaps)} = protoNode.
		IDD result = Quantify.exists(rangedFactory, portNode, "port");
		assertSame(protoNode, result);
	}

	@Test
	@DisplayName("Forall with custom range: all edges TRUE in range")
	void testForallCustomRangeAllTrue() {
		VariableOrder rangedOrder = new VariableOrder("proto");
		VariableRanges rangedRanges = new VariableRanges(
			Map.of("proto", VariableRange.of(0, 255)),
			rangedOrder
		);
		IDDFactory rangedFactory = new IDDFactory(rangedOrder, rangedRanges);

		// Every edge in the range [0,255] points to TRUE => AND of {TRUE} = TRUE.
		IDD node = rangedFactory.getNode(
			0,
			List.of(new Edge(0, 255, IDD.TRUE))
		);
		// This should be reduced to TRUE.
		assertSame(IDD.TRUE, node);

		IDD result = Quantify.forall(rangedFactory, node, "proto");
		assertSame(IDD.TRUE, result);
	}

	@Test
	@DisplayName("Exists with custom range eliminates variable, producing TRUE")
	void testExistsCustomRangeEliminates() {
		VariableOrder rangedOrder = new VariableOrder("port");
		VariableRanges rangedRanges = new VariableRanges(
			Map.of("port", VariableRange.of(0, 65535)),
			rangedOrder
		);
		IDDFactory rangedFactory = new IDDFactory(rangedOrder, rangedRanges);

		// port in [80,443] -> TRUE, gap-filling: [0,79]->FALSE, [80,443]->TRUE, [444,65535]->FALSE
		IDD node = rangedFactory.buildFromIntervals(
			"port",
			List.of(new Edge(80, 443, rangedFactory.trueNode()))
		);

		// exists port: OR of {FALSE, TRUE, FALSE} = TRUE.
		IDD result = Quantify.exists(rangedFactory, node, "port");
		assertSame(IDD.TRUE, result);
	}

	@Test
	@DisplayName("Forall with custom range and mixed children")
	void testForallCustomRangeMixedChildren() {
		VariableOrder rangedOrder = new VariableOrder("port");
		VariableRanges rangedRanges = new VariableRanges(
			Map.of("port", VariableRange.of(0, 65535)),
			rangedOrder
		);
		IDDFactory rangedFactory = new IDDFactory(rangedOrder, rangedRanges);

		// port in [80,443]->TRUE, [444,65535]->FALSE.
		// Gap-filling: [0,79]->FALSE, [80,443]->TRUE, [444,65535]->FALSE.
		IDD node = rangedFactory.getNode(
			0,
			List.of(
				new Edge(80, 443, IDD.TRUE),
				new Edge(444, 65535, IDD.FALSE)
			)
		);

		// forall port: AND of {FALSE, TRUE, FALSE} = FALSE.
		IDD result = Quantify.forall(rangedFactory, node, "port");
		assertSame(IDD.FALSE, result);
	}
}
