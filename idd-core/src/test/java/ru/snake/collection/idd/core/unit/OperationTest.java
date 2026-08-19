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
import ru.snake.collection.idd.core.util.VariableRange;

/**
 * Tests for IDD Boolean operations (and, or, xor, implies, not) on the factory.
 */
class OperationTest {

	private VariableOrder order;

	private IDDFactory factory;

	@BeforeEach
	void setUp() {
		order = new VariableOrder("x", "y");
		factory = new IDDFactory(order);
	}

	@Test
	@DisplayName("AND of TRUE and TRUE is TRUE")
	void testAndTrueTrue() {
		assertSame(IDD.TRUE, factory.and(IDD.TRUE, IDD.TRUE));
	}

	@Test
	@DisplayName("AND of TRUE and FALSE is FALSE")
	void testAndTrueFalse() {
		assertSame(IDD.FALSE, factory.and(IDD.TRUE, IDD.FALSE));
	}

	@Test
	@DisplayName("OR of FALSE and FALSE is FALSE")
	void testOrFalseFalse() {
		assertSame(IDD.FALSE, factory.or(IDD.FALSE, IDD.FALSE));
	}

	@Test
	@DisplayName("OR of TRUE and FALSE is TRUE")
	void testOrTrueFalse() {
		assertSame(IDD.TRUE, factory.or(IDD.TRUE, IDD.FALSE));
	}

	@Test
	@DisplayName("NOT of TRUE is FALSE")
	void testNotTrue() {
		assertSame(IDD.FALSE, factory.not(IDD.TRUE));
	}

	@Test
	@DisplayName("NOT of FALSE is TRUE")
	void testNotFalse() {
		assertSame(IDD.TRUE, factory.not(IDD.FALSE));
	}

	@Test
	@DisplayName("XOR of TRUE and FALSE is TRUE")
	void testXorTrueFalse() {
		assertSame(IDD.TRUE, factory.xor(IDD.TRUE, IDD.FALSE));
	}

	@Test
	@DisplayName("XOR of TRUE and TRUE is FALSE")
	void testXorTrueTrue() {
		assertSame(IDD.FALSE, factory.xor(IDD.TRUE, IDD.TRUE));
	}

	@Test
	@DisplayName("Implies: TRUE -> FALSE is FALSE")
	void testImpliesTrueFalse() {
		assertSame(IDD.FALSE, factory.implies(IDD.TRUE, IDD.FALSE));
	}

	@Test
	@DisplayName("Implies: FALSE -> anything is TRUE")
	void testImpliesFalseAny() {
		assertSame(IDD.TRUE, factory.implies(IDD.FALSE, IDD.FALSE));
		assertSame(IDD.TRUE, factory.implies(IDD.FALSE, IDD.TRUE));
	}

	@Test
	@DisplayName("AND of overlapping intervals: evaluation consistency")
	void testAndIntervals() {
		IDD f = factory.buildFromIntervals(
			"x",
			List.of(new Edge(1, 10, factory.trueNode()))
		);
		IDD g = factory.buildFromIntervals(
			"x",
			List.of(new Edge(5, 15, factory.trueNode()))
		);
		IDD result = factory.and(f, g);

		// Should be TRUE only for [5, 10].
		assertTrue(Evaluate.evaluate(result, order, Map.of("x", 7, "y", 0)));
		assertFalse(Evaluate.evaluate(result, order, Map.of("x", 3, "y", 0)));
		assertFalse(Evaluate.evaluate(result, order, Map.of("x", 12, "y", 0)));
	}

	@Test
	@DisplayName("OR of complements yields TRUE")
	void testOrComplementTrue() {
		IDD f = factory.buildFromIntervals(
			"x",
			List.of(new Edge(1, 10, factory.trueNode()))
		);
		IDD notF = factory.not(f);
		IDD result = factory.or(f, notF);
		assertSame(IDD.TRUE, result);
	}

	@Test
	@DisplayName("De Morgan: NOT(A OR B) == NOT(A) AND NOT(B)")
	void testDeMorgan1() {
		IDD a = factory.buildFromIntervals(
			"x",
			List.of(new Edge(1, 5, factory.trueNode()))
		);
		IDD b = factory.buildFromIntervals(
			"x",
			List.of(new Edge(3, 7, factory.trueNode()))
		);
		IDD left = factory.not(factory.or(a, b));
		IDD right = factory.and(factory.not(a), factory.not(b));

		// For a few values, check evaluation matches.
		for (int v = 0; v <= 10; v++) {
			boolean l = Evaluate.evaluate(left, order, Map.of("x", v, "y", 0));
			boolean r = Evaluate.evaluate(right, order, Map.of("x", v, "y", 0));
			assertEquals(l, r, "Mismatch at x=" + v);
		}
	}

