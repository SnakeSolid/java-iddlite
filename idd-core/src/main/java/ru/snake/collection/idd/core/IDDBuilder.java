package ru.snake.collection.idd.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for constructing IDDs from multi-variable interval rules.
 * <p>
 * Each rule is a conjunction of conditions terminated by {@code then(boolean)}.
 * Rules are combined into a single IDD: {@code true} rules are OR-ed together,
 * and {@code false} rules explicitly define regions that must evaluate to
 * {@code false} even if covered by a {@code true} rule.
 * <p>
 * Example — single-variable with mixed outcomes:
 * 
 * <pre>{@code
 * IDD idd = factory.builder().when("x").in(1, 10).then(true).when("x").in(11, 20).then(false).build();
 * }</pre>
 *
 * Example — multi-variable rule (conditions within a rule are AND-ed):
 * 
 * <pre>{@code
 * IDD rule = factory.builder().when("proto").in(6, 6).when("dst_port").in(80, 443).then(true).build();
 * }</pre>
 *
 * Example — multiple rules combined with OR:
 * 
 * <pre>{@code
 * IDD policy = factory.builder()
 * 	.when("x")
 * 	.in(1, 5)
 * 	.when("y")
 * 	.in(1, 5)
 * 	.then(true)
 * 	.when("x")
 * 	.in(6, 10)
 * 	.when("y")
 * 	.in(1, 5)
 * 	.when("z")
 * 	.in(0, 10)
 * 	.then(true)
 * 	.build();
 * }</pre>
 */
public final class IDDBuilder {

	private final IDDFactory factory;

	/**
	 * Conditions accumulated for the current rule (not yet committed by then).
	 */
	private final List<Condition> pendingConditions = new ArrayList<>();

	/** Committed true-rules (conjunction IDDs to OR together). */
	private final List<IDD> trueRules = new ArrayList<>();

	/** Committed false-rules (conjunction IDDs to exclude). */
	private final List<IDD> falseRules = new ArrayList<>();

	IDDBuilder(IDDFactory factory) {
		this.factory = factory;
	}

	/**
	 * Start a condition on the given variable.
	 * <p>
	 * The condition is added to the current pending rule and can be chained
	 * with more {@code when} calls or terminated with {@code then}.
	 */
	public WhenCondition when(String varName) {
		int varIndex = factory.order().index(varName);
		return new WhenCondition(varIndex);
	}

	/**
	 * Commits the pending conditions as a single rule with the given outcome.
	 * <p>
	 * The conditions are AND-ed into an IDD fragment and stored. Call
	 * {@link #build()} when all rules are defined.
	 *
	 * @param value the boolean outcome for this rule
	 * @return this builder for further chaining
	 */
	public IDDBuilder then(boolean value) {
		if (pendingConditions.isEmpty()) {
			throw new IllegalStateException("then() requires at least one preceding when().in()/is()");
		}

		IDD ruleIdd = buildRuleIdd(pendingConditions);
		pendingConditions.clear();

		if (value) {
			trueRules.add(ruleIdd);
		} else {
			falseRules.add(ruleIdd);
		}

		return this;
	}

	/**
	 * Builds the final IDD from all committed rules.
	 * <p>
	 * True rules are combined with OR. False rules are combined with OR and
	 * then excluded from the result (regions matching a false rule evaluate to
	 * false even if also matching a true rule).
	 *
	 * @return the constructed IDD
	 */
	public IDD build() {
		if (!pendingConditions.isEmpty()) {
			throw new IllegalStateException("Uncommitted conditions — call then(boolean) before build()");
		}

		if (trueRules.isEmpty() && falseRules.isEmpty()) {
			return factory.trueNode();
		}

		// OR all true rules together.
		IDD result = IDD.FALSE;
		for (IDD rule : trueRules) {
			result = factory.or(result, rule);
		}

		// If there are false rules, exclude their regions from the result.
		// A false rule takes precedence: if a point matches a false rule,
		// it evaluates to false even if it also matches a true rule.
		if (!falseRules.isEmpty()) {
			IDD falseMask = IDD.FALSE;
			for (IDD rule : falseRules) {
				falseMask = factory.or(falseMask, rule);
			}
			// result = result AND (NOT falseMask)
			result = factory.and(result, factory.not(falseMask));
		}

		return result;
	}

	/**
	 * Builds an IDD for a single rule: AND of all its conditions.
	 */
	private IDD buildRuleIdd(List<Condition> conditions) {
		IDD result = factory.trueNode();

		// Group conditions by variable.
		List<Condition> sorted = new ArrayList<>(conditions);
		sorted.sort((a, b) -> Integer.compare(a.varIndex, b.varIndex));

		List<Edge> edges = new ArrayList<>();
		int i = 0;
		while (i < sorted.size()) {
			int varIndex = sorted.get(i).varIndex;
			edges.clear();

			while (i < sorted.size() && sorted.get(i).varIndex == varIndex) {
				Condition c = sorted.get(i);
				edges.add(new Edge(c.low, c.high, factory.trueNode()));
				i++;
			}

			IDD varIdd = factory.getNode(varIndex, edges);
			result = factory.and(result, varIdd);
		}

		return result;
	}

	@Override
	public String toString() {
		return ("IDDBuilder[pending=" + pendingConditions.size() + ", trueRules=" + trueRules.size() + ", falseRules="
				+ falseRules.size() + "]");
	}

	static IDDBuilder create(IDDFactory factory) {
		return new IDDBuilder(factory);
	}

	/** A single variable interval condition within a rule. */
	private record Condition(int varIndex, int low, int high) {
	}

	/**
	 * Fluent intermediate step returned by {@link IDDBuilder#when(String)}.
	 * <p>
	 * Call {@link #in(int, int)} or {@link #is(int)} to complete the condition,
	 * which returns the builder for further chaining.
	 */
	public final class WhenCondition {

		private final int varIndex;

		WhenCondition(int varIndex) {
			this.varIndex = varIndex;
		}

		/**
		 * Returns the variable index for this condition.
		 */
		public int varIndex() {
			return varIndex;
		}

		/**
		 * Specifies an interval [low, high] for this condition.
		 *
		 * @param low  the lower bound (inclusive)
		 * @param high the upper bound (inclusive)
		 * @return the builder for further chaining
		 */
		public IDDBuilder in(int low, int high) {
			pendingConditions.add(new Condition(varIndex, low, high));
			return IDDBuilder.this;
		}

		/**
		 * Specifies an exact value for this condition (shorthand for
		 * {@code in(value, value)}).
		 *
		 * @param value the exact value
		 * @return the builder for further chaining
		 */
		public IDDBuilder is(int value) {
			return in(value, value);
		}

		@Override
		public String toString() {
			return "WhenCondition[varIndex=" + varIndex + "]";
		}
	}
}
