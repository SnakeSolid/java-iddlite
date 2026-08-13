package ru.snake.collection.idd.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Factory for creating IDD nodes with hash-consing (unique table) and
 * reduction.
 * <p>
 * All IDD nodes must be created through this factory to ensure canonicity. Uses
 * {@link WeakHashMap} for the unique table, allowing unreachable nodes to be
 * garbage-collected.
 */
public final class IDDFactory {

	private final VariableOrder order;

	private final Map<NodeKey, IDD> uniqueTable;

	public IDDFactory(VariableOrder order) {
		this.order = Objects.requireNonNull(order);
		this.uniqueTable = new WeakHashMap<>();
	}

	public VariableOrder order() {
		return order;
	}

	/**
	 * Returns the TRUE terminal node.
	 */
	public IDD trueNode() {
		return IDD.TRUE;
	}

	/**
	 * Returns the FALSE terminal node.
	 */
	public IDD falseNode() {
		return IDD.FALSE;
	}

	/**
	 * Creates a fluent builder for constructing IDDs.
	 */
	public IDDBuilder builder() {
		return IDDBuilder.create(this);
	}

	/**
	 * Builds an IDD for the given variable from a list of edges.
	 * <p>
	 * Edges not explicitly listed are implicitly filled with FALSE edges.
	 *
	 * @param varName the variable name
	 * @param edges   the explicit edges (intervals pointing to child IDDs)
	 * @return the constructed IDD
	 */
	public IDD buildFromIntervals(String varName, List<Edge> edges) {
		if (edges.isEmpty()) {
			return falseNode();
		}

		int varIndex = order.index(varName);

		return getNode(varIndex, edges);
	}

	/**
	 * Returns an IDD node for the given variable and edge list.
	 * <p>
	 * This method normalises the edge list (merges consecutive edges with the
	 * same child, fills gaps with FALSE edges), performs reduction (eliminates
	 * nodes with a single edge), and ensures hash-consing (returns the same
	 * object for identical inputs).
	 */
	public IDD getNode(int variable, List<Edge> rawEdges) {
		List<Edge> normalised = normaliseEdges(rawEdges);

		// Reduction: if only one edge covering the full domain, eliminate the
		// node.
		if (normalised.size() == 1) {
			Edge e = normalised.get(0);

			if (e.low() == Integer.MIN_VALUE && e.high() == Integer.MAX_VALUE) {
				return e.child();
			}
		}

		NodeKey key = new NodeKey(variable, normalised);
		return uniqueTable.computeIfAbsent(key, k -> new IDD(variable, normalised));
	}

	/**
	 * Normalises the edge list: 1. Sorts by low boundary. 2. Merges consecutive
	 * edges pointing to the same child. 3. Fills gaps with FALSE edges.
	 */
	private List<Edge> normaliseEdges(List<Edge> rawEdges) {
		if (rawEdges.isEmpty()) {
			throw new IllegalArgumentException("Edge list must not be empty");
		}

		// Sort by low boundary.
		List<Edge> sorted = new ArrayList<>(rawEdges);
		sorted.sort(Comparator.comparingInt(Edge::low));

		// Merge consecutive edges pointing to the same child.
		List<Edge> merged = new ArrayList<>();
		Edge current = sorted.get(0);

		for (int i = 1; i < sorted.size(); i++) {
			Edge next = sorted.get(i);

			if (current.child() == next.child() && current.high() + 1L == next.low()) {
				// Merge: extend the current interval.
				current = new Edge(current.low(), next.high(), current.child());
			} else {
				merged.add(current);
				current = next;
			}
		}

		merged.add(current);

		// Fill gaps with FALSE edges.
		List<Edge> filled = new ArrayList<>();
		int expectedLow = Integer.MIN_VALUE;

		for (Edge e : merged) {
			if (e.low() > expectedLow) {
				filled.add(new Edge(expectedLow, e.low() - 1, IDD.FALSE));
			}

			filled.add(e);
			long nextLow = (long) e.high() + 1;

			if (nextLow <= Integer.MAX_VALUE) {
				expectedLow = (int) nextLow;
			} else {
				expectedLow = Integer.MAX_VALUE;
			}
		}

		// Add trailing FALSE edge if needed.
		if (!filled.isEmpty() && filled.get(filled.size() - 1).high() < Integer.MAX_VALUE) {
			int trailingLow = filled.get(filled.size() - 1).high() + 1;

			if (trailingLow <= Integer.MAX_VALUE) {
				filled.add(new Edge(trailingLow, Integer.MAX_VALUE, IDD.FALSE));
			}
		}

		// Second merge pass: merge adjacent edges with the same child created
		// by gap filling.
		List<Edge> result = new ArrayList<>();
		Edge cur = filled.get(0);

		for (int i = 1; i < filled.size(); i++) {
			Edge next = filled.get(i);

			if (cur.child() == next.child() && cur.high() + 1L == next.low()) {
				cur = new Edge(cur.low(), next.high(), cur.child());
			} else {
				result.add(cur);
				cur = next;
			}
		}
		result.add(cur);

		return List.copyOf(result);
	}

	/**
	 * Key for the unique table.
	 */
	private static final class NodeKey {

		private final int variable;

		private final List<Edge> edges;

		private final int hashCode;

		NodeKey(int variable, List<Edge> edges) {
			this.variable = variable;
			this.edges = edges;
			this.hashCode = computeHashCode();
		}

		private int computeHashCode() {
			int h = 31 + variable;

			for (Edge e : edges) {
				h = 31 * h + e.low();
				h = 31 * h + e.high();
				h = 31 * h + System.identityHashCode(e.child());
			}

			return h;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			} else if (o == null || getClass() != o.getClass()) {
				return false;
			}

			NodeKey other = (NodeKey) o;

			if (this.variable != other.variable) {
				return false;
			} else if (this.edges.size() != other.edges.size()) {
				return false;
			}

			for (int i = 0; i < this.edges.size(); i++) {
				Edge a = this.edges.get(i), b = other.edges.get(i);

				if (a.low() != b.low() || a.high() != b.high() || a.child() != b.child()) {
					return false;
				}
			}

			return true;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}
	}
}
