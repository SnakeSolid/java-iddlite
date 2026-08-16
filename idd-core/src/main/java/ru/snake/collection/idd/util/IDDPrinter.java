package ru.snake.collection.idd.util;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.VariableOrder;

/**
 * Utility for printing IDD diagrams in human-readable formats.
 * <p>
 * Provides three modes:
 * <ul>
 * <li>{@link #print(IDD, VariableOrder)} — indented with node labels for shared
 * subtrees</li>
 * <li>{@link #printCompact(IDD, VariableOrder)} — one-line summary per
 * node</li>
 * <li>{@link #printTree(IDD, VariableOrder)} — tree-style with box-drawing
 * characters</li>
 * </ul>
 * <p>
 * Each mode has an overload that accepts a {@link ValueFormatter} to customise
 * how edge values are displayed (e.g. IPv4 dotted-decimal, protocol names).
 * <p>
 * All modes handle shared nodes correctly by assigning unique labels, since
 * IDDs are DAGs created via hash-consing.
 */
public final class IDDPrinter {

	private IDDPrinter() {
	}

	// ==================================================================
	// Indented mode
	// ==================================================================

	/**
	 * Prints the IDD as an indented text representation.
	 * <p>
	 * Shared internal nodes are printed once with a label (e.g. {@code @n3}),
	 * and subsequent references point to that label. Terminal nodes
	 * ({@code TRUE}, {@code FALSE}) are always printed inline as constants
	 * rather than as references.
	 *
	 * @param f     the root IDD node
	 * @param order the variable order (for readable variable names)
	 * @return the formatted string
	 */
	public static String print(IDD f, VariableOrder order) {
		return print(f, order, ValueFormatter.RAW);
	}

	/**
	 * Prints the IDD as an indented text representation with custom value
	 * formatting for edge intervals.
	 * <p>
	 * Shared internal nodes are printed once with a label (e.g. {@code @n3}),
	 * and subsequent references point to that label. Terminal nodes
	 * ({@code TRUE}, {@code FALSE}) are always printed inline as constants.
	 *
	 * @param f         the root IDD node
	 * @param order     the variable order (for readable variable names)
	 * @param formatter formats raw integer values (e.g. IP address, protocol
	 *                      name)
	 * @return the formatted string
	 */
	public static String print(IDD f, VariableOrder order, ValueFormatter formatter) {
		Counter counter = new Counter();
		Map<IDD, String> labels = new IdentityHashMap<>();
		assignLabels(f, labels, counter);

		StringBuilder sb = new StringBuilder();
		Set<IDD> printed = Collections.newSetFromMap(new IdentityHashMap<>());
		printNode(f, order, labels, formatter, sb, 0, printed);

		return sb.toString();
	}

	// ==================================================================
	// Compact mode — one summary line per node
	// ==================================================================

	/**
	 * Returns a compact one-line summary per node.
	 * <p>
	 * Shared nodes are labeled and referenced similarly to
	 * {@link #print(IDD, VariableOrder)}.
	 *
	 * @param f     the root IDD node
	 * @param order the variable order
	 * @return the compact string
	 */
	public static String printCompact(IDD f, VariableOrder order) {
		return printCompact(f, order, ValueFormatter.RAW);
	}

	/**
	 * Returns a compact one-line summary per node with custom value formatting.
	 *
	 * @param f         the root IDD node
	 * @param order     the variable order
	 * @param formatter formats raw integer values
	 * @return the compact string
	 */
	public static String printCompact(IDD f, VariableOrder order, ValueFormatter formatter) {
		Counter counter = new Counter();
		Map<IDD, String> labels = new IdentityHashMap<>();
		assignLabels(f, labels, counter);

		StringBuilder sb = new StringBuilder();
		printCompactNode(f, order, labels, formatter, sb, 0);

		return sb.toString();
	}

	// ==================================================================
	// Tree mode — visual tree with box-drawing characters
	// ==================================================================

	/**
	 * Prints the IDD as a tree diagram using box-drawing characters.
	 * <p>
	 * Example output:
	 *
	 * <pre>
	 * x
	 * ├─ [0,0] ──► FALSE
	 * └─ [1,10] ──► y
	 *    ├─ [0,5] ──► TRUE
	 *    └─ [6,10] ──► FALSE
	 * </pre>
	 *
	 * @param f     the root IDD node
	 * @param order the variable order
	 * @return the tree diagram as a string
	 */
	public static String printTree(IDD f, VariableOrder order) {
		return printTree(f, order, ValueFormatter.RAW);
	}

