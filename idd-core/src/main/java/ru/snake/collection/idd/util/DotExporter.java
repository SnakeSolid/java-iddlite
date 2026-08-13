package ru.snake.collection.idd.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.IdentityHashMap;
import java.util.Map;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.VariableOrder;

/**
 * Exports an IDD to a Graphviz DOT file.
 */
public final class DotExporter {

	private DotExporter() {
	}

	/**
	 * Exports the IDD to a DOT file at the given path.
	 *
	 * @param f        the root IDD
	 * @param order    the variable order (for readable node labels)
	 * @param filePath the output file path
	 */
	public static void export(IDD f, VariableOrder order, String filePath) throws IOException {
		Counter counter = new Counter();
		Map<IDD, String> labels = new IdentityHashMap<>();
		assignLabels(f, labels, counter);

		try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
			pw.println("digraph IDD {");
			pw.println("  rankdir=TB;");
			pw.println("  node [shape=ellipse];");

			for (Map.Entry<IDD, String> entry : labels.entrySet()) {
				IDD node = entry.getKey();
				String label = entry.getValue();

				if (node.isTerminal()) {
					pw.printf("  %s [label=\"%s\" shape=doublecircle];\n", label, node.isTrue() ? "TRUE" : "FALSE");
				} else {
					pw.printf("  %s [label=\"%s\\nvar=%s\"];\n", label, label, order.name(node.variable()));
				}
			}

			for (Map.Entry<IDD, String> entry : labels.entrySet()) {
				IDD node = entry.getKey();
				String fromLabel = entry.getValue();

				for (Edge e : node.edges()) {
					String toLabel = labels.get(e.child());
					pw.printf("  %s -> %s [label=\"%s\"];\n", fromLabel, toLabel, e.interval());
				}
			}

			pw.println("}");
		}
	}

	private static void assignLabels(IDD f, Map<IDD, String> labels, Counter counter) {
		if (labels.containsKey(f)) {
			return;
		}

		labels.put(f, "n" + counter.next());

		for (Edge e : f.edges()) {
			assignLabels(e.child(), labels, counter);
		}
	}

	/**
	 * Exports the IDD to a DOT file using numeric variable labels.
	 */
	public static void export(IDD f, String filePath) throws IOException {
		Counter counter = new Counter();
		Map<IDD, String> labels = new IdentityHashMap<>();

		try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
			pw.println("digraph IDD {");
			pw.println("  rankdir=TB;");
			pw.println("  node [shape=ellipse];");

			visit(f, labels, pw, counter);

			pw.println("}");
		}
	}

	private static void visit(IDD f, Map<IDD, String> labels, PrintWriter pw, Counter counter) {
		if (labels.containsKey(f)) {
			return;
		}

		String label = "n" + counter.next();
		labels.put(f, label);

		if (f.isTerminal()) {
			pw.printf("  %s [label=\"%s\" shape=doublecircle];\n", label, f.isTrue() ? "TRUE" : "FALSE");
		} else {
			pw.printf("  %s [label=\"%s\\nvar=%d\"];\n", label, label, f.variable());
		}

		for (Edge e : f.edges()) {
			visit(e.child(), labels, pw, counter);
			String toLabel = labels.get(e.child());
			pw.printf("  %s -> %s [label=\"%s\"];\n", label, toLabel, e.interval());
		}
	}

	private static final class Counter {

		private int value;

		int next() {
			return value++;
		}
	}
}
