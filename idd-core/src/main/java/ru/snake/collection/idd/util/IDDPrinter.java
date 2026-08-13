package ru.snake.collection.idd.util;

import java.util.IdentityHashMap;
import java.util.Map;
import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.VariableOrder;

/**
 * Utility for printing IDD diagrams in human-readable formats.
 * <p>
 * Provides three modes:
 * <ul>
 *   <li>{@link #print(IDD, VariableOrder)} — indented with node labels for shared subtrees</li>
 *   <li>{@link #printCompact(IDD, VariableOrder)} — one-line summary per node</li>
 *   <li>{@link #printTree(IDD, VariableOrder)} — tree-style with box-drawing characters</li>
 * </ul>
 * <p>
 * All modes handle shared nodes correctly by assigning unique labels,
 * since IDDs are DAGs created via hash-consing.
 */
public final class IDDPrinter {

	private IDDPrinter() {}

	/**
	 * Prints the IDD as an indented text representation.
	 * <p>
	 * Shared nodes are printed once with a label (e.g. <code>@n3</code>),
	 * and subsequent references point to that label.
	 *
	 * @param f     the root IDD node
	 * @param order the variable order (for readable variable names)
	 * @return the formatted string
	 */
	public static String print(IDD f, VariableOrder order) {
		Counter counter = new Counter();
		Map<IDD, String> labels = new IdentityHashMap<>();
		assignLabels(f, labels, counter);

		StringBuilder sb = new StringBuilder();
		printNode(f, order, labels, sb, 0);
		return sb.toString();
	}

	// ------------------------------------------------------------------
	// Compact mode — one summary line per node
	// ------------------------------------------------------------------

	/**
	 * Returns a compact one-line summary per node.
	 * <p>
	 * Shared nodes are labeled and referenced similarly to {@link #print(IDD, VariableOrder)}.
	 *
	 * @param f     the root IDD node
	 * @param order the variable order
	 * @return the compact string
	 */
	public static String printCompact(IDD f, VariableOrder order) {
		Counter counter = new Counter();
		Map<IDD, String> labels = new IdentityHashMap<>();
		assignLabels(f, labels, counter);

		StringBuilder sb = new StringBuilder();
		printCompactNode(f, order, labels, sb, 0);
		return sb.toString();
	}

	// ------------------------------------------------------------------
	// Tree mode — visual tree with box-drawing characters
	// ------------------------------------------------------------------

	/**
	 * Prints the IDD as a tree diagram using box-drawing characters.
	 * <p>
	 * Example output:
	 * <pre>
	 * x
	 * ├── [0,0] ──► FALSE
	 * └── [1,10] ──► y
	 *     ├── [0,5] ──► TRUE
	 *     └── [6,10] ──► FALSE
	 * </pre>
	 *
	 * @param f     the root IDD node
	 * @param order the variable order
	 * @return the tree diagram as a string
	 */
	public static String printTree(IDD f, VariableOrder order) {
		Counter counter = new Counter();
		Map<IDD, String> labels = new IdentityHashMap<>();
		assignLabels(f, labels, counter);

		StringBuilder sb = new StringBuilder();
		printTreeNode(f, order, labels, sb, "", true);
		return sb.toString();
	}

	// ================================================================
	// Internal: label assignment (shared across all modes)
	// ================================================================

	private static void assignLabels(
		IDD f,
		Map<IDD, String> labels,
		Counter counter
	) {
		if (labels.containsKey(f)) {
			return;
		}

		labels.put(f, "n" + counter.next());

		for (Edge e : f.edges()) {
			assignLabels(e.child(), labels, counter);
		}
	}

	// ================================================================
	// Internal: indented print mode
	// ================================================================

	private static void printNode(
		IDD f,
		VariableOrder order,
		Map<IDD, String> labels,
		StringBuilder sb,
		int indent
	) {
		String prefix = "  ".repeat(indent);
		String label = labels.get(f);

		if (f.isTerminal()) {
			String terminal = f.isTrue() ? "TRUE" : "FALSE";
			sb.append(prefix)
				.append(label)
				.append(" ")
				.append(terminal)
				.append("\n");
			return;
		}

		String varName = order.name(f.variable());
		sb.append(prefix)
			.append(label)
			.append(" var=")
			.append(varName)
			.append("\n");

		for (Edge e : f.edges()) {
			String childLabel = labels.get(e.child());

			if (f.isTrue() || f.isFalse()) {
				// unreachable for internal nodes, but keep it safe
				sb.append(prefix)
					.append("  [")
					.append(e.low())
					.append(",")
					.append(e.high())
					.append("] -> ")
					.append(childLabel)
					.append("\n");
			} else if (
				labels
					.values()
					.stream()
					.filter(l -> l.equals(childLabel))
					.count() > 1 ||
				isSharedChild(f, e.child(), labels)
			) {
				// This child is reachable from another node — show as reference
				sb.append(prefix)
					.append("  [")
					.append(e.low())
					.append(",")
					.append(e.high())
					.append("] -> @")
					.append(childLabel)
					.append("\n");
			} else {
				sb.append(prefix)
					.append("  [")
					.append(e.low())
					.append(",")
					.append(e.high())
					.append("] ->\n");
				printNode(e.child(), order, labels, sb, indent + 1);
			}
		}
	}

