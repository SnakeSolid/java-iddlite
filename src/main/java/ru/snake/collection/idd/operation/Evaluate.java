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
	 * Evaluates the IDD with the given assignment.
	 *
	 * @param f          the IDD to evaluate
	 * @param assignment map from variable name to value
	 * @return the Boolean result of the evaluation
	 * @throws IllegalArgumentException if a variable in the IDD is not present
	 *                                      in the assignment
	 */
	public static boolean evaluate(IDD f, VariableOrder order, Map<String, Integer> assignment) {
		if (f.isTerminal()) {
			return f.isTrue();
		}

		String varName = order.name(f.variable());
		Integer value = assignment.get(varName);

		if (value == null) {
			throw new IllegalArgumentException("Missing assignment for variable: " + varName);
		}

		for (Edge e : f.edges()) {
			if (value >= e.low() && value <= e.high()) {
				return evaluate(e.child(), order, assignment);
			}
		}

		throw new IllegalStateException("No edge covers value " + value + " for variable " + varName);
	}
}