	/**
	 * Prints the IDD as a tree diagram with custom value formatting.
	 *
	 * @param f         the root IDD node
	 * @param order     the variable order
	 * @param formatter formats raw integer values
	 * @return the tree diagram as a string
	 */
	public static String printTree(IDD f, VariableOrder order, ValueFormatter formatter) {
		Counter counter = new Counter();
		Map<IDD, String> labels = new IdentityHashMap<>();
		assignLabels(f, labels, counter);

		StringBuilder sb = new StringBuilder();
		printTreeNode(f, order, labels, formatter, sb, "", true);

		return sb.toString();
	}

	// ==================================================================
	// Internal: label assignment (shared across all modes)
	// ==================================================================

	private static void assignLabels(IDD f, Map<IDD, String> labels, Counter counter) {
		if (labels.containsKey(f)) {
			return;
		}

		labels.put(f, "n" + counter.next());

		for (Edge e : f.edges()) {
			assignLabels(e.child(), labels, counter);
		}
	}

	// ==================================================================
	// Internal: interval formatting
	// ==================================================================

	/**
	 * Formats an edge interval using the given formatter.
	 *
	 * @param varIndex  the variable index for this node's edges
	 * @param low       interval low bound
	 * @param high      interval high bound
	 * @param formatter the value formatter
	 * @return a string like {@code [10.0.0.0,10.0.0.255]} or {@code [1,5]}
	 */
	private static String formatInterval(int varIndex, int low, int high, ValueFormatter formatter) {
		String lowStr = formatter.format(varIndex, low);
		String highStr = formatter.format(varIndex, high);
		return "[" + lowStr + "," + highStr + "]";
	}

	// ==================================================================
	// Internal: indented print mode
	// ==================================================================

	private static void printNode(
		IDD f,
		VariableOrder order,
		Map<IDD, String> labels,
		ValueFormatter formatter,
		StringBuilder sb,
		int indent,
		Set<IDD> printed
	) {
		String label = labels.get(f);

		// If already printed elsewhere, we still came here via recursion —
		// just skip to avoid duplicate output. The caller will have printed
		// the reference (@label) before recursing.
		if (!printed.add(f)) {
			return;
		}

		String prefix = "  ".repeat(indent);

		if (f.isTerminal()) {
			String terminal = f.isTrue() ? "TRUE" : "FALSE";
			sb.append(prefix).append(label).append(" ").append(terminal).append("\n");

			return;
		}

		String varName = order.name(f.variable());
		int varIndex = f.variable();
		sb.append(prefix).append(label).append(" var=").append(varName).append("\n");

		for (Edge e : f.edges()) {
			String childLabel = labels.get(e.child());
			String interval = formatInterval(varIndex, e.low(), e.high(), formatter);

			if (e.child().isTerminal()) {
				// Always print terminals inline
				String terminal = e.child().isTrue() ? "TRUE" : "FALSE";
				sb.append(prefix).append("  ").append(interval).append(" -> ").append(terminal).append("\n");
			} else if (isSharedChild(f, e.child(), labels)) {
				// Shared child — show reference, then print inline if first
				// visit
				sb.append(prefix).append("  ").append(interval).append(" -> @").append(childLabel).append("\n");
				printNode(e.child(), order, labels, formatter, sb, indent + 1, printed);
			} else {
				sb.append(prefix).append("  ").append(interval).append(" ->\n");
				printNode(e.child(), order, labels, formatter, sb, indent + 1, printed);
			}
		}
	}

	/**
	 * Checks if a child node is referenced from other nodes in the diagram.
	 */
	private static boolean isSharedChild(IDD parent, IDD child, Map<IDD, String> labels) {
		for (Map.Entry<IDD, String> entry : labels.entrySet()) {
			IDD node = entry.getKey();

			if (node == parent) {
				continue;
			}

			if (node.isTerminal()) {
				continue;
			}

			for (Edge e : node.edges()) {
				if (e.child() == child) {
					return true;
				}
			}
		}

		return false;
	}

