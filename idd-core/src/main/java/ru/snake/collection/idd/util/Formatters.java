package ru.snake.collection.idd.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

import ru.snake.collection.idd.core.VariableOrder;

/**
 * Pre-built {@link ValueFormatter} implementations and a fluent builder for
 * composing per-variable formatters.
 * <p>
 * Common formatters:
 * <ul>
 * <li>{@link #ipv4()} — converts packed integer to dotted-decimal (e.g.
 * <code>10.0.0.1</code>)</li>
 * <li>{@link #ipProtocol()} — converts IP protocol number to name (e.g.
 * <code>TCP</code>, <code>UDP</code>)</li>
 * </ul>
 * <p>
 * Typical usage:
 * 
 * <pre>
 * ValueFormatter formatter = Formatters.builder()
 * 	.forIndex("src_ip", ipv4())
 * 	.forIndex("dst_ip", ipv4())
 * 	.forIndex("protocol", ipProtocol())
 * 	.build();
 *
 * String pretty = IDDPrinter.printTree(firewall, order, formatter);
 * </pre>
 */
public final class Formatters {

	private Formatters() {
	}

	// ==================================================================
	// Built-in formatters
	// ==================================================================

	/**
	 * Formats a packed IPv4 integer as dotted-decimal notation.
	 * <p>
	 * The integer is expected in big-endian byte order:
	 * <code>a &lt;&lt; 24 | b &lt;&lt; 16 | c &lt;&lt; 8 | d</code>. Negative
	 * values (sign-extended) are displayed correctly by masking each octet with
	 * {@code 0xFF}.
	 *
	 * @return a formatter producing strings like <code>192.168.1.1</code>
	 */
	public static IntFunction<String> ipv4() {
		return value -> {
			int a = (value >> 24) & 0xFF;
			int b = (value >> 16) & 0xFF;
			int c = (value >> 8) & 0xFF;
			int d = (value >> 0) & 0xFF;
			return a + "." + b + "." + c + "." + d;
		};
	}

	/**
	 * Formats an IP protocol number as its IANA name.
	 * <p>
	 * Well-known protocols (RFC 1700 and common extensions) are resolved by
	 * name. Unknown numbers are returned as-is (e.g. <code>47</code>).
	 *
	 * @return a formatter producing strings like <code>TCP</code>,
	 *         <code>UDP</code>, <code>ICMP</code>
	 */
	public static IntFunction<String> ipProtocol() {
		return value -> PROTOCOL_NAMES.getOrDefault(value, Integer.toString(value));
	}

	/**
	 * Formats a TCP/UDP port number. For well-known ports (0-1023), returns the
	 * service name; for others, returns the number.
	 *
	 * @return a formatter producing strings like <code>http</code>,
	 *         <code>ssh</code>, <code>8080</code>
	 */
	public static IntFunction<String> port() {
		return value -> PORT_NAMES.getOrDefault(value, Integer.toString(value));
	}

	// ==================================================================
	// Well-known protocol numbers (RFC 1700 + common)
	// ==================================================================

	private static final Map<Integer, String> PROTOCOL_NAMES = new HashMap<>();

	static {
		PROTOCOL_NAMES.put(1, "ICMP");
		PROTOCOL_NAMES.put(2, "IGMP");
		PROTOCOL_NAMES.put(6, "TCP");
		PROTOCOL_NAMES.put(17, "UDP");
		PROTOCOL_NAMES.put(41, "IPv6");
		PROTOCOL_NAMES.put(47, "GRE");
		PROTOCOL_NAMES.put(50, "ESP");
		PROTOCOL_NAMES.put(51, "AH");
		PROTOCOL_NAMES.put(58, "ICMPv6");
		PROTOCOL_NAMES.put(89, "OSPF");
		PROTOCOL_NAMES.put(132, "SCTP");
		PROTOCOL_NAMES.put(137, "MPLS");
	}

	// ==================================================================
	// Well-known port names (selected services)
	// ==================================================================

	private static final Map<Integer, String> PORT_NAMES = new HashMap<>();

	static {
		PORT_NAMES.put(20, "ftp-data");
		PORT_NAMES.put(21, "ftp");
		PORT_NAMES.put(22, "ssh");
		PORT_NAMES.put(25, "smtp");
		PORT_NAMES.put(53, "dns");
		PORT_NAMES.put(67, "dhcp");
		PORT_NAMES.put(68, "dhcp");
		PORT_NAMES.put(69, "tftp");
		PORT_NAMES.put(80, "http");
		PORT_NAMES.put(110, "pop3");
		PORT_NAMES.put(119, "nntp");
		PORT_NAMES.put(123, "ntp");
		PORT_NAMES.put(143, "imap");
		PORT_NAMES.put(161, "snmp");
		PORT_NAMES.put(162, "snmp-trap");
		PORT_NAMES.put(389, "ldap");
		PORT_NAMES.put(443, "https");
		PORT_NAMES.put(465, "smtps");
		PORT_NAMES.put(514, "syslog");
		PORT_NAMES.put(5192, "aol");
		PORT_NAMES.put(587, "smtp-submission");
		PORT_NAMES.put(636, "ldaps");
		PORT_NAMES.put(993, "imaps");
		PORT_NAMES.put(995, "pop3s");
		PORT_NAMES.put(1433, "mssql");
		PORT_NAMES.put(1521, "oracle");
		PORT_NAMES.put(2049, "nfs");
		PORT_NAMES.put(3306, "mysql");
		PORT_NAMES.put(3389, "rdp");
		PORT_NAMES.put(5432, "postgresql");
		PORT_NAMES.put(5900, "vnc");
		PORT_NAMES.put(6379, "redis");
		PORT_NAMES.put(8080, "http-alt");
		PORT_NAMES.put(8443, "https-alt");
		PORT_NAMES.put(9200, "elasticsearch");
		PORT_NAMES.put(27017, "mongodb");
	}

	// ==================================================================
	// Fluent builder
	// ==================================================================

	/**
	 * Creates a new {@link Builder} for composing per-variable formatters.
	 * <p>
	 * Variable indices are resolved against the provided {@code order} at
	 * {@link Builder#build()} time, so the caller only provides variable names.
	 *
	 * @param order the variable order to resolve names against
	 * @return a new builder
	 */
	public static Builder builder(VariableOrder order) {
		return new Builder(order);
	}

	/**
	 * Fluent builder that maps variable names to value formatters.
	 */
	public static final class Builder {

		private final VariableOrder order;

		private final Map<Integer, IntFunction<String>> map = new HashMap<>();

		private Builder(VariableOrder order) {
			if (order == null) {
				throw new NullPointerException("VariableOrder must not be null");
			}

			this.order = order;
		}

		/**
		 * Registers a formatter for the given variable name.
		 *
		 * @param varName   the variable name (must exist in the order)
		 * @param formatter the formatter for this variable's values
		 * @return this builder
		 */
		public Builder forIndex(String varName, IntFunction<String> formatter) {
			int index = order.index(varName);
			map.put(index, formatter);
			return this;
		}

		/**
		 * Builds the composite formatter.
		 * <p>
		 * Variables not registered via {@link #forIndex(String, IntFunction)}
		 * fall back to raw integer display.
		 *
		 * @return the composite formatter
		 */
		public ValueFormatter build() {
			return ValueFormatter.composite(map);
		}
	}
}
