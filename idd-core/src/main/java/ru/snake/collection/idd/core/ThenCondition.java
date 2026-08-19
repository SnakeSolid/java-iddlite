package ru.snake.collection.idd.core;

/**
 * Fluent terminal step representing the completed interval condition in an
 * {@code IDDBuilder} rule chain.
 * <p>
 * Created by {@link IntervalCondition#in(int, int)}. Carries the variable
 * index, interval bounds, and a reference back to the builder so that
 * {@code then(boolean)} can register the rule and return the builder for
 * further chaining.
 * <p>
 * Example:
 *
 * <pre>{@code
 * builder.when("x").in(1, 5).then(true);
 * }</pre>
 *
 * @see IDDBuilder
 * @see IntervalCondition
 */
public final class ThenCondition {

	private final IDDBuilder builder;

	private final int varIndex;

	private final int low;

	private final int high;

	/**
	 * Constructs a then-condition with the given builder, variable index, and
	 * interval.
	 *
	 * @param builder  the builder that created this condition
	 * @param varIndex the zero-based variable index
	 * @param low      the lower bound of the interval (inclusive)
	 * @param high     the upper bound of the interval (inclusive)
	 */
	ThenCondition(IDDBuilder builder, int varIndex, int low, int high) {
		this.builder = builder;
		this.varIndex = varIndex;
		this.low = low;
		this.high = high;
	}

	/**
	 * Returns the variable index associated with this condition.
	 */
	public int varIndex() {
		return varIndex;
	}

	/**
	 * Returns the lower bound of the interval.
	 */
	public int low() {
		return low;
	}

	/**
	 * Returns the upper bound of the interval.
	 */
	public int high() {
		return high;
	}

	/**
	 * Registers the rule with the builder and returns it for further chaining.
	 *
	 * @param value the boolean outcome for this interval
	 * @return the builder for further fluent calls
	 */
	public IDDBuilder then(boolean value) {
		builder.addRule(varIndex, low, high, value);
		return builder;
	}

	@Override
	public String toString() {
		return ("ThenCondition[varIndex=" + varIndex + ", low=" + low + ", high=" + high + "]");
	}
}
