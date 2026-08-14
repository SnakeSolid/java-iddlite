package ru.snake.collection.idd.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.operation.Apply;
import ru.snake.collection.idd.operation.Evaluate;
import ru.snake.collection.idd.operation.Quantify;
import ru.snake.collection.idd.operation.Restrict;
import ru.snake.collection.idd.util.VariableRange;

class RangedFirewallTest {

	@Test
	@DisplayName("Firewall rules with port and protocol ranges")
	void testRangedFirewall() {
		Map<String, VariableRange> ranges = Map.of(
			"src_port",
			VariableRange.of(0, 65535),
			"dst_port",
			VariableRange.of(0, 65535),
			"proto",
			VariableRange.of(0, 255)
		);
		VariableOrder order = new VariableOrder(
			ranges,
			"proto",
			"src_port",
			"dst_port"
		);
		IDDFactory factory = new IDDFactory(order);

		// Rule 1: TCP (6) to port 80
		IDD rule1 = factory
			.builder()
			.when("proto")
			.in(6, 6)
			.then(true)
			.when("dst_port")
			.in(80, 80)
			.then(true)
			.build();

		// Rule 2: TCP to port 443
		IDD rule2 = factory
			.builder()
			.when("proto")
			.in(6, 6)
			.then(true)
			.when("dst_port")
			.in(443, 443)
			.then(true)
			.build();

		// Rule 3: UDP (17) to ports 53 (DNS)
		IDD rule3 = factory
			.builder()
			.when("proto")
			.in(17, 17)
			.then(true)
			.when("dst_port")
			.in(53, 53)
			.then(true)
			.build();

		// Combined policy: any of the rules
		IDD policy = Apply.or(factory, Apply.or(factory, rule1, rule2), rule3);

		// TCP to port 80 should be accepted
		assertTrue(
			Evaluate.evaluate(
				policy,
				order,
				Map.of("proto", 6, "src_port", 12345, "dst_port", 80)
			)
		);

		// TCP to port 443 should be accepted
		assertTrue(
			Evaluate.evaluate(
				policy,
				order,
				Map.of("proto", 6, "src_port", 54321, "dst_port", 443)
			)
		);

		// UDP to port 53 should be accepted
		assertTrue(
			Evaluate.evaluate(
				policy,
				order,
				Map.of("proto", 17, "src_port", 9999, "dst_port", 53)
			)
		);

		// TCP to port 8080 should be rejected
		assertFalse(
			Evaluate.evaluate(
				policy,
				order,
				Map.of("proto", 6, "src_port", 12345, "dst_port", 8080)
			)
		);

		// UDP to port 80 should be rejected
		assertFalse(
			Evaluate.evaluate(
				policy,
				order,
				Map.of("proto", 17, "src_port", 12345, "dst_port", 80)
			)
		);

		// ICMP (1) should be rejected
		assertFalse(
			Evaluate.evaluate(
				policy,
				order,
				Map.of("proto", 1, "src_port", 0, "dst_port", 0)
			)
		);
	}

	@Test
	@DisplayName("Restrict fixes a variable and simplifies the diagram")
	void testRestrictRangedFirewall() {
		Map<String, VariableRange> ranges = Map.of(
			"port",
			VariableRange.of(0, 65535),
			"proto",
			VariableRange.of(0, 255)
		);
		VariableOrder order = new VariableOrder(ranges, "proto", "port");
		IDDFactory factory = new IDDFactory(order);

		IDD policy = factory
			.builder()
			.when("proto")
			.in(6, 6)
			.then(true)
			.when("port")
			.in(80, 443)
			.then(true)
			.build();

		// Restrict proto=6 => should simplify to port-based rule
		IDD restricted = Restrict.restrict(factory, policy, "proto", 6);

		// After restricting proto=6, the policy depends only on port.
		assertTrue(
			Evaluate.evaluate(restricted, order, Map.of("proto", 6, "port", 80))
		);
		assertFalse(
			Evaluate.evaluate(
				restricted,
				order,
				Map.of("proto", 6, "port", 8080)
			)
		);
	}

