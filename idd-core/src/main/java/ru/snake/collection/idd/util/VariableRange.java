package ru.snake.collection.idd.util;

import java.util.Objects;

/**
 * Represents the valid value range for a variable in an IDD.
 * <p>
 * Default is the full integer range {@code [MIN_VALUE, MAX_VALUE]}.
 * Specialized ranges constrain gap-filling and reduction to the
 * semantic domain of the variable (e.g., ports: 0..65535).
 * <p>
 * Immutable and thread-safe.
 */
public final class VariableRange {

	/** The lower bound (inclusive). */
	private final int min;

	/** The upper bound (inclusive). */
	private final int max;

	/** Cached full-range instance. */
	private static final VariableRange FULL_RANGE = new VariableRange(
		Integer.MIN_VALUE, Integer.MAX_VALUE);

	/**
	 * Constructs a range with the given bounds.
	 *
	 * @param min the lower bound (inclusive)
	 * @param max the upper bound (inclusive)
	 * @throws IllegalArgumentException if min > max
	 */
	public VariableRange(int min, int max) {
		if (min > max) {
			throw new IllegalArgumentException(
				"min (" + min + ") > max (" + max + ")");
		}

		this.min = min;
		this.max = max;
	}

	/**
	 * Returns the default full integer range {@code [MIN_VALUE, MAX_VALUE]}.
	 *
	 * @return the full-range instance
	 */
	public static VariableRange fullRange() {
		return FULL_RANGE;
	}

	/**
	 * Creates a specialized range from the given bounds.
	 *
	 * @param min the lower bound (inclusive)
	 * @param max the upper bound (inclusive)
	 * @return the new range
	 * @throws IllegalArgumentException if min > max
	 */
	public static VariableRange of(int min, int max) {
		return new VariableRange(min, max);
	}

	/**
	 * Returns the lower bound (inclusive).
	 *
	 * @return the minimum valid value
	 */
	public int min() {
		return min;
	}

	/**
	 * Returns the upper bound (inclusive).
	 *
	 * @return the maximum valid value
	 */
	public int max() {
		return max;
	}

	/**
	 * Returns true if the given value falls within this range.
	 *
	 * @param value the value to test
	 * @return true if {@code min <= value <= max}
	 */
	public boolean contains(int value) {
		return value >= min && value <= max;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o == null || getClass() != o.getClass()) {
			return false;
		}

		VariableRange other = (VariableRange) o;

		return min == other.min && max == other.max;
	}

	@Override
	public int hashCode() {
		return Objects.hash(min, max);
	}

	@Override
	public String toString() {
		return "[" + min + "," + max + "]";
	}
}
