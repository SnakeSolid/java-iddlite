package ru.snake.collection.idd.firewall;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a firewall rules file into a list of {@link FirewallRule} objects.
 * <p>
 * <h3>Rules file format</h3>
 *
 * <pre>
 * # comments and blank lines are ignored
 *
 * ACCEPT proto=tcp dst_port=80
 * ACCEPT proto=tcp dst_port=443
 * DROP  proto=udp dst_ip=10.0.0.0/8
 * DROP  *
 * </pre>
 *
 * <h3>Syntax</h3>
 * <ul>
 * <li>{@code ACCEPT} or {@code DROP} — the rule action</li>
 * <li>{@code proto=N}, {@code src_ip=...}, {@code dst_ip=...},
 * {@code src_port=...}, {@code dst_port=...} — field constraints</li>
 * <li>{@code *} — catch-all (no constraints)</li>
 * <li>IP: dotted quad (10.0.0.1) or CIDR (10.0.0.0/8) or "any"</li>
 * <li>Port: number (80), range (8000-9000) or "any"</li>
 * <li>Proto: number (6), name (tcp, udp, icmp) or "any"</li>
 * <li>Omitting a field is equivalent to "any"</li>
 * </ul>
 *
 * <h3>First-match-wins semantics</h3>
 * <p>
 * Rules are processed in file order. A packet matches the first rule whose
 * constraints are all satisfied.
 * </p>
 */
public final class FirewallParser {

	/** Protocol name to number mapping. */
	private static final Map<String, Integer> PROTO_MAP = Map.of(
		"tcp",
		6,
		"udp",
		17,
		"icmp",
		1
	);

	/** Pattern for a key=value token. */
	private static final Pattern KV_PATTERN = Pattern.compile("([a-z_]+)=(.+)");

	private FirewallParser() {}

	/**
	 * Parses all rules from the given reader.
	 *
	 * @param reader the input reader
	 * @return the ordered list of parsed rules
	 * @throws IOException              if the reader cannot be read
	 * @throws IllegalArgumentException on parse errors
	 */
	public static List<FirewallRule> parse(Reader reader) throws IOException {
		try (BufferedReader br = new BufferedReader(reader)) {
			List<FirewallRule> rules = new ArrayList<>();
			String line;
			int sequence = 0;

			while ((line = br.readLine()) != null) {
				String trimmed = line.trim();

				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}

				rules.add(parseRule(trimmed, sequence++));
			}

			return rules;
		}
	}

	private static FirewallRule parseRule(String line, int sequence) {
		String[] tokens = line.split("\\s+");

		if (tokens.length == 0) {
			throw new IllegalArgumentException("Empty rule line");
		}

		FirewallRule.Action action = parseAction(tokens[0]);

		if (tokens.length < 2) {
			throw new IllegalArgumentException(
				"Rule missing constraints: " + line
			);
		}

		// Parse constraints from tokens.
		FirewallRule.Constraint srcIp = null;
		FirewallRule.Constraint dstIp = null;
		FirewallRule.Constraint srcPort = null;
		FirewallRule.Constraint dstPort = null;
		FirewallRule.Constraint proto = null;

		for (int i = 1; i < tokens.length; i++) {
			String token = tokens[i];

			if (token.equals("*")) {
				// Catch-all token — no additional constraints.
				continue;
			}

			Matcher m = KV_PATTERN.matcher(token);
			if (!m.matches()) {
				throw new IllegalArgumentException(
					"Invalid rule token: '" + token + "' in: " + line
				);
			}

			String key = m.group(1);
			String value = m.group(2);

			switch (key) {
				case "src_ip" -> srcIp = parseIpConstraint(value);
				case "dst_ip" -> dstIp = parseIpConstraint(value);
				case "src_port" -> srcPort = parsePortConstraint(value);
				case "dst_port" -> dstPort = parsePortConstraint(value);
				case "proto" -> proto = parseProtoConstraint(value);
				default -> throw new IllegalArgumentException(
					"Unknown field: " + key + " in: " + line
				);
			}
		}

		return new FirewallRule(
			action,
			sequence,
			srcIp,
			dstIp,
			srcPort,
			dstPort,
			proto
		);
	}

	private static FirewallRule.Action parseAction(String token) {
		return switch (token.toUpperCase()) {
			case "ACCEPT" -> FirewallRule.Action.ACCEPT;
			case "DROP" -> FirewallRule.Action.DROP;
			default -> throw new IllegalArgumentException(
				"Unknown action: " + token + " (expected ACCEPT or DROP)"
			);
		};
	}

	private static FirewallRule.Constraint parseIpConstraint(String value) {
		if (value.equals("any") || value.equals("0.0.0.0/0")) {
			return null;
		}

		if (value.contains("/")) {
			int[] range = IpUtil.cidrRange(value);
			return new FirewallRule.Constraint(range[0], range[1]);
		}

		int ip = IpUtil.parseIp(value);
		return new FirewallRule.Constraint(ip, ip);
	}

	private static FirewallRule.Constraint parsePortConstraint(String value) {
		if (value.equals("any")) {
			return null;
		}

		if (value.contains("-")) {
			String[] parts = value.split("-", 2);
			int low = Integer.parseInt(parts[0]);
			int high = Integer.parseInt(parts[1]);
			validatePort(low, "port range");
			validatePort(high, "port range");

			if (low > high) {
				throw new IllegalArgumentException(
					"Invalid port range: " + low + "-" + high
				);
			}

			return new FirewallRule.Constraint(low, high);
		}

		int port = Integer.parseInt(value);
		validatePort(port, "port");
		return new FirewallRule.Constraint(port, port);
	}

	private static void validatePort(int port, String context) {
		if (port < FirewallVars.PORT_MIN || port > FirewallVars.PORT_MAX) {
			throw new IllegalArgumentException(
				"Port out of range in " + context + ": " + port
			);
		}
	}

	private static FirewallRule.Constraint parseProtoConstraint(String value) {
		if (value.equals("any")) {
			return null;
		}

		Integer num = PROTO_MAP.get(value.toLowerCase());
		if (num != null) {
			return new FirewallRule.Constraint(num, num);
		}

		int proto;
		try {
			proto = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(
				"Invalid protocol: '" +
					value +
					"' (expected a number or tcp/udp/icmp/any)",
				e
			);
		}

		if (proto < FirewallVars.PROTO_MIN || proto > FirewallVars.PROTO_MAX) {
			throw new IllegalArgumentException(
				"Protocol out of range: " + proto
			);
		}

		return new FirewallRule.Constraint(proto, proto);
	}
}
