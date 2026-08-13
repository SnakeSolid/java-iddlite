package ru.snake.collection.idd.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.util.Formatters;
import ru.snake.collection.idd.util.IDDPrinter;
import ru.snake.collection.idd.util.ValueFormatter;

class IDDPrinterTest {

	private VariableOrder order;
	private IDDFactory factory;

	@BeforeEach
	void setUp() {
		order = new VariableOrder("x", "y", "z");
		factory = new IDDFactory(order);
	}

	@Test
	@DisplayName("print(terminal TRUE) outputs TRUE")
	void testPrintTrue() {
		String result = IDDPrinter.print(IDD.TRUE, order);
		assertTrue(result.contains("TRUE"));
	}

	@Test
	@DisplayName("print(terminal FALSE) outputs FALSE")
	void testPrintFalse() {
		String result = IDDPrinter.print(IDD.FALSE, order);
		assertTrue(result.contains("FALSE"));
	}

	@Test
	@DisplayName("print(simple node) shows variable and edges")
	void testPrintSimple() {
		// x: [0,0]->FALSE, [1,10]->TRUE
		// Factory normalizes: gaps filled with FALSE edges covering full
		// integer range
		IDD f = factory.getNode(0, List.of(new Edge(0, 0, IDD.FALSE), new Edge(1, 10, IDD.TRUE)));

		String result = IDDPrinter.print(f, order);

		assertNotNull(result);
		assertTrue(result.contains("var=x"), "Should contain variable name: " + result);
		assertTrue(result.contains("[1,10]"), "Should contain interval [1,10]: " + result);
		assertTrue(result.contains("TRUE"), "Should contain TRUE: " + result);
		assertTrue(result.contains("FALSE"), "Should contain FALSE: " + result);
	}

	@Test
	@DisplayName("print(nested IDD) shows hierarchy")
	void testPrintNested() {
		// x -> y: x has one edge to a y node
		IDD yNode = factory.getNode(1, List.of(new Edge(0, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE)));
		IDD xNode = factory.getNode(0, List.of(new Edge(0, 10, yNode)));

		String result = IDDPrinter.print(xNode, order);

		assertNotNull(result);
		assertTrue(result.contains("var=x"), "Should contain variable x: " + result);
		assertTrue(result.contains("var=y"), "Should contain variable y: " + result);
	}

	@Test
	@DisplayName("print(shared node) labels the node and references it")
	void testPrintSharedNode() {
		// Create a shared child: z node referenced from two edges of y
		IDD zNode = factory.getNode(2, List.of(new Edge(0, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE)));
		IDD yNode = factory.getNode(1, List.of(new Edge(0, 5, zNode), new Edge(6, 10, zNode)));

		String result = IDDPrinter.print(yNode, order);

		assertNotNull(result);
		assertTrue(result.contains("var=y"), "Should contain variable y: " + result);
		// z should appear (either printed or referenced)
		assertTrue(result.contains("var=z") || result.contains("@"), "Should show z or reference: " + result);
	}

	@Test
	@DisplayName("printCompact(terminal) outputs single line")
	void testPrintCompactTerminal() {
		String result = IDDPrinter.printCompact(IDD.TRUE, order);
		assertTrue(result.contains("TRUE"));
		assertEquals(1, result.lines().count(), "Terminal should be one line: " + result);
	}

	@Test
	@DisplayName("printCompact(simple node) outputs compact format")
	void testPrintCompactSimple() {
		IDD f = factory.getNode(0, List.of(new Edge(1, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE)));

		String result = IDDPrinter.printCompact(f, order);

		assertNotNull(result);
		assertTrue(result.contains("var=x"), "Should contain variable: " + result);
		assertTrue(result.contains("[1,5]"), "Should contain interval: " + result);
	}

	@Test
	@DisplayName("printTree(simple node) outputs tree diagram")
	void testPrintTreeSimple() {
		IDD f = factory.getNode(0, List.of(new Edge(0, 0, IDD.FALSE), new Edge(1, 10, IDD.TRUE)));

		String result = IDDPrinter.printTree(f, order);

		assertNotNull(result);
		assertTrue(result.contains("x"), "Should contain variable x: " + result);
		// Should contain box-drawing or arrow characters
		assertTrue(
			result.contains("─") || result.contains("->") || result.contains("ARROW"),
			"Should contain connector characters: " + result
		);
	}

	@Test
	@DisplayName("printTree(nested IDD) outputs hierarchical tree")
	void testPrintTreeNested() {
		IDD yNode = factory.getNode(1, List.of(new Edge(0, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE)));
		IDD xNode = factory.getNode(0, List.of(new Edge(0, 10, yNode)));

		String result = IDDPrinter.printTree(xNode, order);

		assertNotNull(result);
		assertTrue(result.contains("x"), "Should contain variable x: " + result);
		assertTrue(result.contains("y"), "Should contain variable y: " + result);
	}

	@Test
	@DisplayName("print(formatter) delegates to raw by default")
	void testPrintDefaultFormatter() {
		IDD f = factory.getNode(0, List.of(new Edge(1, 5, IDD.TRUE)));

		String without = IDDPrinter.print(f, order);
		String withRaw = IDDPrinter.print(f, order, ValueFormatter.RAW);

		assertEquals(without, withRaw, "Default formatter should equal RAW");
	}

