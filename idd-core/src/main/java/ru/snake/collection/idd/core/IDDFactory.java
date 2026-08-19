package ru.snake.collection.idd.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import ru.snake.collection.idd.core.util.VariableRange;

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

	private final VariableRanges ranges;

	private final Map<NodeKey, IDD> uniqueTable;

	private final Map<OperationKey, IDD> applyCache = new WeakHashMap<>();

	/**
	 * Constructs a factory with full integer ranges for all variables.
	 *
	 * @param order the variable order
	 */
	public IDDFactory(VariableOrder order) {
		this(order, new VariableRanges(Map.of(), order));
	}

	/**
	 * Constructs a factory with custom variable ranges.
	 *
	 * @param order  the variable order
	 * @param ranges the per-variable range constraints
	 */
	public IDDFactory(VariableOrder order, VariableRanges ranges) {
		this.order = Objects.requireNonNull(order);
		this.ranges = Objects.requireNonNull(ranges);
		this.uniqueTable = new WeakHashMap<>();
	}

	public VariableOrder order() {
		return order;
	}

	/**
	 * Returns the variable range registry for this factory.
	 */
	public VariableRanges ranges() {
		return ranges;
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
	 *
	 * @param variable the variable index
	 * @param rawEdges the edges for this node
	 * @return the constructed or cached IDD node
	 * @throws IllegalArgumentException if any edge is outside the variable's
	 *                                      valid range
	 */
	public IDD getNode(int variable, List<Edge> rawEdges) {
		VariableRange range = ranges.range(variable);

		// Validate raw edges are within the variable's range.
		for (Edge e : rawEdges) {
			if (e.low() < range.min() || e.high() > range.max()) {
				throw new IllegalArgumentException(
					"Edge [" +
						e.low() +
						"," +
						e.high() +
						"] out of range [" +
						range.min() +
						"," +
						range.max() +
						"] for variable " +
						variable
				);
			}
		}

		List<Edge> normalised = normaliseEdges(variable, rawEdges);

		// Reduction: if only one edge covering the full range, eliminate the
		// node.
		if (normalised.size() == 1) {
			Edge e = normalised.get(0);

			if (e.low() == range.min() && e.high() == range.max()) {
				return e.child();
			}
		}

		NodeKey key = new NodeKey(variable, normalised);
		return uniqueTable.computeIfAbsent(key, k ->
			new IDD(variable, normalised)
		);
	}

	/**
	 * Normalises the edge list: 1. Sorts by low boundary. 2. Merges consecutive
	 * edges pointing to the same child. 3. Fills gaps with FALSE edges.
	 * <p>
	 * Gap filling uses the variable's valid range as boundaries instead of the
	 * full integer range.
	 *
	 * @param variable the variable index (used to look up the range)
	 * @param rawEdges the edges to normalise
	 * @return the normalised, unmodifiable edge list
	 * @throws IllegalArgumentException if the edge list is empty
	 */
	private List<Edge> normaliseEdges(int variable, List<Edge> rawEdges) {
		if (rawEdges.isEmpty()) {
			throw new IllegalArgumentException("Edge list must not be empty");
		}

		VariableRange range = ranges.range(variable);
		int minVal = range.min();
		int maxVal = range.max();

		// Sort by low boundary.
		List<Edge> sorted = new ArrayList<>(rawEdges);
		sorted.sort(Comparator.comparingInt(Edge::low));

		// Merge consecutive edges pointing to the same child.
		List<Edge> merged = new ArrayList<>();
		Edge current = sorted.get(0);

		for (int i = 1; i < sorted.size(); i++) {
			Edge next = sorted.get(i);

			if (
				current.child() == next.child() &&
				current.high() + 1L == next.low()
			) {
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
		int expectedLow = minVal;

		for (Edge e : merged) {
			if (e.low() > expectedLow) {
				filled.add(new Edge(expectedLow, e.low() - 1, IDD.FALSE));
			}

			filled.add(e);
			long nextLow = (long) e.high() + 1;

			if (nextLow <= maxVal) {
				expectedLow = (int) nextLow;
			} else {
				expectedLow = maxVal;
			}
		}

		// Add trailing FALSE edge if needed.
		if (
			!filled.isEmpty() && filled.get(filled.size() - 1).high() < maxVal
		) {
			int trailingLow = filled.get(filled.size() - 1).high() + 1;

			if (trailingLow <= maxVal) {
				filled.add(new Edge(trailingLow, maxVal, IDD.FALSE));
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
				Edge a = this.edges.get(i),
					b = other.edges.get(i);

				if (
					a.low() != b.low() ||
					a.high() != b.high() ||
					a.child() != b.child()
				) {
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

	@Override
	public String toString() {
		return (
			"IDDFactory[order=" + order + ", nodes=" + uniqueTable.size() + "]"
		);
	}

	// ---- Apply operations ----

	/**
	 * Computes the logical AND of two IDDs.
	 * <p>
	 * Memoised via a {@link WeakHashMap} cache so repeated subcomputations on
	 * the same operands are avoided.
	 *
	 * @param f the left operand
	 * @param g the right operand
	 * @return {@code f AND g}
	 */
	public IDD and(IDD f, IDD g) {
		return apply(f, g, Operation.AND);
	}

	/**
	 * Computes the logical OR of two IDDs.
	 *
	 * @param f the left operand
	 * @param g the right operand
	 * @return {@code f OR g}
	 */
	public IDD or(IDD f, IDD g) {
		return apply(f, g, Operation.OR);
	}

	/**
	 * Computes the logical XOR of two IDDs.
	 *
	 * @param f the left operand
	 * @param g the right operand
	 * @return {@code f XOR g}
	 */
	public IDD xor(IDD f, IDD g) {
		return apply(f, g, Operation.XOR);
	}

	/**
	 * Computes the logical implication {@code f → g}.
	 *
	 * @param f the antecedent
	 * @param g the consequent
	 * @return {@code !f OR g}
	 */
	public IDD implies(IDD f, IDD g) {
		return apply(f, g, Operation.IMPLIES);
	}

	/**
	 * Computes the logical NOT of an IDD.
	 *
	 * @param f the operand
	 * @return {@code NOT f}
	 */
	public IDD not(IDD f) {
		if (f.isTerminal()) {
			return f.isTrue() ? IDD.FALSE : IDD.TRUE;
		}

		List<Edge> newEdges = new ArrayList<>();
		for (Edge ef : f.edges()) {
			IDD child = not(ef.child());
			newEdges.add(new Edge(ef.low(), ef.high(), child));
		}
		return getNode(f.variable(), newEdges);
	}

	/**
	 * Core apply: combines two IDDs under the given connective. Uses
	 * memoisation to avoid redundant recursive work.
	 */
	private IDD apply(IDD f, IDD g, Operation op) {
		OperationKey key = new OperationKey(f, g, op);
		return applyCache.computeIfAbsent(key, k -> applyRecursive(f, g, op));
	}

	private IDD applyRecursive(IDD f, IDD g, Operation op) {
		// Base: both terminals.
		if (f.isTerminal() && g.isTerminal()) {
			boolean result = op.apply(f.isTrue(), g.isTrue());
			return result ? IDD.TRUE : IDD.FALSE;
		}

		if (f.isTerminal()) {
			return applyTerminalLeft(f, g, op);
		}
		if (g.isTerminal()) {
			return applyTerminalRight(f, g, op);
		}

		int fVar = f.variable();
		int gVar = g.variable();

		if (fVar == gVar) {
			return applySameVar(f, g, fVar, op);
		} else if (fVar < gVar) {
			return applyHigherVar(f, g, op);
		} else {
			return applyLowerVar(f, g, op);
		}
	}

	private IDD applySameVar(IDD f, IDD g, int var, Operation op) {
		List<Edge> newEdges = new ArrayList<>();
		int i = 0,
			j = 0;
		List<Edge> fEdges = f.edges(),
			gEdges = g.edges();

		while (i < fEdges.size() && j < gEdges.size()) {
			Edge ef = fEdges.get(i),
				eg = gEdges.get(j);
			int lo = Math.max(ef.low(), eg.low());
			int hi = Math.min(ef.high(), eg.high());

			if (lo <= hi) {
				IDD child = applyRecursive(ef.child(), eg.child(), op);
				newEdges.add(new Edge(lo, hi, child));
			}

			if (ef.high() < eg.high()) {
				i++;
			} else if (eg.high() < ef.high()) {
				j++;
			} else {
				i++;
				j++;
			}
		}

		if (newEdges.isEmpty()) {
			return IDD.FALSE;
		}
		return getNode(var, newEdges);
	}

	/**
	 * f's variable is earlier in the order (higher in the diagram).
	 */
	private IDD applyHigherVar(IDD f, IDD g, Operation op) {
		List<Edge> newEdges = new ArrayList<>();
		for (Edge ef : f.edges()) {
			IDD child = applyRecursive(ef.child(), g, op);
			newEdges.add(new Edge(ef.low(), ef.high(), child));
		}
		return getNode(f.variable(), newEdges);
	}

	/**
	 * g's variable is earlier in the order (higher in the diagram).
	 */
	private IDD applyLowerVar(IDD f, IDD g, Operation op) {
		List<Edge> newEdges = new ArrayList<>();
		for (Edge eg : g.edges()) {
			IDD child = applyRecursive(f, eg.child(), op);
			newEdges.add(new Edge(eg.low(), eg.high(), child));
		}
		return getNode(g.variable(), newEdges);
	}

	private IDD applyTerminalLeft(IDD fTerm, IDD g, Operation op) {
		List<Edge> newEdges = new ArrayList<>();
		for (Edge eg : g.edges()) {
			IDD child = applyRecursive(fTerm, eg.child(), op);
			newEdges.add(new Edge(eg.low(), eg.high(), child));
		}
		return getNode(g.variable(), newEdges);
	}

	private IDD applyTerminalRight(IDD f, IDD gTerm, Operation op) {
		List<Edge> newEdges = new ArrayList<>();
		for (Edge ef : f.edges()) {
			IDD child = applyRecursive(ef.child(), gTerm, op);
			newEdges.add(new Edge(ef.low(), ef.high(), child));
		}
		return getNode(f.variable(), newEdges);
	}

	/**
	 * Cache key for Boolean operations. Uses stable {@link Operation} enum
	 * instead of {@code System.identityHashCode} on lambdas.
	 */
	private static final class OperationKey {

		private final IDD f, g;
		private final Operation op;

		OperationKey(IDD f, IDD g, Operation op) {
			this.f = f;
			this.g = g;
			this.op = op;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			} else if (o == null || getClass() != o.getClass()) {
				return false;
			}

			OperationKey other = (OperationKey) o;
			return f == other.f && g == other.g && op == other.op;
		}

		@Override
		public int hashCode() {
			return Objects.hash(
				System.identityHashCode(f),
				System.identityHashCode(g),
				op
			);
		}
	}
}
