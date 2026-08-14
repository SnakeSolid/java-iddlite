package ru.snake.collection.idd.firewall;

import java.util.Map;

/**
 * Represents a single parsed network packet.
 * <p>
 * Contains values for all five predefined firewall variables.
 */
public final class FirewallPacket {

	private final int srcIp;

	private final int dstIp;

	private final int srcPort;

	private final int dstPort;

	private final int proto;

	/**
	 * Constructs a packet.
	 *
	 * @param srcIp   the source IP as a signed 32-bit int
	 * @param dstIp   the destination IP as a signed 32-bit int
	 * @param srcPort the source port
	 * @param dstPort the destination port
	 * @param proto   the protocol number
	 */
	public FirewallPacket(
		int srcIp,
		int dstIp,
		int srcPort,
		int dstPort,
		int proto
	) {
		this.srcIp = srcIp;
		this.dstIp = dstIp;
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.proto = proto;
	}

	/** Returns the source IP as a signed 32-bit int. */
	public int srcIp() {
		return srcIp;
	}

	/** Returns the destination IP as a signed 32-bit int. */
	public int dstIp() {
		return dstIp;
	}

	/** Returns the source port. */
	public int srcPort() {
		return srcPort;
	}

	/** Returns the destination port. */
	public int dstPort() {
		return dstPort;
	}

	/** Returns the protocol number. */
	public int proto() {
		return proto;
	}

	/**
	 * Returns a {@link java.util.Map} suitable for IDD evaluation.
	 *
	 * @return variable name to integer value mapping
	 */
	public java.util.Map<String, Integer> toAssignment() {
		return Map.of(
			FirewallVars.SRC_IP,
			srcIp,
			FirewallVars.DST_IP,
			dstIp,
			FirewallVars.SRC_PORT,
			srcPort,
			FirewallVars.DST_PORT,
			dstPort,
			FirewallVars.PROTO,
			proto
		);
	}

	@Override
	public String toString() {
		return (
			"Packet[src_ip=" +
			toIpString(srcIp) +
			" dst_ip=" +
			toIpString(dstIp) +
			" src_port=" +
			srcPort +
			" dst_port=" +
			dstPort +
			" proto=" +
			proto +
			"]"
		);
	}

	private static String toIpString(int ip) {
		return (
			((ip >> 24) & 0xFF) +
			"." +
			((ip >> 16) & 0xFF) +
			"." +
			((ip >> 8) & 0xFF) +
			"." +
			(ip & 0xFF)
		);
	}
}