	@Test
	@DisplayName("printTree with custom formatter formats intervals")
	void testPrintTreeCustomFormatter() {
		VariableOrder firewallOrder = new VariableOrder("src_ip", "protocol");
		IDDFactory fwFactory = new IDDFactory(firewallOrder);

		// Simulate an IP range: 10.0.0.1 to 10.0.0.2
		int ip1 = (10 << 24) | (0 << 16) | (0 << 8) | 1;
		int ip2 = (10 << 24) | (0 << 16) | (0 << 8) | 2;
		IDD f = fwFactory
			.getNode(0, List.of(new Edge(ip1, ip2, IDD.TRUE), new Edge(ip2 + 1, Integer.MAX_VALUE, IDD.FALSE)));

		// Default: raw integers
		String raw = IDDPrinter.printTree(f, firewallOrder);
		assertTrue(raw.contains(Integer.toString(ip1)), "Raw output should contain raw IP: " + raw);

		// With formatter: dotted-decimal
		ValueFormatter formatter = Formatters.builder(firewallOrder).forIndex("src_ip", Formatters.ipv4()).build();
		String formatted = IDDPrinter.printTree(f, firewallOrder, formatter);
		assertTrue(formatted.contains("10.0.0.1"), "Formatted output should contain dotted-decimal IP: " + formatted);
	}

	@Test
	@DisplayName("print with protocol formatter converts numbers to names")
	void testPrintProtocolFormatter() {
		VariableOrder firewallOrder = new VariableOrder("protocol");
		IDDFactory fwFactory = new IDDFactory(firewallOrder);

		// TCP=6, UDP=17
		IDD f = fwFactory.getNode(0, List.of(new Edge(6, 6, IDD.TRUE), new Edge(17, 17, IDD.TRUE)));

		ValueFormatter formatter = Formatters.builder(firewallOrder)
			.forIndex("protocol", Formatters.ipProtocol())
			.build();
		String formatted = IDDPrinter.print(f, firewallOrder, formatter);

		assertTrue(formatted.contains("TCP"), "Should contain protocol name TCP: " + formatted);
		assertTrue(formatted.contains("UDP"), "Should contain protocol name UDP: " + formatted);
	}

	@Test
	@DisplayName("printCompact with custom formatter formats intervals")
	void testPrintCompactCustomFormatter() {
		VariableOrder fwOrder = new VariableOrder("dst_port");
		IDDFactory fwFactory = new IDDFactory(fwOrder);

		// Ports 80 and 443
		IDD f = fwFactory.getNode(0, List.of(new Edge(80, 80, IDD.TRUE), new Edge(443, 443, IDD.TRUE)));

		ValueFormatter formatter = Formatters.builder(fwOrder).forIndex("dst_port", Formatters.port()).build();
		String formatted = IDDPrinter.printCompact(f, fwOrder, formatter);

		assertTrue(formatted.contains("http"), "Should contain port name http: " + formatted);
		assertTrue(formatted.contains("https"), "Should contain port name https: " + formatted);
	}

	@Test
	@DisplayName("Formatters.ipv4() converts packed integer to dotted-decimal")
	void testIpv4Formatter() {
		var formatter = Formatters.ipv4();

		// 192.168.1.1
		int ip = (192 << 24) | (168 << 16) | (1 << 8) | 1;
		assertEquals("192.168.1.1", formatter.apply(ip));

		// 10.0.0.0
		ip = (10 << 24) | (0 << 16) | (0 << 8) | 0;
		assertEquals("10.0.0.0", formatter.apply(ip));

		// 255.255.255.255
		ip = (255 << 24) | (255 << 16) | (255 << 8) | 255;
		assertEquals("255.255.255.255", formatter.apply(ip));
	}

	@Test
	@DisplayName("Formatters.ipv4() handles negative (sign-extended) values")
	void testIpv4FormatterNegative() {
		var formatter = Formatters.ipv4();

		// 192.168.0.0 is negative when sign-extended: 0xC0A80000
		int ip = (192 << 24) | (168 << 16);
		// The raw int is negative because bit 31 is set
		assertTrue(ip < 0, "This IP should be negative as a signed int");
		assertEquals("192.168.0.0", formatter.apply(ip));
	}

	@Test
	@DisplayName("Formatters.ipProtocol() resolves well-known protocols")
	void testIpProtocolFormatter() {
		var formatter = Formatters.ipProtocol();

		assertEquals("TCP", formatter.apply(6));
		assertEquals("UDP", formatter.apply(17));
		assertEquals("ICMP", formatter.apply(1));
		// Unknown protocol falls back to number
		assertEquals("99", formatter.apply(99));
	}

	@Test
	@DisplayName("Formatters.port() resolves well-known ports")
	void testPortFormatter() {
		var formatter = Formatters.port();

		assertEquals("http", formatter.apply(80));
		assertEquals("ssh", formatter.apply(22));
		assertEquals("dns", formatter.apply(53));
		assertEquals("https", formatter.apply(443));
		// Unknown port falls back to number
		assertEquals("31337", formatter.apply(31337));
	}

	@Test
	@DisplayName("print(none of the standard terminals) handles all edges")
	void testPrintAllEdges() {
		// Three edges covering the full range
		IDD f = factory.getNode(
			0,
			List.of(
				new Edge(Integer.MIN_VALUE, -1, IDD.FALSE),
				new Edge(0, 0, IDD.TRUE),
				new Edge(1, Integer.MAX_VALUE, IDD.FALSE)
			)
		);

		String result = IDDPrinter.print(f, order);

		assertNotNull(result);
		assertTrue(result.contains("var=x"), "Should contain variable: " + result);
		assertEquals(1, result.lines().filter(l -> l.contains("var=x")).count(), "Should have exactly one var=x line");
	}
}