	// ==================================================================
	// Internal: compact print mode
	// ==================================================================

	private static void printCompactNode(
		IDD f,
		VariableOrder order,
		Map<IDD, String> labels,
		ValueFormatter formatter,
		StringBuilder sb,
		int indent
	) {
		String prefix = "  ".repeat(indent);
		String label = labels.get(f);

		if (f.isTerminal()) {
			String terminal = f.isTrue() ? "TRUE" : "FALSE";
			sb.append(prefix).append(label).append(" ").append(terminal).append("\n");

			return;
		}

		String varName = order.name(f.variable());
		int varIndex = f.variable();
		sb.append(prefix).append(label).append(" var=").append(varName);

		for (Edge e : f.edges()) {
			String childLabel = labels.get(e.child());
			String interval = formatInterval(varIndex, e.low(), e.high(), formatter);

			if (isSharedChild(f, e.child(), labels)) {
				sb.append(" ").append(interval).append("]->@").append(childLabel);
			} else {
				sb.append(" ").append(interval).append("]->").append(childLabel);
			}
		}

		sb.append("\n");

		// Print non-shared children inline (indented)
		for (Edge e : f.edges()) {
			if (!isSharedChild(f, e.child(), labels)) {
				printCompactNode(e.child(), order, labels, formatter, sb, indent + 1);
			}
		}
	}

	// ==================================================================
	// Internal: tree print mode
	// ==================================================================

	private static final String BRANCH = "├─ ";

	private static final String LAST_BRANCH = "└─ ";

	private static final String CONTINUATION = "│  ";

	private static final String BLANK = "   ";

	private static final String ARROW = " ──► ";

	private static void printTreeNode(
		IDD f,
		VariableOrder order,
		Map<IDD, String> labels,
		ValueFormatter formatter,
		StringBuilder sb,
		String prefix,
		boolean isLast
	) {
		String label = labels.get(f);

		if (f.isTerminal()) {
			String terminal = f.isTrue() ? "TRUE" : "FALSE";
			String nodePrefix = isLast ? LAST_BRANCH : BRANCH;
			sb.append(prefix).append(nodePrefix).append(label).append(" ").append(terminal).append("\n");

			return;
		}

		String varName = order.name(f.variable());
		int varIndex = f.variable();

		if (prefix.isEmpty()) {
			// Root node — no prefix characters
			sb.append(varName).append(" (").append(label).append(")\n");
		} else {
			String nodePrefix = isLast ? LAST_BRANCH : BRANCH;
			sb.append(prefix).append(nodePrefix).append(varName).append(" (").append(label).append(")\n");
		}

		String childPrefix;

		if (prefix.isEmpty()) {
			childPrefix = "";
		} else if (isLast) {
			childPrefix = prefix + BLANK;
		} else {
			childPrefix = prefix + CONTINUATION;
		}

		int edgeCount = f.edges().size();
		int index = 0;

		for (Edge e : f.edges()) {
			index++;
			boolean childIsLast = index == edgeCount;
			String childLabel = labels.get(e.child());
			String interval = formatInterval(varIndex, e.low(), e.high(), formatter);
			String linePrefix = childPrefix + (childIsLast ? LAST_BRANCH : BRANCH) + interval + ARROW;

			if (e.child().isTerminal()) {
				String terminal = e.child().isTrue() ? "TRUE" : "FALSE";
				sb.append(linePrefix).append(childLabel).append(" ").append(terminal).append("\n");
			} else if (isSharedChild(f, e.child(), labels)) {
				String childVarName = order.name(e.child().variable());
				sb.append(linePrefix).append(childVarName).append(" (@").append(childLabel).append(")\n");
			} else {
				String childVarName = order.name(e.child().variable());
				sb.append(linePrefix).append(childVarName).append(" (").append(childLabel).append(")\n");
				String grandChildPrefix;

				if (childIsLast) {
					grandChildPrefix = childPrefix + BLANK;
				} else {
					grandChildPrefix = childPrefix + CONTINUATION;
				}

				printTreeNode(e.child(), order, labels, formatter, sb, grandChildPrefix, childIsLast);
			}
		}
	}

	/**
	 * Simple incrementing counter for node labels.
	 */
	private static final class Counter {

		private int value;

		int next() {
			return value++;
		}
	}
}
