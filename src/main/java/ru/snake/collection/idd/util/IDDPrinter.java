package ru.snake.collection.idd.util;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.VariableOrder;

/**
 * Pretty-prints an IDD to a human-readable string.
 */
public final class IDDPrinter {

	private IDDPrinter() {
	}

	/**
	 * Prints the IDD as an indented text representation.
	 */
	public static String print(IDD f, VariableOrder order) {
		StringBuilder sb = new StringBuilder();
		printRecursive(f, order, sb, 0);
		return sb.toString();
	}

	private static void printRecursive(IDD f, VariableOrder order, StringBuilder sb, int indent) {
		String prefix = "  ".repeat(indent);

		if (f.isTerminal()) {
			sb.append(prefix).append(f.isTrue() ? "TRUE" : "FALSE").append("\n");
			return;
		}

		sb.append(prefix).append("var=").append(order.name(f.variable())).append("\n");

		for (Edge e : f.edges()) {
			sb.append(prefix).append("  [").append(e.low()).append(",").append(e.high()).append("] ->\n");
			printRecursive(e.child(), order, sb, indent + 2);
		}
	}
}
