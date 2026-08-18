package ru.snake.collection.idd.core;

import java.util.Objects;

/**
 * Represents an edge in an IDD node: a mapping from an interval to a child IDD.
 * Immutable and thread-safe.
 */
public final class Edge {

	private final int low;

	private final int high;

	private final IDD child;

	public Edge(int low, int high, IDD child) {
		if (low > high) {
			throw new IllegalArgumentException(
				"low (" + low + ") > high (" + high + ")"
			);
		}

		if (child == null) {
			throw new NullPointerException("child must not be null");
		}

		this.low = low;
		this.high = high;
		this.child = child;
	}

	public int low() {
		return low;
	}

	public int high() {
		return high;
	}

	public IDD child() {
		return child;
	}

	/**
	 * Returns the edge whose interval contains the given value, or null if no
	 * edge covers it.
	 */
	public static Edge findEdge(java.util.List<Edge> edges, int value) {
		for (Edge e : edges) {
			if (value >= e.low && value <= e.high) {
				return e;
			}

			if (value < e.low) {
				return null;
			}
		}

		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Edge edge = (Edge) o;

		return low == edge.low && high == edge.high && child == edge.child;
	}

	@Override
	public int hashCode() {
		return Objects.hash(low, high, System.identityHashCode(child));
	}

	@Override
	public String toString() {
		return "[" + low + "," + high + "]->" + child;
	}
}
