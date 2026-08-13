package ru.snake.collection.idd.operation;

import java.util.ArrayList;
import java.util.List;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;

/**
 * Restricts an IDD by setting a variable to a specific value. Also known as
 * cofactoring: f|_{x=v}.
 */
public final class Restrict {

	private Restrict() {
	}

	/**
	 * Restricts the IDD by setting the named variable to the given value.
	 *
	 * @param factory the IDD factory
	 * @param f       the IDD to restrict
	 * @param varName the variable name
	 * @param value   the value to set
	 * @return the restricted IDD
	 */
	public static IDD restrict(IDDFactory factory, IDD f, String varName, int value) {
		int varIndex = factory.order().index(varName);
		return restrict(factory, f, varIndex, value);
	}

	/**
	 * Restricts the IDD by setting the variable at the given index to the given
	 * value.
	 */
	public static IDD restrict(IDDFactory factory, IDD f, int varIndex, int value) {
		if (f.isTerminal()) {
			return f;
		}

		int fVar = f.variable();

		if (fVar < varIndex) {
			// f's variable is earlier in order; recurse into all children.
			List<Edge> newEdges = new ArrayList<>();

			for (Edge e : f.edges()) {
				IDD child = restrict(factory, e.child(), varIndex, value);
				newEdges.add(new Edge(e.low(), e.high(), child));
			}

			return factory.getNode(fVar, newEdges);
		}

		if (fVar == varIndex) {
			// Find the edge whose interval contains the value.
			for (Edge e : f.edges()) {
				if (value >= e.low() && value <= e.high()) {
					return restrict(factory, e.child(), varIndex, value);
				}
			}

			// Value not covered (should not happen with a complete partition).
			return IDD.FALSE;
		}

		// f's variable is later in order; the target variable doesn't appear
		// here.
		return f;
	}
}
