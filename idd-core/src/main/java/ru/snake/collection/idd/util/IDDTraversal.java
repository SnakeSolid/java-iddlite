package ru.snake.collection.idd.util;

import java.util.IdentityHashMap;
import java.util.Map;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;

/**
 * Shared IDD traversal utilities used by {@link IDDPrinter} and
 * {@link MermaidExporter}.
 * <p>
 * Provides:
 * <ul>
 * <li>{@link Counter} — a simple incrementing integer counter for node
 * labels.</li>
 * <li>{@link #assignLabels(IDD, Map, Counter)} — DFS traversal assigning
 * {@code "n0"}, {@code "n1"}, ... labels to each node.</li>
 * <li>{@link #formatInterval(int, int, int, ValueFormatter)} — formats an edge
 * interval using the given {@link ValueFormatter}.</li>
 * </ul>
 */
public final class IDDTraversal {

	private IDDTraversal() {
	}

	/**
	 * DFS traversal that assigns a unique string label ({@code "n0"},
	 * {@code "n1"}, ...) to every reachable node.
	 *
	 * @param f       the root IDD node
	 * @param labels  map to populate with node-to-label mappings
	 * @param counter the label counter
	 */
	public static void assignLabels(IDD f, Map<IDD, String> labels, Counter counter) {
		if (labels.containsKey(f)) {
			return;
		}

		labels.put(f, "n" + counter.next());

		for (Edge e : f.edges()) {
			assignLabels(e.child(), labels, counter);
		}
	}

	/**
	 * Convenience overload that creates the counter and label map internally.
	 *
	 * @param f the root IDD node
	 * @return an IdentityHashMap mapping each reachable node to its label
	 */
	public static Map<IDD, String> assignLabels(IDD f) {
		Counter counter = new Counter();
		Map<IDD, String> labels = new IdentityHashMap<>();
		assignLabels(f, labels, counter);
		return labels;
	}

	/**
	 * Formats an edge interval using the given formatter.
	 *
	 * @param varIndex  the variable index for this node's edges
	 * @param low       interval low bound
	 * @param high      interval high bound
	 * @param formatter the value formatter
	 * @return a string like {@code [10.0.0.0,10.0.0.255]} or {@code [1,5]}
	 */
	public static String formatInterval(int varIndex, int low, int high, ValueFormatter formatter) {
		String lowStr = formatter.format(varIndex, low);
		String highStr = formatter.format(varIndex, high);
		return "[" + lowStr + "," + highStr + "]";
	}

	/**
	 * Simple incrementing counter for generating node labels.
	 */
	public static final class Counter {

		private int value;

		/** Returns the next sequential number (0, 1, 2, ...). */
		public int next() {
			return value++;
		}
	}
}
