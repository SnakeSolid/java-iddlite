package ru.snake.collection.idd.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.operation.Evaluate;

class BuilderTest {

	private VariableOrder order;
	private IDDFactory factory;

	@BeforeEach
	void setUp() {
		order = new VariableOrder("x", "y");
		factory = new IDDFactory(order);
	}

	@Test
	@DisplayName("Builder creates a correct IDD")
	void testBasicBuilder() {
		IDD idd = factory.builder().when("x").in(1, 10).then(true).when("x").in(11, 20).then(false).build();

		assertFalse(idd.isTerminal());
		assertTrue(Evaluate.evaluate(idd, order, Map.of("x", 5)));
		assertFalse(Evaluate.evaluate(idd, order, Map.of("x", 15)));
	}

	@Test
	@DisplayName("Builder with no rules returns TRUE")
	void testEmptyBuilder() {
		IDD idd = factory.builder().build();
		assertSame(IDD.TRUE, idd);
	}

	@Test
	@DisplayName("Builder can produce TRUE-only IDD")
	void testTrueOnlyBuilder() {
		IDD idd = factory.builder().when("x").in(1, 10).then(true).build();

		assertTrue(Evaluate.evaluate(idd, order, Map.of("x", 5)));
		// Outside the specified interval, the gap-filling adds FALSE.
		assertFalse(Evaluate.evaluate(idd, order, Map.of("x", 0)));
	}

	@Test
	@DisplayName("Canonicity: two builders producing the same IDD return the same object")
	void testCanonicity() {
		IDD a = factory.builder().when("x").in(1, 5).then(true).when("x").in(6, 10).then(false).build();
		IDD b = factory.builder().when("x").in(1, 5).then(true).when("x").in(6, 10).then(false).build();
		assertSame(a, b);
	}
}
