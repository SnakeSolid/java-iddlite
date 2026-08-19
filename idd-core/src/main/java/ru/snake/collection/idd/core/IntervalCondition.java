package ru.snake.collection.idd.core;

/**
 * Fluent intermediate step representing the interval condition in an
 * {@code IDDBuilder} rule chain.
 * <p>
 * Created by {@link IDDBuilder#when(String)}. Carries the resolved variable
 * index and a reference back to the builder so that the fluent chain can
 * complete the rule without requiring the caller to thread the builder through
 * each step.
 * <p>
 * The interval bounds are set by {@link #in(int, int)} which returns a
 * {@link ThenCondition}.
 * <p>
 * Example:
 *
 * <pre>{@code
 * builder.when("x").in(1, 5).then(true);
 * }</pre>
 *
 * @see IDDBuilder
 * @see ThenCondition
 */
public final class IntervalCondition {

	private final IDDBuilder builder;

	private final int varIndex;

	/**
	 * Constructs an interval condition bound to the given builder.
	 *
	 * @param builder  the builder that created this condition
	 * @param varIndex the zero-based variable index in the
	 *                     {@link VariableOrder}
	 */
	IntervalCondition(IDDBuilder builder, int varIndex) {
		this.builder = builder;
		this.varIndex = varIndex;
	}

	/**
	 * Returns the variable index associated with this condition.
	 */
	public int varIndex() {
		return varIndex;
	}

	/**
	 * Specifies the interval [low, high] for this condition and returns a
	 * {@link ThenCondition} that can be completed with {@code then(boolean)}.
	 *
	 * @param low  the lower bound (inclusive)
	 * @param high the upper bound (inclusive)
	 * @return the next step in the fluent chain
	 */
	public ThenCondition in(int low, int high) {
		return new ThenCondition(builder, varIndex, low, high);
	}

	@Override
	public String toString() {
		return "IntervalCondition[varIndex=" + varIndex + "]";
	}
}
