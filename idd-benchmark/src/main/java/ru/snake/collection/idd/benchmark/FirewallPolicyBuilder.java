package ru.snake.collection.idd.benchmark;

import java.util.List;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;

/**
 * Builds IDD firewall policies from {@link FirewallRuleSet} rule specifications.
 *
 * <p>
 * Provides the shared logic used by both evaluation and compilation benchmarks:
 * creating a factory, resolving IP/port specs, and constructing rule nodes.
 */
public final class FirewallPolicyBuilder {

	private final IDDFactory factory;

	private FirewallPolicyBuilder(IDDFactory factory) {
		this.factory = factory;
	}

	/**
	 * Creates a builder for the given factory.
	 */
	public static FirewallPolicyBuilder of(IDDFactory factory) {
		return new FirewallPolicyBuilder(factory);
	}

	/**
	 * Builds a firewall IDD from the first {@code ruleCount} rules of the
	 * standard rule set. Rules are combined with OR.
	 *
	 * @param ruleCount  number of rules to include
	 * @return the compiled firewall IDD (OR of all individual rules)
	 */
	public IDD buildFirewall(int ruleCount) {
		List<FirewallRuleSet.RuleSpec> allRules = FirewallRuleSet.getAllRules();
		IDD firewall = IDD.FALSE;
		int count = Math.min(ruleCount, allRules.size());

		for (int i = 0; i < count; i++) {
			firewall = factory.or(firewall, buildRule(allRules.get(i)));
		}

		return firewall;
	}

	/**
	 * Builds a single rule IDD from its specification.
	 */
	public IDD buildRule(FirewallRuleSet.RuleSpec spec) {
		int[] s = FirewallBenchmarkUtils.resolveIp(spec.srcIp());
		int[] d = FirewallBenchmarkUtils.resolveIp(spec.dstIp());
		int[] sp = FirewallBenchmarkUtils.resolvePort(spec.srcPort());
		int[] dp = FirewallBenchmarkUtils.resolvePort(spec.dstPort());
		int[] pr = spec.protocol() != null
			? new int[] { spec.protocol(), spec.protocol() }
			: new int[] { 0, 255 };

		IDD result = factory.buildFromIntervals(
			FirewallBenchmarkUtils.VAR_SRC_IP,
			List.of(new Edge(s[0], s[1], factory.trueNode()))
		);
		result = factory.and(
			result,
			factory.buildFromIntervals(
				FirewallBenchmarkUtils.VAR_DST_IP,
				List.of(new Edge(d[0], d[1], factory.trueNode()))
			)
		);
		result = factory.and(
			result,
			factory.buildFromIntervals(
				FirewallBenchmarkUtils.VAR_SRC_PORT,
				List.of(new Edge(sp[0], sp[1], factory.trueNode()))
			)
		);
		result = factory.and(
			result,
			factory.buildFromIntervals(
				FirewallBenchmarkUtils.VAR_DST_PORT,
				List.of(new Edge(dp[0], dp[1], factory.trueNode()))
			)
		);
		result = factory.and(
			result,
			factory.buildFromIntervals(
				FirewallBenchmarkUtils.VAR_PROTOCOL,
				List.of(new Edge(pr[0], pr[1], factory.trueNode()))
			)
		);

		return result;
	}
}
