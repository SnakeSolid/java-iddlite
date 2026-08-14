package ru.snake.collection.idd.firewall;

/**
 * Utility methods for parsing IPv4 addresses and CIDR notation.
 * <p>
 * IP addresses are encoded as signed 32-bit integers in big-endian order:
 * {@code (a << 24) | (b << 16) | (c << 8) | d}.
 */
public final class IpUtil {

	private IpUtil() {}

	/**
	 * Parses a dotted-quad IPv4 address string to a signed 32-bit int.
	 *
	 * @param addr the dotted-quad string (e.g. "10.0.0.1")
	 * @return the IP as a signed 32-bit integer
	 * @throws IllegalArgumentException if the format is invalid
	 */
	public static int parseIp(String addr) {
		String[] parts = addr.split("\\.");
		if (parts.length != 4) {
			throw new IllegalArgumentException("Invalid IP address: " + addr);
		}

		int result = 0;
		for (String part : parts) {
			int octet = Integer.parseInt(part);
			if (octet < 0 || octet > 255) {
				throw new IllegalArgumentException(
					"Invalid octet in IP address " + addr + ": " + octet
				);
			}

			result = (result << 8) | (octet & 0xFF);
		}

		return result;
	}

	/**
	 * Returns the network address for a CIDR prefix.
	 *
	 * @param cidr the CIDR string (e.g. "10.0.0.0/8")
	 * @return the network address as a signed 32-bit int
	 * @throws IllegalArgumentException if the format is invalid
	 */
	public static int cidrNetwork(String cidr) {
		String[] parts = cidr.split("/");
		int host = parseIp(parts[0]);
		int prefix = Integer.parseInt(parts[1]);

		if (prefix < 0 || prefix > 32) {
			throw new IllegalArgumentException(
				"Invalid CIDR prefix: " + prefix
			);
		}

		if (prefix == 0) {
			return 0;
		}

		int mask = ~0 << (32 - prefix);
		return host & mask;
	}

	/**
	 * Returns the broadcast address for a CIDR prefix.
	 *
	 * @param cidr the CIDR string (e.g. "10.0.0.0/8")
	 * @return the broadcast address as a signed 32-bit int
	 * @throws IllegalArgumentException if the format is invalid
	 */
	public static int cidrBroadcast(String cidr) {
		int network = cidrNetwork(cidr);
		String[] parts = cidr.split("/");
		int prefix = Integer.parseInt(parts[1]);

		if (prefix == 0) {
			return -1; // all bits set = 0xFFFFFFFF = -1 as signed int
		}

		int hostBits = (1 << (32 - prefix)) - 1;
		return network | hostBits;
	}

	/**
	 * Returns the inclusive address range for a CIDR prefix.
	 *
	 * @param cidr the CIDR string (e.g. "10.0.0.0/8")
	 * @return int array of [network, broadcast]
	 */
	public static int[] cidrRange(String cidr) {
		return new int[] { cidrNetwork(cidr), cidrBroadcast(cidr) };
	}
}