	/**
	 * Checks if a child node is referenced from other nodes in the diagram.
	 */
	private static boolean isSharedChild(
		IDD parent,
		IDD child,
		Map<IDD, String> labels
	) {
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

	// ================================================================
	// Internal: compact print mode
	// ================================================================

	private static void printCompactNode(
		IDD f,
		VariableOrder order,
		Map<IDD, String> labels,
		StringBuilder sb,
		int indent
	) {
		String prefix = "  ".repeat(indent);
		String label = labels.get(f);

		if (f.isTerminal()) {
			String terminal = f.isTrue() ? "TRUE" : "FALSE";
			sb.append(prefix)
				.append(label)
				.append(" ")
				.append(terminal)
				.append("\n");
			return;
		}

		String varName = order.name(f.variable());
		sb.append(prefix).append(label).append(" var=").append(varName);

		for (Edge e : f.edges()) {
			String childLabel = labels.get(e.child());

			if (isSharedChild(f, e.child(), labels)) {
				sb.append(" [")
					.append(e.low())
					.append(",")
					.append(e.high())
					.append("]->@")
					.append(childLabel);
			} else {
				sb.append(" [")
					.append(e.low())
					.append(",")
					.append(e.high())
					.append("]->")
					.append(childLabel);
			}
		}

		sb.append("\n");

		// Print non-shared children inline (indented)
		for (Edge e : f.edges()) {
			if (!isSharedChild(f, e.child(), labels)) {
				printCompactNode(e.child(), order, labels, sb, indent + 1);
			}
		}
	}

	// ================================================================
	// Internal: tree print mode
	// ================================================================

	private static final String BRANCH = "├── ";
	private static final String LAST_BRANCH = "└── ";
	private static final String CONTINUATION = "│   ";
	private static final String BLANK = "    ";
	private static final String ARROW = " ──► ";

	private static void printTreeNode(
		IDD f,
		VariableOrder order,
		Map<IDD, String> labels,
		StringBuilder sb,
		String prefix,
		boolean isLast
	) {
		String label = labels.get(f);

		if (f.isTerminal()) {
			String terminal = f.isTrue() ? "TRUE" : "FALSE";
			String nodePrefix = isLast ? LAST_BRANCH : BRANCH;
			sb.append(prefix)
				.append(nodePrefix)
				.append(label)
				.append(" ")
				.append(terminal)
				.append("\n");
			return;
		}

		String varName = order.name(f.variable());
		if (prefix.isEmpty()) {
			// Root node — no prefix characters
			sb.append(varName).append(" (").append(label).append(")\n");
		} else {
			String nodePrefix = isLast ? LAST_BRANCH : BRANCH;
			sb.append(prefix)
				.append(nodePrefix)
				.append(varName)
				.append(" (")
				.append(label)
				.append(")\n");
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
			String interval = "[" + e.low() + "," + e.high() + "]";

			String linePrefix =
				childPrefix +
				(childIsLast ? LAST_BRANCH : BRANCH) +
				interval +
				ARROW;

			if (e.child().isTerminal()) {
				String terminal = e.child().isTrue() ? "TRUE" : "FALSE";
				sb.append(linePrefix)
					.append(childLabel)
					.append(" ")
					.append(terminal)
					.append("\n");
			} else if (isSharedChild(f, e.child(), labels)) {
				String childVarName = order.name(e.child().variable());
				sb.append(linePrefix)
					.append(childVarName)
					.append(" (@")
					.append(childLabel)
					.append(")\n");
			} else {
				String childVarName = order.name(e.child().variable());
				sb.append(linePrefix)
					.append(childVarName)
					.append(" (")
					.append(childLabel)
					.append(")\n");
				String grandChildPrefix;
				if (childIsLast) {
					grandChildPrefix = childPrefix + BLANK;
				} else {
					grandChildPrefix = childPrefix + CONTINUATION;
				}
				printTreeNode(
					e.child(),
					order,
					labels,
					sb,
					grandChildPrefix,
					childIsLast
				);
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
