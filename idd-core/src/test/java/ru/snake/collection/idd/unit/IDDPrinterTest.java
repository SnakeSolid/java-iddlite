package ru.snake.collection.idd.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.util.IDDPrinter;

class IDDPrinterTest {

	private VariableOrder order;
	private IDDFactory factory;

	@BeforeEach
	void setUp() {
		order = new VariableOrder("x", "y", "z");
		factory = new IDDFactory(order);
	}

	@Test
	@DisplayName("print(terminal TRUE) outputs TRUE")
	void testPrintTrue() {
		String result = IDDPrinter.print(IDD.TRUE, order);
		assertTrue(result.contains("TRUE"));
	}

	@Test
	@DisplayName("print(terminal FALSE) outputs FALSE")
	void testPrintFalse() {
		String result = IDDPrinter.print(IDD.FALSE, order);
		assertTrue(result.contains("FALSE"));
	}

	@Test
	@DisplayName("print(simple node) shows variable and edges")
	void testPrintSimple() {
		// x: [0,0]->FALSE, [1,10]->TRUE
		// Factory normalizes: gaps filled with FALSE edges covering full integer range
		IDD f = factory.getNode(
			0,
			List.of(new Edge(0, 0, IDD.FALSE), new Edge(1, 10, IDD.TRUE))
		);

		String result = IDDPrinter.print(f, order);

		assertNotNull(result);
		assertTrue(
			result.contains("var=x"),
			"Should contain variable name: " + result
		);
		assertTrue(
			result.contains("[1,10]"),
			"Should contain interval [1,10]: " + result
		);
		assertTrue(result.contains("TRUE"), "Should contain TRUE: " + result);
		assertTrue(result.contains("FALSE"), "Should contain FALSE: " + result);
	}

	@Test
	@DisplayName("print(nested IDD) shows hierarchy")
	void testPrintNested() {
		// x -> y: x has one edge to a y node
		IDD yNode = factory.getNode(
			1,
			List.of(new Edge(0, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE))
		);
		IDD xNode = factory.getNode(0, List.of(new Edge(0, 10, yNode)));

		String result = IDDPrinter.print(xNode, order);

		assertNotNull(result);
		assertTrue(
			result.contains("var=x"),
			"Should contain variable x: " + result
		);
		assertTrue(
			result.contains("var=y"),
			"Should contain variable y: " + result
		);
	}

	@Test
	@DisplayName("print(shared node) labels the node and references it")
	void testPrintSharedNode() {
		// Create a shared child: z node referenced from two edges of y
		IDD zNode = factory.getNode(
			2,
			List.of(new Edge(0, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE))
		);
		IDD yNode = factory.getNode(
			1,
			List.of(new Edge(0, 5, zNode), new Edge(6, 10, zNode))
		);

		String result = IDDPrinter.print(yNode, order);

		assertNotNull(result);
		assertTrue(
			result.contains("var=y"),
			"Should contain variable y: " + result
		);
		// z should appear (either printed or referenced)
		assertTrue(
			result.contains("var=z") || result.contains("@"),
			"Should show z or reference: " + result
		);
	}

	@Test
	@DisplayName("printCompact(terminal) outputs single line")
	void testPrintCompactTerminal() {
		String result = IDDPrinter.printCompact(IDD.TRUE, order);
		assertTrue(result.contains("TRUE"));
		assertEquals(
			1,
			result.lines().count(),
			"Terminal should be one line: " + result
		);
	}

	@Test
	@DisplayName("printCompact(simple node) outputs compact format")
	void testPrintCompactSimple() {
		IDD f = factory.getNode(
			0,
			List.of(new Edge(1, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE))
		);

		String result = IDDPrinter.printCompact(f, order);

		assertNotNull(result);
		assertTrue(
			result.contains("var=x"),
			"Should contain variable: " + result
		);
		assertTrue(
			result.contains("[1,5]"),
			"Should contain interval: " + result
		);
	}

	@Test
	@DisplayName("printTree(simple node) outputs tree diagram")
	void testPrintTreeSimple() {
		IDD f = factory.getNode(
			0,
			List.of(new Edge(0, 0, IDD.FALSE), new Edge(1, 10, IDD.TRUE))
		);

		String result = IDDPrinter.printTree(f, order);

		assertNotNull(result);
		assertTrue(
			result.contains("x"),
			"Should contain variable x: " + result
		);
		// Should contain box-drawing or arrow characters
		assertTrue(
			result.contains("─") ||
				result.contains("->") ||
				result.contains("ARROW"),
			"Should contain connector characters: " + result
		);
	}

	@Test
	@DisplayName("printTree(nested IDD) outputs hierarchical tree")
	void testPrintTreeNested() {
		IDD yNode = factory.getNode(
			1,
			List.of(new Edge(0, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE))
		);
		IDD xNode = factory.getNode(0, List.of(new Edge(0, 10, yNode)));

		String result = IDDPrinter.printTree(xNode, order);

		assertNotNull(result);
		assertTrue(
			result.contains("x"),
			"Should contain variable x: " + result
		);
		assertTrue(
			result.contains("y"),
			"Should contain variable y: " + result
		);
	}

	@Test
	@DisplayName("print(none of the standard terminals) handles all edges")
	void testPrintAllEdges() {
		// Three edges covering the full range
		IDD f = factory.getNode(
			0,
			List.of(
				new Edge(Integer.MIN_VALUE, -1, IDD.FALSE),
				new Edge(0, 0, IDD.TRUE),
				new Edge(1, Integer.MAX_VALUE, IDD.FALSE)
			)
		);

		String result = IDDPrinter.print(f, order);

		assertNotNull(result);
		assertTrue(
			result.contains("var=x"),
			"Should contain variable: " + result
		);
		assertEquals(
			1,
			result
				.lines()
				.filter(l -> l.contains("var=x"))
				.count(),
			"Should have exactly one var=x line"
		);
	}
}
