package ru.snake.collection.idd.util;

import java.util.Objects;

/**
 * Represents a closed integer interval [low, high]. Immutable and thread-safe.
 */
public final class Interval {

	private final int low;

	private final int high;

	public Interval(int low, int high) {
		if (low > high) {
			throw new IllegalArgumentException("low (" + low + ") > high (" + high + ")");
		}

		this.low = low;
		this.high = high;
	}

	public int low() {
		return low;
	}

	public int high() {
		return high;
	}

	/**
	 * Returns true if this interval covers the given value.
	 */
	public boolean contains(int value) {
		return value >= low && value <= high;
	}

	/**
	 * Returns true if this interval is adjacent to the next interval, i.e., the
	 * other interval starts at this.high + 1.
	 */
	public boolean isAdjacentTo(int nextLow) {
		// Use long to detect overflow: high == Integer.MAX_VALUE means nextLow
		// would overflow
		return (long) high + 1 == nextLow;
	}

	/**
	 * Returns the successor boundary: high + 1, as a long to handle overflow.
	 */
	public long nextLow() {
		return (long) high + 1;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Interval interval = (Interval) o;

		return low == interval.low && high == interval.high;
	}

	@Override
	public int hashCode() {
		return Objects.hash(low, high);
	}

	@Override
	public String toString() {
		return "[" + low + "," + high + "]";
	}
}