	@Test
	@DisplayName("Quantify eliminates a ranged variable correctly")
	void testQuantifyRangedVariable() {
		Map<String, VariableRange> ranges = Map.of(
			"port",
			VariableRange.of(0, 65535),
			"proto",
			VariableRange.of(0, 255)
		);
		VariableOrder order = new VariableOrder(ranges, "proto", "port");
		IDDFactory factory = new IDDFactory(order);

		// f = (proto in [6,6]) AND (port in [80,443])
		IDD protoPart = factory.buildFromIntervals(
			"proto",
			List.of(new Edge(6, 6, factory.trueNode()))
		);
		IDD portPart = factory.buildFromIntervals(
			"port",
			List.of(new Edge(80, 443, factory.trueNode()))
		);
		IDD f = Apply.and(factory, protoPart, portPart);

		// exists port. f => proto in [6,6]
		IDD existsPort = Quantify.exists(factory, f, "port");
		assertTrue(
			Evaluate.evaluate(existsPort, order, Map.of("proto", 6, "port", 0))
		);
		assertFalse(
			Evaluate.evaluate(existsPort, order, Map.of("proto", 17, "port", 0))
		);

		// forall port. f => FALSE (because not all ports are in [80,443])
		IDD forallPort = Quantify.forall(factory, f, "port");
		assertSame(IDD.FALSE, forallPort);
	}

	@Test
	@DisplayName("NOT and OR complement with custom ranges")
	void testComplementWithCustomRanges() {
		Map<String, VariableRange> ranges = Map.of(
			"port",
			VariableRange.of(0, 65535)
		);
		VariableOrder order = new VariableOrder(ranges, "port");
		IDDFactory factory = new IDDFactory(order);

		IDD f = factory.buildFromIntervals(
			"port",
			List.of(new Edge(80, 443, factory.trueNode()))
		);
		IDD notF = Apply.not(factory, f);
		IDD tautology = Apply.or(factory, f, notF);

		// f OR NOT(f) must be TRUE for the entire variable range.
		assertSame(IDD.TRUE, tautology);
	}

	@Test
	@DisplayName("Single-value variable range")
	void testSingleValueRange() {
		Map<String, VariableRange> ranges = Map.of(
			"flag",
			VariableRange.of(0, 1)
		);
		VariableOrder order = new VariableOrder(ranges, "flag");
		IDDFactory factory = new IDDFactory(order);

		// Flag is TRUE only when it equals 1.
		IDD f = factory.buildFromIntervals(
			"flag",
			List.of(new Edge(1, 1, factory.trueNode()))
		);

		assertFalse(Evaluate.evaluate(f, order, Map.of("flag", 0)));
		assertTrue(Evaluate.evaluate(f, order, Map.of("flag", 1)));

		// NOT(f) is TRUE when flag == 0.
		IDD notF = Apply.not(factory, f);
		assertTrue(Evaluate.evaluate(notF, order, Map.of("flag", 0)));
		assertFalse(Evaluate.evaluate(notF, order, Map.of("flag", 1)));

		// f OR NOT(f) covers the full range [0,1] => reduces to TRUE.
		assertSame(IDD.TRUE, Apply.or(factory, f, notF));
	}