	@Test
	@DisplayName("De Morgan: NOT(A AND B) == NOT(A) OR NOT(B)")
	void testDeMorgan2() {
		IDD a = factory.buildFromIntervals(
			"x",
			List.of(new Edge(1, 5, factory.trueNode()))
		);
		IDD b = factory.buildFromIntervals(
			"x",
			List.of(new Edge(3, 7, factory.trueNode()))
		);
		IDD left = factory.not(factory.and(a, b));
		IDD right = factory.or(factory.not(a), factory.not(b));

		for (int v = 0; v <= 10; v++) {
			boolean l = Evaluate.evaluate(left, order, Map.of("x", v, "y", 0));
			boolean r = Evaluate.evaluate(right, order, Map.of("x", v, "y", 0));
			assertEquals(l, r, "Mismatch at x=" + v);
		}
	}

	@Test
	@DisplayName("AND across two variables: evaluation consistency")
	void testAndTwoVariables() {
		IDD f = factory.buildFromIntervals(
			"x",
			List.of(new Edge(1, 5, factory.trueNode()))
		);
		IDD g = factory.buildFromIntervals(
			"y",
			List.of(new Edge(10, 20, factory.trueNode()))
		);
		IDD result = factory.and(f, g);

		assertTrue(Evaluate.evaluate(result, order, Map.of("x", 3, "y", 15)));
		assertFalse(Evaluate.evaluate(result, order, Map.of("x", 3, "y", 5)));
		assertFalse(Evaluate.evaluate(result, order, Map.of("x", 7, "y", 15)));
	}

	// ---- Tests with custom ranges ----

	@Test
	@DisplayName("AND with custom range: overlapping port intervals")
	void testAndWithCustomRange() {
		VariableOrder rangedOrder = new VariableOrder("port");
		VariableRanges rangedRanges = new VariableRanges(
			Map.of("port", VariableRange.of(0, 65535)),
			rangedOrder
		);
		IDDFactory rangedFactory = new IDDFactory(rangedOrder, rangedRanges);

		IDD f = rangedFactory.buildFromIntervals(
			"port",
			List.of(new Edge(80, 443, rangedFactory.trueNode()))
		);
		IDD g = rangedFactory.buildFromIntervals(
			"port",
			List.of(new Edge(400, 8080, rangedFactory.trueNode()))
		);
		IDD result = rangedFactory.and(f, g);

		// Overlap is [400, 443].
		assertTrue(Evaluate.evaluate(result, rangedOrder, Map.of("port", 443)));
		assertFalse(
			Evaluate.evaluate(result, rangedOrder, Map.of("port", 399))
		);
		assertFalse(
			Evaluate.evaluate(result, rangedOrder, Map.of("port", 444))
		);
	}

	@Test
	@DisplayName("NOT with custom range: complement is correct")
	void testNotWithCustomRange() {
		VariableOrder rangedOrder = new VariableOrder("proto");
		VariableRanges rangedRanges = new VariableRanges(
			Map.of("proto", VariableRange.of(0, 255)),
			rangedOrder
		);
		IDDFactory rangedFactory = new IDDFactory(rangedOrder, rangedRanges);

		IDD f = rangedFactory.buildFromIntervals(
			"proto",
			List.of(new Edge(6, 17, rangedFactory.trueNode()))
		);
		IDD notF = rangedFactory.not(f);

		// f is TRUE for [6,17], FALSE elsewhere in [0,255].
		// NOT(f) is FALSE for [6,17], TRUE elsewhere.
		assertFalse(Evaluate.evaluate(notF, rangedOrder, Map.of("proto", 6)));
		assertFalse(Evaluate.evaluate(notF, rangedOrder, Map.of("proto", 17)));
		assertTrue(Evaluate.evaluate(notF, rangedOrder, Map.of("proto", 0)));
		assertTrue(Evaluate.evaluate(notF, rangedOrder, Map.of("proto", 255)));
	}

	@Test
	@DisplayName("OR of complements with custom range yields TRUE")
	void testOrComplementCustomRange() {
		VariableOrder rangedOrder = new VariableOrder("port");
		VariableRanges rangedRanges = new VariableRanges(
			Map.of("port", VariableRange.of(0, 65535)),
			rangedOrder
		);
		IDDFactory rangedFactory = new IDDFactory(rangedOrder, rangedRanges);

		IDD f = rangedFactory.buildFromIntervals(
			"port",
			List.of(new Edge(100, 200, rangedFactory.trueNode()))
		);
		IDD notF = rangedFactory.not(f);
		IDD result = rangedFactory.or(f, notF);
		assertSame(IDD.TRUE, result);
	}

	@Test
	@DisplayName("AND across two ranged variables")
	void testAndTwoRangedVariables() {
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

		IDD portPart = rangedFactory.buildFromIntervals(
			"port",
			List.of(new Edge(80, 443, rangedFactory.trueNode()))
		);
		IDD protoPart = rangedFactory.buildFromIntervals(
			"proto",
			List.of(new Edge(6, 6, rangedFactory.trueNode()))
		);
		IDD combined = rangedFactory.and(portPart, protoPart);

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
				Map.of("port", 80, "proto", 17)
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
