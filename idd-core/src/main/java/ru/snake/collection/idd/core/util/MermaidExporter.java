package ru.snake.collection.idd.core.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.IdentityHashMap;
import java.util.Map;
import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.VariableOrder;

/**
 * Exports an IDD to a Mermaid diagram.
 *
 * <p>
 * The generated output uses Mermaid's {@code graph TD} syntax, rendering
 * top-to-bottom flowcharts that can be viewed directly in Markdown-compatible
 * tools such as GitHub, GitLab, and the Zed editor.
 * </p>
 * <p>
 * Each method has an overload that accepts a {@link ValueFormatter} to
 * customise how edge interval values are displayed (e.g. IPv4 dotted-decimal,
 * protocol names). Defaults to {@link ValueFormatter#RAW}.
 * </p>
 */
public final class MermaidExporter {

	private MermaidExporter() {}

	// -----------------------------------------------------------------------
	// File-based exports
	// -----------------------------------------------------------------------

	/**
	 * Exports the IDD to a Mermaid diagram file at the given path.
	 *
	 * @param f        the root IDD
	 * @param order    the variable order (for readable node labels)
	 * @param filePath the output file path
	 */
	public static void export(IDD f, VariableOrder order, String filePath)
		throws IOException {
		export(f, order, ValueFormatter.RAW, filePath);
	}

	/**
	 * Exports the IDD to a Mermaid diagram file with custom value formatting.
	 *
	 * @param f         the root IDD
	 * @param order     the variable order
	 * @param formatter formats raw integer values in edge intervals
	 * @param filePath  the output file path
	 */
	public static void export(
		IDD f,
		VariableOrder order,
		ValueFormatter formatter,
		String filePath
	) throws IOException {
		try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
			pw.print(toString(f, order, formatter));
		}
	}

	/**
	 * Exports the IDD to a Mermaid diagram file using numeric variable labels.
	 *
	 * @param f        the root IDD
	 * @param filePath the output file path
	 */
	public static void export(IDD f, String filePath) throws IOException {
		export(f, ValueFormatter.RAW, filePath);
	}

	/**
	 * Exports the IDD to a Mermaid diagram file with custom value formatting.
	 *
	 * @param f         the root IDD
	 * @param formatter formats raw integer values in edge intervals
	 * @param filePath  the output file path
	 */
	public static void export(IDD f, ValueFormatter formatter, String filePath)
		throws IOException {
		try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
			pw.print(toString(f, formatter));
		}
	}

	// -----------------------------------------------------------------------
	// String-based exports
	// -----------------------------------------------------------------------

	/**
	 * Returns the IDD as a Mermaid diagram string using human-readable variable
	 * names from the given order.
	 *
	 * @param f     the root IDD
	 * @param order the variable order
	 * @return a Mermaid diagram string
	 */
	public static String toString(IDD f, VariableOrder order) {
		return toString(f, order, ValueFormatter.RAW);
	}

	/**
	 * Returns the IDD as a Mermaid diagram string with custom value formatting.
	 *
	 * @param f         the root IDD
	 * @param order     the variable order
	 * @param formatter formats raw integer values in edge intervals
	 * @return a Mermaid diagram string
	 */
	public static String toString(
		IDD f,
		VariableOrder order,
		ValueFormatter formatter
	) {
		Map<IDD, String> labels = IDDTraversal.assignLabels(f);

		StringBuilder sb = new StringBuilder();
		sb.append("graph TD\n");
		sb.append("    direction TB\n");

		for (Map.Entry<IDD, String> entry : labels.entrySet()) {
			IDD node = entry.getKey();
			String id = entry.getValue();

			if (node.isTerminal()) {
				String text = node.isTrue() ? "TRUE" : "FALSE";
				sb.append("    ")
					.append(id)
					.append("[\"")
					.append(text)
					.append("\"]:::terminal\n");
			} else {
				String varName = order.name(node.variable());
				sb.append("    ")
					.append(id)
					.append("[\"")
					.append(id)
					.append("<br/>var=")
					.append(escape(varName))
					.append("\"]\n");
			}
		}

		for (Map.Entry<IDD, String> entry : labels.entrySet()) {
			IDD node = entry.getKey();
			String fromId = entry.getValue();
			int varIndex = node.variable();

			for (Edge e : node.edges()) {
				String toId = labels.get(e.child());
				String interval = IDDTraversal.formatInterval(
					varIndex,
					e.low(),
					e.high(),
					formatter
				);
				sb.append("    ")
					.append(fromId)
					.append(" -->|\"")
					.append(interval)
					.append("\"| ")
					.append(toId)
					.append("\n");
			}
		}

		sb.append("\n");
		sb.append(
			"    classDef terminal fill:#c8e6c9,stroke:#2e7d32,stroke-width:2px,color:#1b5e20;"
		);
		return sb.toString();
	}

	/**
	 * Returns the IDD as a Mermaid diagram string using numeric variable
	 * labels.
	 *
	 * @param f the root IDD
	 * @return a Mermaid diagram string
	 */
	public static String toString(IDD f) {
		return toString(f, ValueFormatter.RAW);
	}

	/**
	 * Returns the IDD as a Mermaid diagram string with custom value formatting.
	 *
	 * @param f         the root IDD
	 * @param formatter formats raw integer values in edge intervals
	 * @return a Mermaid diagram string
	 */
	public static String toString(IDD f, ValueFormatter formatter) {
		StringBuilder sb = new StringBuilder();

		sb.append("graph TD\n");
		sb.append("    direction TB\n");

		visit(
			f,
			new IdentityHashMap<>(),
			sb,
			new IDDTraversal.Counter(),
			formatter
		);

		sb.append("\n");
		sb.append(
			"    classDef terminal fill:#c8e6c9,stroke:#2e7d32,stroke-width:2px,color:#1b5e20;"
		);
		return sb.toString();
	}

	// -----------------------------------------------------------------------
	// Internal helpers
	// -----------------------------------------------------------------------

	private static void visit(
		IDD f,
		Map<IDD, String> labels,
		StringBuilder sb,
		IDDTraversal.Counter counter,
		ValueFormatter formatter
	) {
		if (labels.containsKey(f)) {
			return;
		}

		String id = "n" + counter.next();
		labels.put(f, id);

		if (f.isTerminal()) {
			String text = f.isTrue() ? "TRUE" : "FALSE";
			sb.append("    ")
				.append(id)
				.append("[\"")
				.append(text)
				.append("\"]:::terminal\n");
		} else {
			int varIndex = f.variable();
			sb.append("    ")
				.append(id)
				.append("[\"")
				.append(id)
				.append("<br/>var=")
				.append(varIndex)
				.append("\"]\n");

			for (Edge e : f.edges()) {
				visit(e.child(), labels, sb, counter, formatter);
				String toId = labels.get(e.child());
				String interval = IDDTraversal.formatInterval(
					varIndex,
					e.low(),
					e.high(),
					formatter
				);
				sb.append("    ")
					.append(id)
					.append(" -->|\"")
					.append(interval)
					.append("\"| ")
					.append(toId)
					.append("\n");
			}
		}
	}

	/**
	 * Escapes characters that have special meaning in Mermaid node labels.
	 */
	private static String escape(String s) {
		return s
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}
}
