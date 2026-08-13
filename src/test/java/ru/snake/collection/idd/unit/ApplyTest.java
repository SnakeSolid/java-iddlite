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
		IDD f = factory.buildFromIntervals("x", List.of(new Edge(1, 10, factory.trueNode())));
		IDD g = factory.buildFromIntervals("x", List.of(new Edge(5, 15, factory.trueNode())));
		IDD result = Apply.and(factory, f, g);

		// Should be TRUE only for [5, 10].
		assertTrue(Evaluate.evaluate(result, order, Map.of("x", 7)));
		assertFalse(Evaluate.evaluate(result, order, Map.of("x", 3)));
		assertFalse(Evaluate.evaluate(result, order, Map.of("x", 12)));
	}

	@Test
	@DisplayName("OR of complements yields TRUE")
	void testOrComplementTrue() {
		IDD f = factory.buildFromIntervals("x", List.of(new Edge(1, 10, factory.trueNode())));
		IDD notF = Apply.not(factory, f);
		IDD result = Apply.or(factory, f, notF);
		assertSame(IDD.TRUE, result);
	}

	@Test
	@DisplayName("De Morgan: NOT(A OR B) == NOT(A) AND NOT(B)")
	void testDeMorgan1() {
		IDD a = factory.buildFromIntervals("x", List.of(new Edge(1, 5, factory.trueNode())));
		IDD b = factory.buildFromIntervals("x", List.of(new Edge(3, 7, factory.trueNode())));
		IDD left = Apply.not(factory, Apply.or(factory, a, b));
		IDD right = Apply.and(factory, Apply.not(factory, a), Apply.not(factory, b));

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
		IDD a = factory.buildFromIntervals("x", List.of(new Edge(1, 5, factory.trueNode())));
		IDD b = factory.buildFromIntervals("x", List.of(new Edge(3, 7, factory.trueNode())));
		IDD left = Apply.not(factory, Apply.and(factory, a, b));
		IDD right = Apply.or(factory, Apply.not(factory, a), Apply.not(factory, b));

		for (int v = 0; v <= 10; v++) {
			boolean l = Evaluate.evaluate(left, order, Map.of("x", v));
			boolean r = Evaluate.evaluate(right, order, Map.of("x", v));
			assertEquals(l, r, "Mismatch at x=" + v);
		}
	}

	@Test
	@DisplayName("AND across two variables: evaluation consistency")
	void testAndTwoVariables() {
		IDD f = factory.buildFromIntervals("x", List.of(new Edge(1, 5, factory.trueNode())));
		IDD g = factory.buildFromIntervals("y", List.of(new Edge(10, 20, factory.trueNode())));
		IDD result = Apply.and(factory, f, g);

		assertTrue(Evaluate.evaluate(result, order, Map.of("x", 3, "y", 15)));
		assertFalse(Evaluate.evaluate(result, order, Map.of("x", 3, "y", 5)));
		assertFalse(Evaluate.evaluate(result, order, Map.of("x", 7, "y", 15)));
	}
}
