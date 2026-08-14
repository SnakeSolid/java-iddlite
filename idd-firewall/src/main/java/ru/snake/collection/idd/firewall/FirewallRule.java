package ru.snake.collection.idd.firewall;

/**
 * Represents a single parsed firewall rule.
 * <p>
 * Each rule has an action (ACCEPT or DROP) and optional constraints on the
 * five predefined variables. A constraint is an inclusive integer interval;
 * if no constraint exists for a variable, it matches any value.
 */
public final class FirewallRule {

	/** Rule action. */
	public enum Action {
		/** Accept (ALLOW) the packet. */
		ACCEPT,

		/** Drop (DENY) the packet. */
		DROP
	}

	/** Inclusive interval constraint for a variable. */
	public record Constraint(int low, int high) {
	}

	private final Action action;

	private final int sequence;

	private final Constraint srcIp;

	private final Constraint dstIp;

	private final Constraint srcPort;

	private final Constraint dstPort;

	private final Constraint proto;

	/**
	 * Constructs a firewall rule.
	 *
	 * @param action    the rule action
	 * @param sequence  the rule's position in the original list (0-based)
	 * @param srcIp     source IP constraint, or null for any
	 * @param dstIp     destination IP constraint, or null for any
	 * @param srcPort   source port constraint, or null for any
	 * @param dstPort   destination port constraint, or null for any
	 * @param proto     protocol constraint, or null for any
	 */
	public FirewallRule(
		Action action,
		int sequence,
		Constraint srcIp,
		Constraint dstIp,
		Constraint srcPort,
		Constraint dstPort,
		Constraint proto
	) {
		this.action = action;
		this.sequence = sequence;
		this.srcIp = srcIp;
		this.dstIp = dstIp;
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.proto = proto;
	}

	/** Returns the rule action. */
	public Action action() {
		return action;
	}

	/** Returns the rule's sequence number. */
	public int sequence() {
		return sequence;
	}

	/** Returns the source IP constraint, or null for any. */
	public Constraint srcIp() {
		return srcIp;
	}

	/** Returns the destination IP constraint, or null for any. */
	public Constraint dstIp() {
		return dstIp;
	}

	/** Returns the source port constraint, or null for any. */
	public Constraint srcPort() {
		return srcPort;
	}

	/** Returns the destination port constraint, or null for any. */
	public Constraint dstPort() {
		return dstPort;
	}

	/** Returns the protocol constraint, or null for any. */
	public Constraint proto() {
		return proto;
	}

	@Override
	public String toString() {
		return "Rule[" + sequence + "] " + action +
			" srcIp=" + srcIp +
			" dstIp=" + dstIp +
			" srcPort=" + srcPort +
			" dstPort=" + dstPort +
			" proto=" + proto;
	}
}
