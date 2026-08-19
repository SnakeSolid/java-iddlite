package ru.snake.collection.idd.firewall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.core.operation.Evaluate;

class FirewallBuilderTest {

	private IDDFactory factory;

	private VariableOrder order;

	private FirewallBuilder builder;

	@BeforeEach
	void setUp() {
		factory = FirewallVars.factory();
		order = factory.order();
		builder = new FirewallBuilder(factory);
	}

	@Test
	@DisplayName("Empty rules => always DROP")
	void testEmptyRules() {
		IDD policy = builder.build(List.of());
		assertSame(IDD.FALSE, policy);
	}

	@Test
	@DisplayName("Single ACCEPT rule matches its range")
	void testSingleAccept() {
		List<FirewallRule> rules = List.of(
			new FirewallRule(
				FirewallRule.Action.ACCEPT,
				0,
				null,
				null,
				null,
				new FirewallRule.Constraint(80, 80),
				new FirewallRule.Constraint(6, 6)
			)
		);
		IDD policy = builder.build(rules);

		// Should accept: tcp to port 80.
		assertTrue(
			Evaluate.evaluate(
				policy,
				order,
				Map.of("src_ip", 0, "dst_ip", 0, "src_port", 12345, "dst_port", 80, "proto", 6)
			)
		);

		// Should reject: tcp to port 443.
		assertFalse(
			Evaluate.evaluate(
				policy,
				order,
				Map.of("src_ip", 0, "dst_ip", 0, "src_port", 12345, "dst_port", 443, "proto", 6)
			)
		);
	}

	@Test
	@DisplayName("First-match-wins: earlier ACCEPT takes precedence over DROP")
	void testFirstMatchWinsAccept() {
		List<FirewallRule> rules = List.of(
			new FirewallRule(
				FirewallRule.Action.ACCEPT,
				0,
				null,
				null,
				null,
				new FirewallRule.Constraint(80, 80),
				new FirewallRule.Constraint(6, 6)
			),
			new FirewallRule(FirewallRule.Action.DROP, 1, null, null, null, null, new FirewallRule.Constraint(6, 6))
		);
		IDD policy = builder.build(rules);

		// Port 80 TCP — should be ACCEPT (rule 0 matches first).
		assertTrue(
			Evaluate.evaluate(
				policy,
				order,
				Map.of("src_ip", 0, "dst_ip", 0, "src_port", 12345, "dst_port", 80, "proto", 6)
			)
		);

		// Port 443 TCP — rule 0 doesn't match, rule 1 does => DROP.
		assertFalse(
			Evaluate.evaluate(
				policy,
				order,
				Map.of("src_ip", 0, "dst_ip", 0, "src_port", 12345, "dst_port", 443, "proto", 6)
			)
		);
	}

	@Test
	@DisplayName("First-match-wins: earlier DROP takes precedence over ACCEPT")
	void testFirstMatchWinsDrop() {
		List<FirewallRule> rules = List.of(
			new FirewallRule(
				FirewallRule.Action.DROP,
				0,
				new FirewallRule.Constraint(IpUtil.parseIp("192.168.0.0"), IpUtil.parseIp("192.168.255.255")),
				null,
				null,
				null,
				null
			),
			new FirewallRule(
				FirewallRule.Action.ACCEPT,
				1,
				null,
				null,
				null,
				new FirewallRule.Constraint(80, 80),
				new FirewallRule.Constraint(6, 6)
			)
		);
		IDD policy = builder.build(rules);

		// 192.168.1.10 -> port 80 TCP: rule 0 DROP wins first.
		assertFalse(
			Evaluate.evaluate(
				policy,
				order,
				Map.of(
					"src_ip",
					IpUtil.parseIp("192.168.1.10"),
					"dst_ip",
					0,
					"src_port",
					12345,
					"dst_port",
					80,
					"proto",
					6
				)
			)
		);

		// 10.0.0.5 -> port 80 TCP: rule 0 doesn't match, rule 1 ACCEPT.
		assertTrue(
			Evaluate.evaluate(
				policy,
				order,
				Map.of("src_ip", IpUtil.parseIp("10.0.0.5"), "dst_ip", 0, "src_port", 12345, "dst_port", 80, "proto", 6)
			)
		);
	}

	@Test
	@DisplayName("Catch-all DROP at the end")
	void testCatchAllDrop() {
		List<FirewallRule> rules = List.of(
			new FirewallRule(
				FirewallRule.Action.ACCEPT,
				0,
				null,
				null,
				null,
				new FirewallRule.Constraint(80, 80),
				new FirewallRule.Constraint(6, 6)
			),
			new FirewallRule(FirewallRule.Action.DROP, 1, null, null, null, null, null)
		);
		IDD policy = builder.build(rules);

		// Port 80 TCP => ACCEPT.
		assertTrue(
			Evaluate.evaluate(
				policy,
				order,
				Map.of("src_ip", 0, "dst_ip", 0, "src_port", 12345, "dst_port", 80, "proto", 6)
			)
		);

		// Port 443 TCP => no earlier rule matches, catch-all DROP.
		assertFalse(
			Evaluate.evaluate(
				policy,
				order,
				Map.of("src_ip", 0, "dst_ip", 0, "src_port", 12345, "dst_port", 443, "proto", 6)
			)
		);
	}

