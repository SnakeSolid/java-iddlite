package ru.snake.collection.idd.core.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.core.operation.Evaluate;

class StressTest {

	@Test
	@DisplayName("Apply AND/OR with many interval boundaries")
	void testManyBoundaries() {
		VariableOrder order = new VariableOrder("x");
		IDDFactory factory = new IDDFactory(order);

		Random rng = new Random(123);
		IDD a = IDD.FALSE, b = IDD.FALSE;

		for (int i = 0; i < 30; i++) {
			int lo = rng.nextInt(1000);
			int hi = Math.min(lo + rng.nextInt(50), 1000);
			a = factory.or(a, factory.buildFromIntervals("x", List.of(new Edge(lo, hi, factory.trueNode()))));
		}

		for (int i = 0; i < 30; i++) {
			int lo = rng.nextInt(1000);
			int hi = Math.min(lo + rng.nextInt(50), 1000);
			b = factory.or(b, factory.buildFromIntervals("x", List.of(new Edge(lo, hi, factory.trueNode()))));
		}

		IDD c = factory.and(a, b);
		assertNotNull(c);

		// Verify correctness: AND of the two unions is TRUE exactly where
		// both a and b are TRUE at the same point.
		for (int v = 0; v <= 1000; v++) {
			boolean aVal = Evaluate.evaluate(a, order, Map.of("x", v));
			boolean bVal = Evaluate.evaluate(b, order, Map.of("x", v));
			boolean cVal = Evaluate.evaluate(c, order, Map.of("x", v));
			assertEquals(aVal && bVal, cVal, "Mismatch at x=" + v);
		}
	}

	@Test
	@DisplayName("Apply NOT of complex IDD")
	void testNotStress() {
		VariableOrder order = new VariableOrder("x");
		IDDFactory factory = new IDDFactory(order);

		Random rng = new Random(456);
		IDD f = IDD.FALSE;
		for (int i = 0; i < 20; i++) {
			int lo = rng.nextInt(500);
			int hi = Math.min(lo + rng.nextInt(30), 500);
			f = factory.or(f, factory.buildFromIntervals("x", List.of(new Edge(lo, hi, factory.trueNode()))));
		}

		IDD notF = factory.not(f);
		// f OR NOT(f) should be TRUE.
		assertSame(IDD.TRUE, factory.or(f, notF));
	}
}
