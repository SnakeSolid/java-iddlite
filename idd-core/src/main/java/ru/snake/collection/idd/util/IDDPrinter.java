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
		Map<IDD, String> labels = IDDTraversal.assignLabels(f);

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
		Map<IDD, String> labels = IDDTraversal.assignLabels(f);

		StringBuilder sb = new StringBuilder();
		Set<IDD> printed = Collections.newSetFromMap(new IdentityHashMap<>());
		printCompactNode(f, order, labels, formatter, sb, 0, printed);

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
		Map<IDD, String> labels = IDDTraversal.assignLabels(f);

		StringBuilder sb = new StringBuilder();
		Set<IDD> printed = Collections.newSetFromMap(new IdentityHashMap<>());
		printTreeNode(f, order, labels, formatter, sb, "", true, printed);

		return sb.toString();
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
			String interval = IDDTraversal.formatInterval(varIndex, e.low(), e.high(), formatter);

			if (e.child().isTerminal()) {
				// Always print terminals inline
				String terminal = e.child().isTrue() ? "TRUE" : "FALSE";
				sb.append(prefix).append("  ").append(interval).append(" -> ").append(terminal).append("\n");
			} else if (printed.contains(e.child())) {
				// Child already printed elsewhere — show reference
				sb.append(prefix).append("  ").append(interval).append(" -> @").append(childLabel).append("\n");
			} else {
				sb.append(prefix).append("  ").append(interval).append(" ->\n");
				printNode(e.child(), order, labels, formatter, sb, indent + 1, printed);
			}
		}
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
		int indent,
		Set<IDD> printed
	) {
		String label = labels.get(f);

		// If already printed elsewhere, skip — callers show a reference.
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
		sb.append(prefix).append(label).append(" var=").append(varName);

		for (Edge e : f.edges()) {
			String childLabel = labels.get(e.child());
			String interval = IDDTraversal.formatInterval(varIndex, e.low(), e.high(), formatter);

			if (e.child().isTerminal()) {
				sb.append(" ").append(interval).append("]]->").append(childLabel);
			} else if (printed.contains(e.child())) {
				sb.append(" ").append(interval).append("]]->@").append(childLabel);
			} else {
				sb.append(" ").append(interval).append("]]->").append(childLabel);
			}
		}

		sb.append("\n");

		// Recurse into children that haven't been printed yet
		for (Edge e : f.edges()) {
			if (!e.child().isTerminal() && !printed.contains(e.child())) {
				printCompactNode(e.child(), order, labels, formatter, sb, indent + 1, printed);
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

	private static final String ARROW = " --> ";

	private static void printTreeNode(
		IDD f,
		VariableOrder order,
		Map<IDD, String> labels,
		ValueFormatter formatter,
		StringBuilder sb,
		String prefix,
		boolean isLast,
		Set<IDD> printed
	) {
		String label = labels.get(f);

		if (f.isTerminal()) {
			String terminal = f.isTrue() ? "TRUE" : "FALSE";
			String nodePrefix = isLast ? LAST_BRANCH : BRANCH;
			sb.append(prefix).append(nodePrefix).append(label).append(" ").append(terminal).append("\n");

			return;
		}

		// If already in printed set, the caller printed the header on its edge
		// line — skip printing it again, but expand grandchildren.
		boolean firstVisit = printed.add(f);

		String varName = order.name(f.variable());
		int varIndex = f.variable();

		if (prefix.isEmpty()) {
			// Root node
			sb.append(varName).append(" (").append(label).append(")\n");
		} else if (firstVisit) {
			// Header was printed on the edge line — don't repeat it
		} else {
			// First visit from a direct call
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
			String interval = IDDTraversal.formatInterval(varIndex, e.low(), e.high(), formatter);
			String linePrefix = childPrefix + (childIsLast ? LAST_BRANCH : BRANCH) + interval + ARROW;

			if (e.child().isTerminal()) {
				String terminal = e.child().isTrue() ? "TRUE" : "FALSE";
				sb.append(linePrefix).append(terminal).append("\n");
			} else if (printed.contains(e.child())) {
				// Shared — already printed from another edge
				String childVarName = order.name(e.child().variable());
				String childLabel = labels.get(e.child());
				sb.append(linePrefix).append(childVarName).append(" (@").append(childLabel).append(")\n");
			} else {
				// First visit — header on edge line, then expand grandchildren
				String childVarName = order.name(e.child().variable());
				String childLabel = labels.get(e.child());
				printed.add(e.child());
				sb.append(linePrefix).append(childVarName).append(" (").append(childLabel).append(")\n");
				String grandChildPrefix = childPrefix + (childIsLast ? BLANK : CONTINUATION);
				printTreeNode(e.child(), order, labels, formatter, sb, grandChildPrefix, true, printed);
			}
		}
	}
}
