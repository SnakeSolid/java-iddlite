package ru.snake.collection.idd.firewall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.core.operation.Evaluate;

class FirewallParserTest {

	@Test
	@DisplayName("Parse simple ACCEPT rule with proto and dst_port")
	void testSimpleAccept() throws Exception {
		String input = "ACCEPT proto=tcp dst_port=80\n";
		List<FirewallRule> rules = FirewallParser.parse(
			new StringReader(input)
		);
		assertEquals(1, rules.size());
		FirewallRule rule = rules.get(0);
		assertEquals(FirewallRule.Action.ACCEPT, rule.action());
		assertEquals(0, rule.sequence());
		assertNotNull(rule.proto());
		assertEquals(6, rule.proto().low());
		assertEquals(6, rule.proto().high());
		assertNotNull(rule.dstPort());
		assertEquals(80, rule.dstPort().low());
		assertEquals(80, rule.dstPort().high());
		// Unused fields are null.
		assertEquals(null, rule.srcIp());
		assertEquals(null, rule.dstIp());
		assertEquals(null, rule.srcPort());
	}

	@Test
	@DisplayName("Parse DROP rule with CIDR IP")
	void testDropCidrIp() throws Exception {
		String input = "DROP proto=udp dst_ip=10.0.0.0/8\n";
		List<FirewallRule> rules = FirewallParser.parse(
			new StringReader(input)
		);
		assertEquals(1, rules.size());
		FirewallRule rule = rules.get(0);
		assertEquals(FirewallRule.Action.DROP, rule.action());
		assertNotNull(rule.dstIp());
		assertEquals(IpUtil.cidrNetwork("10.0.0.0/8"), rule.dstIp().low());
		assertEquals(IpUtil.cidrBroadcast("10.0.0.0/8"), rule.dstIp().high());
	}

	@Test
	@DisplayName("Parse catch-all rule")
	void testCatchAll() throws Exception {
		String input = "DROP *\n";
		List<FirewallRule> rules = FirewallParser.parse(
			new StringReader(input)
		);
		FirewallRule rule = rules.get(0);
		assertEquals(FirewallRule.Action.DROP, rule.action());
		assertEquals(null, rule.srcIp());
		assertEquals(null, rule.dstIp());
		assertEquals(null, rule.srcPort());
		assertEquals(null, rule.dstPort());
		assertEquals(null, rule.proto());
	}

	@Test
	@DisplayName("Parse port range")
	void testPortRange() throws Exception {
		String input = "ACCEPT dst_port=8000-9000\n";
		List<FirewallRule> rules = FirewallParser.parse(
			new StringReader(input)
		);
		FirewallRule rule = rules.get(0);
		assertNotNull(rule.dstPort());
		assertEquals(8000, rule.dstPort().low());
		assertEquals(9000, rule.dstPort().high());
	}

	@Test
	@DisplayName("Skip comments and blank lines")
	void testComments() throws Exception {
		String input =
			"# This is a comment\n" +
			"\n" +
			"ACCEPT proto=tcp dst_port=80\n" +
			"# Another comment\n" +
			"DROP *\n";
		List<FirewallRule> rules = FirewallParser.parse(
			new StringReader(input)
		);
		assertEquals(2, rules.size());
	}

	@Test
	@DisplayName("Parse multiple rules preserves order")
	void testMultipleRules() throws Exception {
		String input =
			"ACCEPT proto=tcp dst_port=80\n" +
			"ACCEPT proto=tcp dst_port=443\n" +
			"DROP *\n";
		List<FirewallRule> rules = FirewallParser.parse(
			new StringReader(input)
		);
		assertEquals(3, rules.size());
		assertEquals(0, rules.get(0).sequence());
		assertEquals(1, rules.get(1).sequence());
		assertEquals(2, rules.get(2).sequence());
	}

	@Test
	@DisplayName("Protocol name mapping")
	void testProtoNames() throws Exception {
		String input = "ACCEPT proto=udp\n";
		List<FirewallRule> rules = FirewallParser.parse(
			new StringReader(input)
		);
		FirewallRule rule = rules.get(0);
		assertEquals(17, rule.proto().low());
	}

	@Test
	@DisplayName("Unknown action throws")
	void testUnknownAction() {
		assertThrows(IllegalArgumentException.class, () ->
			FirewallParser.parse(new StringReader("REJECT proto=tcp\n"))
		);
	}

	@Test
	@DisplayName("Unknown field throws")
	void testUnknownField() {
		assertThrows(IllegalArgumentException.class, () ->
			FirewallParser.parse(new StringReader("ACCEPT foo=bar\n"))
		);
	}

	@Test
	@DisplayName("Invalid port range throws")
	void testInvalidPortRange() {
		assertThrows(IllegalArgumentException.class, () ->
			FirewallParser.parse(
				new StringReader("ACCEPT dst_port=9000-8000\n")
			)
		);
	}

	@Test
	@DisplayName("Invalid IP format throws")
	void testInvalidIp() {
		assertThrows(IllegalArgumentException.class, () ->
			FirewallParser.parse(new StringReader("ACCEPT dst_ip=999.0.0.1\n"))
		);
	}

	@Test
	@DisplayName("CIDR 0.0.0.0/0 is treated as any")
	void testCidrZeroAsAny() throws Exception {
		String input = "ACCEPT src_ip=0.0.0.0/0 dst_port=80\n";
		List<FirewallRule> rules = FirewallParser.parse(
			new StringReader(input)
		);
		FirewallRule rule = rules.get(0);
		// 0.0.0.0/0 means any source IP — constraint should be null.
		assertEquals(null, rule.srcIp());
		assertEquals(80, rule.dstPort().low());
	}
}
