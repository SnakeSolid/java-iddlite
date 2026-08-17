package ru.snake.collection.idd.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;

class StressTest {

	private VariableOrder order;

	private IDDFactory factory;

	@BeforeEach
	void setUp() {
		order = new VariableOrder("src_ip", "dst_ip", "src_port");
		factory = new IDDFactory(order);
	}

	@Test
	@DisplayName("Apply AND/OR with many interval boundaries")
	void testManyBoundaries() {
		VariableOrder localOrder = new VariableOrder("x");
		IDDFactory localFactory = new IDDFactory(localOrder);

		Random rng = new Random(123);
		IDD a = IDD.FALSE, b = IDD.FALSE;

		for (int i = 0; i < 30; i++) {
			int lo = rng.nextInt(1000);
			int hi = Math.min(lo + rng.nextInt(50), 1000);
			a = factory.or(a, localFactory.buildFromIntervals("x", List.of(new Edge(lo, hi, localFactory.trueNode()))));
		}

		for (int i = 0; i < 30; i++) {
			int lo = rng.nextInt(1000);
			int hi = Math.min(lo + rng.nextInt(50), 1000);
			b = factory.or(b, localFactory.buildFromIntervals("x", List.of(new Edge(lo, hi, localFactory.trueNode()))));
		}

		IDD c = factory.and(a, b);
		assertNotNull(c);
	}

	@Test
	@DisplayName("Apply NOT of complex IDD")
	void testNotStress() {
		VariableOrder localOrder = new VariableOrder("x");
		IDDFactory localFactory = new IDDFactory(localOrder);

		Random rng = new Random(456);
		IDD f = IDD.FALSE;
		for (int i = 0; i < 20; i++) {
			int lo = rng.nextInt(500);
			int hi = Math.min(lo + rng.nextInt(30), 500);
			f = factory.or(f, localFactory.buildFromIntervals("x", List.of(new Edge(lo, hi, localFactory.trueNode()))));
		}

		IDD notF = factory.not(f);
		// f OR NOT(f) should be TRUE.
		assertSame(IDD.TRUE, factory.or(f, notF));
	}
}