	@Test
	@DisplayName("Realistic firewall: web, DNS, SSH, then drop all")
	void testRealisticFirewall() {
		List<FirewallRule> rules = List.of(
			// Allow TCP to port 80.
			new FirewallRule(
				FirewallRule.Action.ACCEPT,
				0,
				null,
				null,
				null,
				new FirewallRule.Constraint(80, 80),
				new FirewallRule.Constraint(6, 6)
			),
			// Allow TCP to port 443.
			new FirewallRule(
				FirewallRule.Action.ACCEPT,
				1,
				null,
				null,
				null,
				new FirewallRule.Constraint(443, 443),
				new FirewallRule.Constraint(6, 6)
			),
			// Allow UDP DNS.
			new FirewallRule(
				FirewallRule.Action.ACCEPT,
				2,
				null,
				null,
				null,
				new FirewallRule.Constraint(53, 53),
				new FirewallRule.Constraint(17, 17)
			),
			// Allow TCP SSH.
			new FirewallRule(
				FirewallRule.Action.ACCEPT,
				3,
				null,
				null,
				null,
				new FirewallRule.Constraint(22, 22),
				new FirewallRule.Constraint(6, 6)
			),
			// Drop all.
			new FirewallRule(FirewallRule.Action.DROP, 4, null, null, null, null, null)
		);
		IDD policy = builder.build(rules);

		// HTTP — ACCEPT
		assertPacket(policy, 0, 0, 12345, 80, 6, true);
		// HTTPS — ACCEPT
		assertPacket(policy, 0, 0, 54321, 443, 6, true);
		// UDP DNS — ACCEPT
		assertPacket(policy, 0, 0, 9999, 53, 17, true);
		// TCP SSH — ACCEPT
		assertPacket(policy, 0, 0, 11111, 22, 6, true);
		// TCP to 8080 — DROP
		assertPacket(policy, 0, 0, 12345, 8080, 6, false);
		// UDP to 80 — DROP
		assertPacket(policy, 0, 0, 12345, 80, 17, false);
		// ICMP — DROP
		assertPacket(policy, 0, 0, 0, 0, 1, false);
	}

	@Test
	@DisplayName("IP-based DROP overrides later ACCEPT")
	void testIpBasedDrop() {
		int net10 = IpUtil.cidrNetwork("10.0.0.0/8");
		int brd10 = IpUtil.cidrBroadcast("10.0.0.0/8");

		List<FirewallRule> rules = List.of(
			// Block 10.0.0.0/8 entirely.
			new FirewallRule(
				FirewallRule.Action.DROP,
				0,
				new FirewallRule.Constraint(net10, brd10),
				null,
				null,
				null,
				null
			),
			// Allow everything else to port 80 TCP.
			new FirewallRule(
				FirewallRule.Action.ACCEPT,
				1,
				null,
				null,
				null,
				new FirewallRule.Constraint(80, 80),
				new FirewallRule.Constraint(6, 6)
			),
			new FirewallRule(FirewallRule.Action.DROP, 2, null, null, null, null, null)
		);
		IDD policy = builder.build(rules);

		// Source in 10.0.0.0/8 => DROP even though port 80 TCP.
		assertPacket(policy, IpUtil.parseIp("10.1.2.3"), 0, 12345, 80, 6, false);

		// Source outside 10.0.0.0/8, port 80 TCP => ACCEPT.
		assertPacket(policy, IpUtil.parseIp("192.168.1.1"), 0, 12345, 80, 6, true);
	}

	private void assertPacket(IDD policy, int srcIp, int dstIp, int srcPort, int dstPort, int proto, boolean expected) {
		boolean result = Evaluate.evaluate(
			policy,
			order,
			Map.of("src_ip", srcIp, "dst_ip", dstIp, "src_port", srcPort, "dst_port", dstPort, "proto", proto)
		);
		assertEquals(
			expected,
			result,
			"Mismatch for src_ip=" + srcIp + " dst_ip=" + dstIp + " src_port=" + srcPort + " dst_port=" + dstPort
					+ " proto=" + proto
		);
	}

	// Suppress unused — used in tests above.
	private static void assertTrue(boolean value) {
		assertEquals(true, value);
	}

	private static void assertFalse(boolean value) {
		assertEquals(false, value);
	}
}
