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
import ru.snake.collection.idd.util.VariableRange;

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
		IDD idd = factory
			.builder()
			.when("x")
			.in(1, 10)
			.then(true)
			.when("x")
			.in(11, 20)
			.then(false)
			.build();

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
	@DisplayName(
		"Canonicity: two builders producing the same IDD return the same object"
	)
	void testCanonicity() {
		IDD a = factory
			.builder()
			.when("x")
			.in(1, 5)
			.then(true)
			.when("x")
			.in(6, 10)
			.then(false)
			.build();
		IDD b = factory
			.builder()
			.when("x")
			.in(1, 5)
			.then(true)
			.when("x")
			.in(6, 10)
			.then(false)
			.build();
		assertSame(a, b);
	}

	@Test
	@DisplayName("Builder with custom ranges respects variable's range")
	void testBuilderWithCustomRange() {
		Map<String, VariableRange> ranges = Map.of(
			"port",
			VariableRange.of(0, 65535)
		);
		VariableOrder rangedOrder = new VariableOrder(ranges, "port");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		IDD idd = rangedFactory
			.builder()
			.when("port")
			.in(80, 443)
			.then(true)
			.when("port")
			.in(8080, 8443)
			.then(false)
			.build();

		assertTrue(Evaluate.evaluate(idd, rangedOrder, Map.of("port", 80)));
		assertTrue(Evaluate.evaluate(idd, rangedOrder, Map.of("port", 443)));
		assertFalse(Evaluate.evaluate(idd, rangedOrder, Map.of("port", 8080)));
		// Outside all specified intervals — but still in range.
		assertFalse(Evaluate.evaluate(idd, rangedOrder, Map.of("port", 0)));
		assertFalse(Evaluate.evaluate(idd, rangedOrder, Map.of("port", 65535)));
	}

	@Test
	@DisplayName("Builder with custom range at boundaries")
	void testBuilderRangeBoundaries() {
		Map<String, VariableRange> ranges = Map.of(
			"proto",
			VariableRange.of(0, 255)
		);
		VariableOrder rangedOrder = new VariableOrder(ranges, "proto");
		IDDFactory rangedFactory = new IDDFactory(rangedOrder);

		IDD idd = rangedFactory
			.builder()
			.when("proto")
			.in(0, 127)
			.then(true)
			.build();

		assertTrue(Evaluate.evaluate(idd, rangedOrder, Map.of("proto", 0)));
		assertTrue(Evaluate.evaluate(idd, rangedOrder, Map.of("proto", 127)));
		assertFalse(Evaluate.evaluate(idd, rangedOrder, Map.of("proto", 128)));
		assertFalse(Evaluate.evaluate(idd, rangedOrder, Map.of("proto", 255)));
	}
}