	@Test
	@DisplayName(
		"Deterministic evaluation with ranged variables covers the full range"
	)
	void testDeterministicRangedEvaluations() {
		Map<String, VariableRange> ranges = Map.of(
			"port",
			VariableRange.of(0, 65535),
			"proto",
			VariableRange.of(0, 255)
		);
		VariableOrder order = new VariableOrder(ranges, "proto", "port");
		IDDFactory factory = new IDDFactory(order);

		// Build a port-only rule: accept ports 80-443
		IDD portRule = factory.buildFromIntervals(
			"port",
			List.of(new Edge(80, 443, factory.trueNode()))
		);
		// Build a proto-only rule: accept protocols 6, 17
		IDD protoRule = factory.buildFromIntervals(
			"proto",
			List.of(new Edge(6, 17, factory.trueNode()))
		);
		// AND them: accept when BOTH conditions are met
		IDD policy = Apply.and(factory, portRule, protoRule);

		// Test specific points in the space.
		// Port in range AND proto in range => TRUE
		assertTrue(
			Evaluate.evaluate(policy, order, Map.of("proto", 6, "port", 80))
		);
		assertTrue(
			Evaluate.evaluate(policy, order, Map.of("proto", 17, "port", 443))
		);
		assertTrue(
			Evaluate.evaluate(policy, order, Map.of("proto", 12, "port", 200))
		);

		// Port in range but proto out => FALSE
		assertFalse(
			Evaluate.evaluate(policy, order, Map.of("proto", 0, "port", 80))
		);
		assertFalse(
			Evaluate.evaluate(policy, order, Map.of("proto", 255, "port", 443))
		);

		// Proto in range but port out => FALSE
		assertFalse(
			Evaluate.evaluate(policy, order, Map.of("proto", 6, "port", 0))
		);
		assertFalse(
			Evaluate.evaluate(policy, order, Map.of("proto", 17, "port", 65535))
		);

		// Boundary of port range
		assertFalse(
			Evaluate.evaluate(policy, order, Map.of("proto", 6, "port", 79))
		);
		assertTrue(
			Evaluate.evaluate(policy, order, Map.of("proto", 6, "port", 80))
		);
		assertTrue(
			Evaluate.evaluate(policy, order, Map.of("proto", 6, "port", 443))
		);
		assertFalse(
			Evaluate.evaluate(policy, order, Map.of("proto", 6, "port", 444))
		);

		// Boundary of proto range
		assertFalse(
			Evaluate.evaluate(policy, order, Map.of("proto", 5, "port", 80))
		);
		assertTrue(
			Evaluate.evaluate(policy, order, Map.of("proto", 6, "port", 80))
		);
		assertTrue(
			Evaluate.evaluate(policy, order, Map.of("proto", 17, "port", 80))
		);
		assertFalse(
			Evaluate.evaluate(policy, order, Map.of("proto", 18, "port", 80))
		);

		// Stress: sweep a range of values.
		for (int proto = 0; proto <= 255; proto++) {
			for (int port : new int[] { 0, 79, 80, 443, 444, 65535 }) {
				boolean inProto = proto >= 6 && proto <= 17;
				boolean inPort = port >= 80 && port <= 443;
				boolean expected = inProto && inPort;
				assertEquals(
					expected,
					Evaluate.evaluate(
						policy,
						order,
						Map.of("proto", proto, "port", port)
					),
					"Mismatch at proto=" + proto + ", port=" + port
				);
			}
		}
	}

	@Test
	@DisplayName("Performance: many evaluations with ranged variables")
	void testRangedPerformance() {
		Map<String, VariableRange> ranges = Map.of(
			"src_port",
			VariableRange.of(0, 65535),
			"dst_port",
			VariableRange.of(0, 65535),
			"proto",
			VariableRange.of(0, 255)
		);
		VariableOrder order = new VariableOrder(
			ranges,
			"proto",
			"src_port",
			"dst_port"
		);
		IDDFactory factory = new IDDFactory(order);

		// Build a realistic-ish policy with multiple rules.
		IDD policy = factory.falseNode();

		// Allow TCP to common web ports
		IDD webRule = factory
			.builder()
			.when("proto")
			.in(6, 6)
			.then(true)
			.when("dst_port")
			.in(80, 443)
			.then(true)
			.build();
		policy = Apply.or(factory, policy, webRule);

		// Allow UDP DNS
		IDD dnsRule = factory
			.builder()
			.when("proto")
			.in(17, 17)
			.then(true)
			.when("dst_port")
			.in(53, 53)
			.then(true)
			.build();
		policy = Apply.or(factory, policy, dnsRule);

		// Allow TCP SSH
		IDD sshRule = factory
			.builder()
			.when("proto")
			.in(6, 6)
			.then(true)
			.when("dst_port")
			.in(22, 22)
			.then(true)
			.build();
		policy = Apply.or(factory, policy, sshRule);

		// Evaluate 50000 packets
		Random rand = new Random(12345);
		long start = System.currentTimeMillis();
		int accepted = 0;
		for (int i = 0; i < 50000; i++) {
			Map<String, Integer> pkt = new HashMap<>();
			pkt.put("proto", rand.nextInt(256));
			pkt.put("src_port", rand.nextInt(65536));
			pkt.put("dst_port", rand.nextInt(65536));

			if (Evaluate.evaluate(policy, order, pkt)) {
				accepted++;
			}
		}
		long elapsed = System.currentTimeMillis() - start;

		// Sanity: should accept and reject some packets.
		assertTrue(accepted > 0, "Should accept some packets");
		assertTrue(accepted < 50000, "Should reject some packets");
		// Should complete well within 30 seconds.
		assertTrue(
			elapsed < 30000,
			"Should complete in under 30s, took " + elapsed + "ms"
		);
	}
}
