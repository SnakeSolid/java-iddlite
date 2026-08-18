package ru.snake.collection.idd.benchmark;

/**
 * Shared helpers for firewall benchmarks.
 *
 * <p>
 * Provides IP/CIDR resolution, port spec parsing, deterministic packet
 * generation, and the standard rule set used across evaluation and
 * compilation benchmarks.
 */
public final class FirewallBenchmarkUtils {

	private FirewallBenchmarkUtils() {
	}

	// ------------------------------------------------------------------
	// Variable names
	// ------------------------------------------------------------------

	public static final String VAR_SRC_IP = "src_ip";

	public static final String VAR_DST_IP = "dst_ip";

	public static final String VAR_SRC_PORT = "src_port";

	public static final String VAR_DST_PORT = "dst_port";

	public static final String VAR_PROTOCOL = "protocol";

	// ------------------------------------------------------------------
	// Protocol constants (RFC 1700)
	// ------------------------------------------------------------------

	public static final int PROTO_ICMP = 1;

	public static final int PROTO_TCP = 6;

	public static final int PROTO_UDP = 17;

	// ==================================================================
	// IP / CIDR helpers
	// ==================================================================

	/**
	 * Converts a dotted-quad IPv4 string to a signed 32-bit int.
	 */
	public static int ip(String addr) {
		String[] parts = addr.split("\\.");
		return (((Integer.parseInt(parts[0]) & 0xFF) << 24) | ((Integer.parseInt(parts[1]) & 0xFF) << 16)
				| ((Integer.parseInt(parts[2]) & 0xFF) << 8) | (Integer.parseInt(parts[3]) & 0xFF));
	}

	/**
	 * Resolves a CIDR string (or "*" / null) to an [low, high] int range.
	 */
	public static int[] resolveIp(String cidr) {
		if (cidr == null || cidr.equals("*")) {
			return new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE };
		}

		return cidrRange(cidr);
	}

	/**
	 * Converts a CIDR notation (e.g. "10.0.0.0/8") to an [low, high] int range.
	 */
	public static int[] cidrRange(String cidr) {
		String[] parts = cidr.split("/");
		int host = ip(parts[0]);
		int prefix = Integer.parseInt(parts[1]);
		int mask = prefix == 0 ? 0 : ~0 << (32 - prefix);
		int network = host & mask;
		int hostBits = prefix == 32 ? 0 : (1 << (32 - prefix)) - 1;
		return new int[] { network, network | hostBits };
	}

	// ==================================================================
	// Port helpers
	// ==================================================================

	/**
	 * Resolves a port spec ("*", "80", "1024-2048") to an [low, high] int range.
	 */
	public static int[] resolvePort(String spec) {
		if (spec == null || spec.equals("*")) {
			return new int[] { 0, 65535 };
		}

		if (spec.contains("-")) {
			String[] parts = spec.split("-");
			return new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) };
		}

		return new int[] { Integer.parseInt(spec), Integer.parseInt(spec) };
	}

	// ==================================================================
	// Deterministic packet generation
	// ==================================================================

	/**
	 * Generates a fixed set of 1000 deterministic packets using a seeded LCG
	 * (linear congruential generator). Same seeds always produce the same
	 * packets, making benchmark results fully reproducible.
	 *
	 * @return 1000 packets, each encoded as [srcIp, dstIp, srcPort, dstPort, protocol]
	 */
	public static int[][] generateDeterministicPackets() {
		DetRng rng = new DetRng(12345);
		int[][] packets = new int[1000][5];
		int[] allowedPorts = { 80, 443, 53, 123, 161 };

		for (int i = 0; i < packets.length; i++) {
			int srcIp, dstIp, srcPort, dstPort, proto;
			int roll = rng.nextInt(100);

			if (roll < 30) {
				// Packets from rule-covered ranges
				srcIp = ip("10.0.0.0") | (rng.nextInt() & 0x00FFFFFF);
				dstIp = rng.nextInt() & 0xFFFFFFFF;
				srcPort = rng.nextInt(65536);
				dstPort = allowedPorts[rng.nextInt(allowedPorts.length)];
				proto = PROTO_TCP;
			} else if (roll < 60) {
				// Packets from blocked ranges
				srcIp = ip("203.0.113.0") | (rng.nextInt() & 0x000000FF);
				dstIp = ip("10.0.1.0") | (rng.nextInt() & 0x000000FF);
				srcPort = rng.nextInt(65536);
				dstPort = rng.nextInt(65536);
				proto = PROTO_TCP;
			} else {
				// Fully random across the int domain
				srcIp = rng.nextInt();
				dstIp = rng.nextInt();
				srcPort = rng.nextInt(65536);
				dstPort = rng.nextInt(65536);
				proto = rng.nextInt(256);
			}

			packets[i] = new int[] { srcIp, dstIp, srcPort, dstPort, proto };
		}

		return packets;
	}

	/** Minimal deterministic RNG (LCG) -- no external dependencies. */
	public static class DetRng {

		private long seed;

		public DetRng(long seed) {
			this.seed = seed;
		}

		public int nextInt() {
			seed = seed * 6364136223846793005L + 1442695040888963407L;
			return (int) (seed ^ (seed >>> 33));
		}

		public int nextInt(int bound) {
			return Math.abs(nextInt()) % bound;
		}
	}
}
