package ru.snake.collection.idd.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import ru.snake.collection.idd.operation.Apply;
import ru.snake.collection.idd.operation.Evaluate;
import ru.snake.collection.idd.util.VariableRange;

class EvaluateTest {

	private VariableOrder order;
	private IDDFactory factory;

	@BeforeEach
	void setUp() {
		order = new VariableOrder("x", "y", "z");
		factory = new IDDFactory(order);
	}

	@Test
	@DisplayName("Evaluate TRUE terminal")
	void testEvaluateTrue() {
		assertTrue(Evaluate.evaluate(IDD.TRUE, order, Map.of()));
	}

	@Test
	@DisplayName("Evaluate FALSE terminal")
	void testEvaluateFalse() {
		assertFalse(Evaluate.evaluate(IDD.FALSE, order, Map.of()));
	}

	@Test
	@DisplayName("Evaluate simple interval")
	void testSimpleInterval() {
		IDD node = factory.buildFromIntervals(
			"x",
			List.of(new Edge(1, 10, factory.trueNode()))
		);

		assertTrue(Evaluate.evaluate(node, order, Map.of("x", 5)));
		assertFalse(Evaluate.evaluate(node, order, Map.of("x", 0)));
		assertFalse(Evaluate.evaluate(node, order, Map.of("x", 11)));
	}

	@Test
	@DisplayName("Evaluate multi-variable IDD")
	void testMultiVariable() {
		IDD xPart = factory.buildFromIntervals(
			"x",
			List.of(new Edge(1, 5, factory.trueNode()))
		);
		IDD yPart = factory.buildFromIntervals(
			"y",
			List.of(new Edge(10, 20, factory.trueNode()))
		);
		IDD combined = Apply.and(factory, xPart, yPart);

		assertTrue(Evaluate.evaluate(combined, order, Map.of("x", 3, "y", 15)));
		assertFalse(Evaluate.evaluate(combined, order, Map.of("x", 3, "y", 5)));
		assertFalse(
			Evaluate.evaluate(combined, order, Map.of("x", 7, "y", 15))
		);
	}

	@Test
	@DisplayName("Evaluate throws on missing variable")
	void testMissingVariable() {
		IDD node = factory.buildFromIntervals(
			"x",
			List.of(new Edge(1, 10, factory.trueNode()))
		);
		assertThrows(IllegalArgumentException.class, () ->
			Evaluate.evaluate(node, order, Map.of())
		);
	}

	@Test
	@DisplayName("Evaluate at extreme boundaries")
	void testExtremeBoundaries() {
		IDD node = factory.getNode(
			0,
			List.of(
				new Edge(Integer.MIN_VALUE, Integer.MAX_VALUE / 2, IDD.TRUE),
				new Edge(
					Integer.MAX_VALUE / 2 + 1,
					Integer.MAX_VALUE,
					IDD.FALSE
				)
			)
		);

		assertTrue(
			Evaluate.evaluate(node, order, Map.of("x", Integer.MIN_VALUE))
		);
		assertFalse(
			Evaluate.evaluate(node, order, Map.of("x", Integer.MAX_VALUE))
		);
	}

	// ---- Tests with custom ranges ----

	@Test
	@DisplayName("Evaluate with custom port range")
	void testEvaluateCustomPortRange() {
		Map<String, VariableRange> ranges = Map.of(
			"port",
			VariableRange.of(0, 65535)
		);
		VariableOrder rangedOrder = new VariableOrder(ranges, "port");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		IDD node = rangedFactory.buildFromIntervals(
			"port",
			List.of(new Edge(80, 443, rangedFactory.trueNode()))
		);

		assertTrue(Evaluate.evaluate(node, rangedOrder, Map.of("port", 80)));
		assertTrue(Evaluate.evaluate(node, rangedOrder, Map.of("port", 443)));
		assertTrue(Evaluate.evaluate(node, rangedOrder, Map.of("port", 200)));
		assertFalse(Evaluate.evaluate(node, rangedOrder, Map.of("port", 0)));
		assertFalse(
			Evaluate.evaluate(node, rangedOrder, Map.of("port", 65535))
		);
	}

	@Test
	@DisplayName("Evaluate with custom range at boundary values")
	void testEvaluateRangeBoundaries() {
		Map<String, VariableRange> ranges = Map.of(
			"proto",
			VariableRange.of(0, 255)
		);
		VariableOrder rangedOrder = new VariableOrder(ranges, "proto");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		IDD node = rangedFactory.getNode(
			0,
			List.of(new Edge(0, 127, IDD.TRUE), new Edge(128, 255, IDD.FALSE))
		);

		// Verify all critical boundary values.
		assertTrue(Evaluate.evaluate(node, rangedOrder, Map.of("proto", 0)));
		assertTrue(Evaluate.evaluate(node, rangedOrder, Map.of("proto", 127)));
		assertFalse(Evaluate.evaluate(node, rangedOrder, Map.of("proto", 128)));
		assertFalse(Evaluate.evaluate(node, rangedOrder, Map.of("proto", 255)));
	}

	@Test
	@DisplayName("Evaluate multi-variable with mixed ranges")
	void testEvaluateMixedRanges() {
		Map<String, VariableRange> ranges = Map.of(
			"port",
			VariableRange.of(0, 65535),
			"proto",
			VariableRange.of(0, 255)
		);
		VariableOrder rangedOrder = new VariableOrder(ranges, "port", "proto");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		IDD portPart = rangedFactory.buildFromIntervals(
			"port",
			List.of(new Edge(1, 1024, rangedFactory.trueNode()))
		);
		IDD protoPart = rangedFactory.buildFromIntervals(
			"proto",
			List.of(new Edge(6, 17, rangedFactory.trueNode()))
		);
		IDD combined = Apply.and(rangedFactory, portPart, protoPart);

		assertTrue(
			Evaluate.evaluate(
				combined,
				rangedOrder,
				Map.of("port", 80, "proto", 6)
			)
		);
		assertFalse(
			Evaluate.evaluate(
				combined,
				rangedOrder,
				Map.of("port", 80, "proto", 60)
			)
		);
		assertFalse(
			Evaluate.evaluate(
				combined,
				rangedOrder,
				Map.of("port", 8080, "proto", 6)
			)
		);
	}
}
