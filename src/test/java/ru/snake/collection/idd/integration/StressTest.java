package ru.snake.collection.idd.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.operation.Apply;
import ru.snake.collection.idd.operation.Evaluate;

class StressTest {

	private VariableOrder order;
	private IDDFactory factory;

	@BeforeEach
	void setUp() {
		order = new VariableOrder("src_ip", "dst_ip", "src_port");
		factory = new IDDFactory(order);
	}

	@Test
	@DisplayName("Build a large firewall rule set and evaluate quickly")
	void testFirewallStress() {
		Random rng = new Random(42);
		IDD firewall = IDD.FALSE;

		// Build 50 rules.
		for (int i = 0; i < 50; i++) {
			int srcLow = rng.nextInt(0x01000000);
			int srcHigh = Math.min(srcLow + rng.nextInt(256), Integer.MAX_VALUE);
			int dstLow = rng.nextInt(0x01000000);
			int dstHigh = Math.min(dstLow + rng.nextInt(256), Integer.MAX_VALUE);

			IDD rule = factory.buildFromIntervals("src_ip", List.of(new Edge(srcLow, srcHigh, factory.trueNode())));
			rule = Apply.and(
				factory,
				rule,
				factory.buildFromIntervals("dst_ip", List.of(new Edge(dstLow, dstHigh, factory.trueNode())))
			);

			firewall = Apply.or(factory, firewall, rule);
		}

		long start = System.currentTimeMillis();
		// Evaluate 50000 packets.
		for (int i = 0; i < 50000; i++) {
			Evaluate.evaluate(
				firewall,
				order,
				Map.of(
					"src_ip",
					rng.nextInt(0x01000000),
					"dst_ip",
					rng.nextInt(0x01000000),
					"src_port",
					rng.nextInt(65535)
				)
			);
		}
		long elapsed = System.currentTimeMillis() - start;

		System.out.println("50000 evaluations in " + elapsed + "ms");
		assertTrue(elapsed < 30000, "Evaluation took too long: " + elapsed + "ms");
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
			a = Apply.or(
				localFactory,
				a,
				localFactory.buildFromIntervals("x", List.of(new Edge(lo, hi, localFactory.trueNode())))
			);
		}

		for (int i = 0; i < 30; i++) {
			int lo = rng.nextInt(1000);
			int hi = Math.min(lo + rng.nextInt(50), 1000);
			b = Apply.or(
				localFactory,
				b,
				localFactory.buildFromIntervals("x", List.of(new Edge(lo, hi, localFactory.trueNode())))
			);
		}

		IDD c = Apply.and(localFactory, a, b);
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
			f = Apply.or(
				localFactory,
				f,
				localFactory.buildFromIntervals("x", List.of(new Edge(lo, hi, localFactory.trueNode())))
			);
		}

		IDD notF = Apply.not(localFactory, f);
		// f OR NOT(f) should be TRUE.
		assertSame(IDD.TRUE, Apply.or(localFactory, f, notF));
	}
}
