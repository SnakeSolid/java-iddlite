package ru.snake.collection.idd.firewall;

import java.util.Map;

import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.util.VariableRange;

/**
 * Defines the five predefined firewall variables and their ranges.
 * <p>
 * Variable order in the IDD (top to bottom):
 * <ol>
 * <li>src_ip — IPv4 source address</li>
 * <li>dst_ip — IPv4 destination address</li>
 * <li>src_port — source port</li>
 * <li>dst_port — destination port</li>
 * <li>proto — IP protocol number</li>
 * </ol>
 */
public final class FirewallVars {

	/** IPv4 source address variable name. */
	public static final String SRC_IP = "src_ip";

	/** IPv4 destination address variable name. */
	public static final String DST_IP = "dst_ip";

	/** Source port variable name. */
	public static final String SRC_PORT = "src_port";

	/** Destination port variable name. */
	public static final String DST_PORT = "dst_port";

	/** IP protocol number variable name. */
	public static final String PROTO = "proto";

	/** Minimum port value. */
	public static final int PORT_MIN = 0;

	/** Maximum port value. */
	public static final int PORT_MAX = 65535;

	/** Minimum protocol number. */
	public static final int PROTO_MIN = 0;

	/** Maximum protocol number. */
	public static final int PROTO_MAX = 255;

	/** Minimum IPv4 address (0.0.0.0) as signed int. */
	public static final int IP_MIN = Integer.MIN_VALUE;

	/** Maximum IPv4 address (255.255.255.255) as signed int. */
	public static final int IP_MAX = Integer.MAX_VALUE;

	private FirewallVars() {
	}

	/**
	 * Returns the variable names in IDD order.
	 */
	public static String[] variableNames() {
		return new String[] { SRC_IP, DST_IP, SRC_PORT, DST_PORT, PROTO };
	}

	/**
	 * Returns the predefined variable ranges.
	 */
	public static Map<String, VariableRange> ranges() {
		return Map.of(
			SRC_IP,
			VariableRange.of(IP_MIN, IP_MAX),
			DST_IP,
			VariableRange.of(IP_MIN, IP_MAX),
			SRC_PORT,
			VariableRange.of(PORT_MIN, PORT_MAX),
			DST_PORT,
			VariableRange.of(PORT_MIN, PORT_MAX),
			PROTO,
			VariableRange.of(PROTO_MIN, PROTO_MAX)
		);
	}

	/**
	 * Creates a new {@link VariableOrder} with the predefined firewall
	 * variables and ranges.
	 */
	public static VariableOrder order() {
		return new VariableOrder(ranges(), variableNames());
	}

	/**
	 * Creates a new {@link IDDFactory} with the predefined firewall variables,
	 * order, and ranges.
	 */
	public static IDDFactory factory() {
		return new IDDFactory(order());
	}
}
