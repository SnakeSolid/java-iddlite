package ru.snake.collection.idd.firewall;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses packets from a file or STDIN.
 * <p>
 * <h3>Packets file format</h3>
 * <pre>
 * # comments and blank lines are ignored
 *
 * src_ip dst_ip src_port dst_port proto
 * 192.168.1.10 10.0.0.1 12345 80 tcp
 * 10.0.0.5 10.0.0.1 54321 443 tcp
 * </pre>
 *
 * Each line contains five space-separated fields in the fixed order:
 * <ol>
 *   <li>source IP (dotted quad)</li>
 *   <li>destination IP (dotted quad)</li>
 *   <li>source port (integer)</li>
 *   <li>destination port (integer)</li>
 *   <li>protocol (number or name: tcp, udp, icmp)</li>
 * </ol>
 */
public final class PacketParser {

	/** Protocol name to number mapping. */
	private static final Map<String, Integer> PROTO_MAP = Map.of(
		"tcp", 6,
		"udp", 17,
		"icmp", 1
	);

	private PacketParser() {
	}

	/**
	 * Parses all packets from the given reader.
	 *
	 * @param reader the input reader
	 * @return the ordered list of parsed packets
	 * @throws IOException if the reader cannot be read
	 * @throws IllegalArgumentException on parse errors
	 */
	public static List<FirewallPacket> parse(Reader reader) throws IOException {
		try (BufferedReader br = new BufferedReader(reader)) {
			List<FirewallPacket> packets = new ArrayList<>();
			String line;

			while ((line = br.readLine()) != null) {
				String trimmed = line.trim();

				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}

				packets.add(parsePacket(trimmed));
			}

			return packets;
		}
	}

	private static FirewallPacket parsePacket(String line) {
		String[] tokens = line.split("\\s+");

		if (tokens.length != 5) {
			throw new IllegalArgumentException(
				"Expected 5 fields per packet line, got " + tokens.length +
				": " + line
			);
		}

		int srcIp = IpUtil.parseIp(tokens[0]);
		int dstIp = IpUtil.parseIp(tokens[1]);
		int srcPort = parseInt(tokens[2], "source port");
		int dstPort = parseInt(tokens[3], "destination port");
		int proto = parseProto(tokens[4]);

		// Validate ranges.
		if (srcPort < FirewallVars.PORT_MIN || srcPort > FirewallVars.PORT_MAX) {
			throw new IllegalArgumentException(
				"Source port out of range: " + srcPort
			);
		}
		if (dstPort < FirewallVars.PORT_MIN || dstPort > FirewallVars.PORT_MAX) {
			throw new IllegalArgumentException(
				"Destination port out of range: " + dstPort
			);
		}
		if (proto < FirewallVars.PROTO_MIN || proto > FirewallVars.PROTO_MAX) {
			throw new IllegalArgumentException(
				"Protocol out of range: " + proto
			);
		}

		return new FirewallPacket(srcIp, dstIp, srcPort, dstPort, proto);
	}

	private static int parseInt(String value, String fieldName) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(
				"Invalid integer for " + fieldName + ": " + value,
				e
			);
		}
	}

	private static int parseProto(String value) {
		Integer num = PROTO_MAP.get(value.toLowerCase());
		if (num != null) {
			return num;
		}

		return parseInt(value, "protocol");
	}
}
