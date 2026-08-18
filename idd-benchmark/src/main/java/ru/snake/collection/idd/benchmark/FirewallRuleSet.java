package ru.snake.collection.idd.benchmark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard firewall rule set used across benchmarks.
 *
 * <p>
 * Loads 200 rules from {@code rules.csv} in the classpath. Each line is
 * {@code srcIp,dstIp,srcPort,dstPort,protocol} where protocol is empty for
 * wildcard (any protocol).
 */
public final class FirewallRuleSet {

	private FirewallRuleSet() {
	}

	/**
	 * A single firewall rule specification.
	 */
	public record RuleSpec(String srcIp, String dstIp, String srcPort, String dstPort, Integer protocol) {
	}

	private static final List<RuleSpec> ALL_RULES = loadRules();

	private static List<RuleSpec> loadRules() {
		List<RuleSpec> rules = new ArrayList<>(200);

		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(FirewallRuleSet.class.getResourceAsStream("/rules.csv"), StandardCharsets.UTF_8)
		)) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}

				String[] parts = line.split(",", -1);
				String srcIp = parts[0];
				String dstIp = parts[1];
				String srcPort = parts[2];
				String dstPort = parts[3];
				Integer protocol = parts[4].isEmpty() ? null : Integer.parseInt(parts[4]);
				rules.add(new RuleSpec(srcIp, dstIp, srcPort, dstPort, protocol));
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load rules.csv", e);
		}

		return List.copyOf(rules);
	}

	/**
	 * Returns the complete rule set, guaranteed to contain at least 200 rules.
	 */
	public static List<RuleSpec> getAllRules() {
		return ALL_RULES;
	}

	/**
	 * Returns the total number of available rules.
	 */
	public static int getRuleCount() {
		return ALL_RULES.size();
	}
}
