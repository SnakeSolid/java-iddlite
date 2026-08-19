package ru.snake.collection.idd.firewall;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.operation.Evaluate;

/**
 * Command-line interface for firewall rule analysis.
 * <p>
 * Reads firewall rules from a file, builds an IDD, then evaluates packets from
 * another file or STDIN and prints the verdict for each.
 * <p>
 * <h3>Usage</h3>
 *
 * <pre>
 * java -jar idd-firewall.jar &lt;rules-file&gt; [&lt;packets-file&gt;]
 * </pre>
 * <p>
 * If the packets file is omitted, packets are read from STDIN.
 * <p>
 * <h3>Output</h3> One line per packet: {@code ACCEPT} or {@code DROP}.
 */
public final class FirewallCli {

	private FirewallCli() {
	}

	/**
	 * Entry point.
	 *
	 * @param args command-line arguments: rules file, optional packets file
	 * @throws IOException if files cannot be read
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 1 || args.length > 2) {
			System.err.println("Usage: java -jar idd-firewall.jar <rules-file> [<packets-file>]");
			System.exit(1);
		}

		Path rulesPath = Path.of(args[0]);
		if (!Files.exists(rulesPath)) {
			System.err.println("Rules file not found: " + rulesPath);
			System.exit(1);
		}

		// Parse rules.
		try (Reader rulesReader = new InputStreamReader(Files.newInputStream(rulesPath), StandardCharsets.UTF_8)) {
			List<FirewallRule> rules = FirewallParser.parse(rulesReader);

			if (rules.isEmpty()) {
				System.err.println("Warning: no rules parsed from " + rulesPath);
			}

			// Build IDD policy.
			IDD policy = FirewallBuilder.buildPolicy(rules);

			// Evaluate packets.

			try (Reader packetsReader = getPacketsReader(args)) {
				List<FirewallPacket> packets = PacketParser.parse(packetsReader);

				for (FirewallPacket pkt : packets) {
					boolean accept = Evaluate.evaluate(policy, pkt.toIntArray());
					System.out.println(accept ? "ACCEPT" : "DROP");
				}
			}
		}
	}

	private static Reader getPacketsReader(String[] args) {
		if (args.length == 2) {
			Path packetsPath = Path.of(args[1]);
			if (!Files.exists(packetsPath)) {
				System.err.println("Packets file not found: " + packetsPath);
				System.exit(1);
			}

			try {
				return new InputStreamReader(Files.newInputStream(packetsPath), StandardCharsets.UTF_8);
			} catch (IOException e) {
				System.err.println("Cannot open packets file: " + packetsPath);
				System.exit(1);
				throw new AssertionError("Unreachable", e);
			}
		}

		// Read from STDIN.
		return new InputStreamReader(System.in, StandardCharsets.UTF_8);
	}
}
