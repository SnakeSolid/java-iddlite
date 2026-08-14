package ru.snake.collection.idd.unit;

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
import ru.snake.collection.idd.operation.Apply;
import ru.snake.collection.idd.operation.Evaluate;
import ru.snake.collection.idd.util.VariableRange;

class ApplyTest {

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
		assertSame(IDD.TRUE, Apply.and(factory, IDD.TRUE, IDD.TRUE));
	}

	@Test
	@DisplayName("AND of TRUE and FALSE is FALSE")
	void testAndTrueFalse() {
		assertSame(IDD.FALSE, Apply.and(factory, IDD.TRUE, IDD.FALSE));
	}

	@Test
	@DisplayName("OR of FALSE and FALSE is FALSE")
	void testOrFalseFalse() {
		assertSame(IDD.FALSE, Apply.or(factory, IDD.FALSE, IDD.FALSE));
	}

	@Test
	@DisplayName("OR of TRUE and FALSE is TRUE")
	void testOrTrueFalse() {
		assertSame(IDD.TRUE, Apply.or(factory, IDD.TRUE, IDD.FALSE));
	}

	@Test
	@DisplayName("NOT of TRUE is FALSE")
	void testNotTrue() {
		assertSame(IDD.FALSE, Apply.not(factory, IDD.TRUE));
	}

	@Test
	@DisplayName("NOT of FALSE is TRUE")
	void testNotFalse() {
		assertSame(IDD.TRUE, Apply.not(factory, IDD.FALSE));
	}

	@Test
	@DisplayName("XOR of TRUE and FALSE is TRUE")
	void testXorTrueFalse() {
		assertSame(IDD.TRUE, Apply.xor(factory, IDD.TRUE, IDD.FALSE));
	}

	@Test
	@DisplayName("XOR of TRUE and TRUE is FALSE")
	void testXorTrueTrue() {
		assertSame(IDD.FALSE, Apply.xor(factory, IDD.TRUE, IDD.TRUE));
	}

	@Test
	@DisplayName("Implies: TRUE -> FALSE is FALSE")
	void testImpliesTrueFalse() {
		assertSame(IDD.FALSE, Apply.implies(factory, IDD.TRUE, IDD.FALSE));
	}

	@Test
	@DisplayName("Implies: FALSE -> anything is TRUE")
	void testImpliesFalseAny() {
		assertSame(IDD.TRUE, Apply.implies(factory, IDD.FALSE, IDD.FALSE));
		assertSame(IDD.TRUE, Apply.implies(factory, IDD.FALSE, IDD.TRUE));
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
		IDD result = Apply.and(factory, f, g);

		// Should be TRUE only for [5, 10].
		assertTrue(Evaluate.evaluate(result, order, Map.of("x", 7)));
		assertFalse(Evaluate.evaluate(result, order, Map.of("x", 3)));
		assertFalse(Evaluate.evaluate(result, order, Map.of("x", 12)));
	}

	@Test
	@DisplayName("OR of complements yields TRUE")
	void testOrComplementTrue() {
		IDD f = factory.buildFromIntervals(
			"x",
			List.of(new Edge(1, 10, factory.trueNode()))
		);
		IDD notF = Apply.not(factory, f);
		IDD result = Apply.or(factory, f, notF);
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
		IDD left = Apply.not(factory, Apply.or(factory, a, b));
		IDD right = Apply.and(
			factory,
			Apply.not(factory, a),
			Apply.not(factory, b)
		);

		// For a few values, check evaluation matches.
		for (int v = 0; v <= 10; v++) {
			boolean l = Evaluate.evaluate(left, order, Map.of("x", v));
			boolean r = Evaluate.evaluate(right, order, Map.of("x", v));
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
		IDD left = Apply.not(factory, Apply.and(factory, a, b));
		IDD right = Apply.or(
			factory,
			Apply.not(factory, a),
			Apply.not(factory, b)
		);

		for (int v = 0; v <= 10; v++) {
			boolean l = Evaluate.evaluate(left, order, Map.of("x", v));
			boolean r = Evaluate.evaluate(right, order, Map.of("x", v));
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
		IDD result = Apply.and(factory, f, g);

		assertTrue(Evaluate.evaluate(result, order, Map.of("x", 3, "y", 15)));
		assertFalse(Evaluate.evaluate(result, order, Map.of("x", 3, "y", 5)));
		assertFalse(Evaluate.evaluate(result, order, Map.of("x", 7, "y", 15)));
	}

	// ---- Tests with custom ranges ----

	@Test
	@DisplayName("AND with custom range: overlapping port intervals")
	void testAndWithCustomRange() {
		Map<String, VariableRange> ranges = Map.of(
			"port",
			VariableRange.of(0, 65535)
		);
		VariableOrder rangedOrder = new VariableOrder(ranges, "port");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		IDD f = rangedFactory.buildFromIntervals(
			"port",
			List.of(new Edge(80, 443, rangedFactory.trueNode()))
		);
		IDD g = rangedFactory.buildFromIntervals(
			"port",
			List.of(new Edge(400, 8080, rangedFactory.trueNode()))
		);
		IDD result = Apply.and(rangedFactory, f, g);

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
		Map<String, VariableRange> ranges = Map.of(
			"proto",
			VariableRange.of(0, 255)
		);
		VariableOrder rangedOrder = new VariableOrder(ranges, "proto");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		IDD f = rangedFactory.buildFromIntervals(
			"proto",
			List.of(new Edge(6, 17, rangedFactory.trueNode()))
		);
		IDD notF = Apply.not(rangedFactory, f);

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
		Map<String, VariableRange> ranges = Map.of(
			"port",
			VariableRange.of(0, 65535)
		);
		VariableOrder rangedOrder = new VariableOrder(ranges, "port");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		IDD f = rangedFactory.buildFromIntervals(
			"port",
			List.of(new Edge(100, 200, rangedFactory.trueNode()))
		);
		IDD notF = Apply.not(rangedFactory, f);
		IDD result = Apply.or(rangedFactory, f, notF);
		assertSame(IDD.TRUE, result);
	}

	@Test
	@DisplayName("AND across two ranged variables")
	void testAndTwoRangedVariables() {
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
			List.of(new Edge(80, 443, rangedFactory.trueNode()))
		);
		IDD protoPart = rangedFactory.buildFromIntervals(
			"proto",
			List.of(new Edge(6, 6, rangedFactory.trueNode()))
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
