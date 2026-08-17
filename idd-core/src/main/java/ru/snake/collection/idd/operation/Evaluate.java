package ru.snake.collection.idd.operation;

import java.util.Map;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.VariableOrder;

/**
 * Evaluates an IDD against a concrete variable assignment.
 */
public final class Evaluate {

	private Evaluate() {
	}

	/**
	 * Evaluates the IDD with the given assignment, specified as an array where
	 * {@code values[i]} is the value for the variable at index {@code i} in the
	 * variable order.
	 * <p>
	 * This overload avoids per-node string-keyed map lookups and is the
	 * zero-allocation path suitable for high-throughput evaluation.
	 *
	 * @param f      the IDD to evaluate
	 * @param values array indexed by variable order position
	 * @return the Boolean result of the evaluation
	 * @throws IllegalArgumentException if a variable index is out of bounds
	 */
	public static boolean evaluate(IDD f, int[] values) {
		if (f.isTerminal()) {
			return f.isTrue();
		}

		int value = values[f.variable()];

		for (Edge e : f.edges()) {
			if (value >= e.low() && value <= e.high()) {
				return evaluate(e.child(), values);
			}
		}

		throw new IllegalStateException("No edge covers value " + value + " for variable index " + f.variable());
	}

	/**
	 * Evaluates the IDD with the given assignment.
	 * <p>
	 * Converts the map to an {@code int[]} once at the entry point and
	 * delegates to the array overload. Every variable in the order must be
	 * present in the map.
	 *
	 * @param f          the IDD to evaluate
	 * @param order      the variable order
	 * @param assignment map from variable name to value; must contain all
	 *                       variables defined in {@code order}
	 * @return the Boolean result of the evaluation
	 * @throws IllegalArgumentException if a variable in the order is not
	 *                                      present in the assignment
	 */
	public static boolean evaluate(IDD f, VariableOrder order, Map<String, Integer> assignment) {
		int[] values = new int[order.size()];
		for (int i = 0; i < values.length; i++) {
			String name = order.name(i);
			Integer value = assignment.get(name);
			if (value == null) {
				throw new IllegalArgumentException("Missing assignment for variable: " + name);
			}
			values[i] = value;
		}
		return evaluate(f, values);
	}
}
