package ru.snake.collection.idd.core;

import java.util.List;
import java.util.Objects;

/**
 * An Interval Decision Diagram (IDD) node.
 * <p>
 * Immutable, hash-consed, and reduced. Nodes are only created via
 * {@link ru.snake.collection.idd.factory.IDDFactory}.
 * <p>
 * Terminal nodes (TRUE/FALSE) have variable == -1 and an empty edge list.
 */
public final class IDD {

	public static final IDD TRUE = new IDD();

	public static final IDD FALSE = new IDD(true);

	private final int variable;

	private final List<Edge> edges;

	private final int hashCode;

	private IDD() {
		this.variable = -1;
		this.edges = List.of();
		this.hashCode = 0;
	}

	private IDD(boolean falseTerminal) {
		this.variable = -1;
		this.edges = List.of();
		this.hashCode = falseTerminal ? 1 : 0;
	}

	IDD(int variable, List<Edge> edges) {
		if (variable < 0) {
			throw new IllegalArgumentException("variable index must be >= 0 for internal nodes");
		}

		if (edges.isEmpty()) {
			throw new IllegalArgumentException("internal nodes must have at least one edge");
		}

		this.variable = variable;
		this.edges = edges;
		this.hashCode = computeHashCode();
	}

	/**
	 * Creates a new IDD node with the given variable and edges. Package-private
	 * so that IDDFactory can access it. Factory is in a different package, so
	 * this is used via reflection or a package.
	 */
	static IDD create(int variable, List<Edge> edges) {
		return new IDD(variable, edges);
	}

	/**
	 * Returns the variable index for this node, or -1 for terminals.
	 */
	public int variable() {
		return variable;
	}

	/**
	 * Returns the ordered list of edges for this node. Empty for terminals.
	 */
	public List<Edge> edges() {
		return edges;
	}

	/**
	 * Returns true if this is a terminal node (TRUE or FALSE).
	 */
	public boolean isTerminal() {
		return variable < 0;
	}

	/**
	 * Returns true if this is the TRUE terminal.
	 */
	public boolean isTrue() {
		return this == TRUE;
	}

	/**
	 * Returns true if this is the FALSE terminal.
	 */
	public boolean isFalse() {
		return this == FALSE;
	}

	private int computeHashCode() {
		return Objects.hash(
			variable,
			edges.stream().mapToInt(e -> Objects.hash(e.low(), e.high(), System.identityHashCode(e.child()))).sum()
		);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o == null || getClass() != o.getClass()) {
			return false;
		}

		// For hash-consed diagrams, reference equality implies structural
		// equality.
		return this == o;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		if (this == TRUE) {
			return "TRUE";
		} else if (this == FALSE) {
			return "FALSE";
		}

		return "IDD(var=" + variable + ", edges=" + edges + ")";
	}
}
