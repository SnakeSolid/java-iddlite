package ru.snake.collection.idd.firewall;

import java.util.List;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.operation.Apply;

/**
 * Builds an IDD that represents a firewall policy from a list of rules.
 * <p>
 * Implements <b>first-match-wins</b> semantics: the first rule whose
 * constraints are all satisfied determines the packet's verdict.
 * <p>
 * The construction algorithm iterates through rules in order. For each rule,
 * it computes the "new" part that is not already covered by earlier rules,
 * then accumulates ACCEPT and DROP regions into a single policy IDD.
 * <p>
 * Mathematically, for rules {@code R[0]..R[n-1]} with actions {@code A[0]..A[n-1]}:
 * <ul>
 *   <li>{@code new[0] = R[0]}</li>
 *   <li>{@code new[i] = R[i] AND NOT(R[0] OR R[1] OR ... OR R[i-1])}</li>
 * </ul>
 * The final policy maps each packet to ACCEPT if the matching rule's action
 * is ACCEPT, otherwise DENY.
 */
public final class FirewallBuilder {

	private final IDDFactory factory;

	/**
	 * Constructs a builder using the provided factory.
	 */
	public FirewallBuilder(IDDFactory factory) {
		this.factory = factory;
	}

	/**
	 * Builds the firewall policy IDD from the given rules.
	 * <p>
	 * The resulting IDD evaluates to TRUE for packets that should be ACCEPTed
	 * and FALSE for packets that should be DROPped.
	 *
	 * @param rules the ordered list of firewall rules
	 * @return the combined policy IDD
	 */
	public IDD build(List<FirewallRule> rules) {
		if (rules.isEmpty()) {
			return factory.falseNode();
		}

		// The IDD of all packets matched by earlier rules (union of R[0]..R[i-1]).
		IDD covered = factory.falseNode();

		// Accumulate all regions that should ACCEPT.
		IDD accept = factory.falseNode();

		for (FirewallRule rule : rules) {
			// Build the IDD for this rule's constraint.
			IDD ruleIdd = buildRuleIdd(rule);

			// The "new" part: packets matching this rule that are not already
			// covered by an earlier rule.
			IDD uncovered = Apply.not(factory, covered);
			IDD newPart = Apply.and(factory, ruleIdd, uncovered);

			// If this rule is ACCEPT, add its uncovered region to the accept
			// set.
			if (rule.action() == FirewallRule.Action.ACCEPT) {
				accept = Apply.or(factory, accept, newPart);
			}

			// Expand covered to include this rule's full match set.
			covered = Apply.or(factory, covered, ruleIdd);
		}

		return accept;
	}

	/**
	 * Builds an IDD representing a single rule's constraints.
	 */
	private IDD buildRuleIdd(FirewallRule rule) {
		IDD result = buildVarIdd(
			FirewallVars.SRC_IP, rule.srcIp()
		);
		result = Apply.and(
			factory, result,
			buildVarIdd(FirewallVars.DST_IP, rule.dstIp())
		);
		result = Apply.and(
			factory, result,
			buildVarIdd(FirewallVars.SRC_PORT, rule.srcPort())
		);
		result = Apply.and(
			factory, result,
			buildVarIdd(FirewallVars.DST_PORT, rule.dstPort())
		);
		result = Apply.and(
			factory, result,
			buildVarIdd(FirewallVars.PROTO, rule.proto())
		);
		return result;
	}

	/**
	 * Builds an IDD for a single variable. If the constraint is null, returns
	 * TRUE (matches all values). Otherwise builds a single-edge node covering
	 * the constraint's interval.
	 */
	private IDD buildVarIdd(
		String varName, FirewallRule.Constraint constraint
	) {
		if (constraint == null) {
			return factory.trueNode();
		}

		return factory.buildFromIntervals(
			varName,
			List.of(new Edge(
				constraint.low(),
				constraint.high(),
				factory.trueNode()
			))
		);
	}

	/**
	 * Convenience factory method: create a builder and build from rules.
	 *
	 * @param rules the ordered list of firewall rules
	 * @return the combined policy IDD
	 */
	public static IDD buildPolicy(List<FirewallRule> rules) {
		IDDFactory factory = FirewallVars.factory();
		FirewallBuilder builder = new FirewallBuilder(factory);
		return builder.build(rules);
	}
}
