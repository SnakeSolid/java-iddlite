package ru.snake.collection.idd.core.operation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;

/**
 * Quantification operations: existential and universal.
 */
public final class Quantify {

	private Quantify() {}

	/**
	 * Existential quantification: eliminates the variable by OR-ing all
	 * children.
	 *
	 * @param factory the IDD factory
	 * @param f       the IDD
	 * @param varName the variable name to eliminate
	 * @return the existentially quantified IDD
	 */
	public static IDD exists(IDDFactory factory, IDD f, String varName) {
		int varIndex = factory.order().index(varName);
		return exists(factory, f, varIndex);
	}

	/**
	 * Existential quantification using variable index.
	 */
	public static IDD exists(IDDFactory factory, IDD f, int varIndex) {
		if (f.isTerminal()) {
			return f;
		}

		int fVar = f.variable();

		if (fVar < varIndex) {
			// Recurse into all children.
			List<Edge> newEdges = new ArrayList<>();
			for (Edge e : f.edges()) {
				IDD child = exists(factory, e.child(), varIndex);
				newEdges.add(new Edge(e.low(), e.high(), child));
			}
			return factory.getNode(fVar, newEdges);
		}

		if (fVar == varIndex) {
			// OR all distinct children.
			Set<IDD> children = new LinkedHashSet<>();

			for (Edge e : f.edges()) {
				children.add(e.child());
			}

			IDD result = IDD.FALSE;

			for (IDD child : children) {
				result = factory.or(result, child);
			}

			return result;
		}

		// Variable not present in this subtree.
		return f;
	}

	/**
	 * Universal quantification: eliminates the variable by AND-ing all
	 * children.
	 *
	 * @param factory the IDD factory
	 * @param f       the IDD
	 * @param varName the variable name to eliminate
	 * @return the universally quantified IDD
	 */
	public static IDD forall(IDDFactory factory, IDD f, String varName) {
		int varIndex = factory.order().index(varName);
		return forall(factory, f, varIndex);
	}

	/**
	 * Universal quantification using variable index.
	 */
	public static IDD forall(IDDFactory factory, IDD f, int varIndex) {
		if (f.isTerminal()) {
			return f;
		}

		int fVar = f.variable();
		if (fVar < varIndex) {
			List<Edge> newEdges = new ArrayList<>();
			for (Edge e : f.edges()) {
				IDD child = forall(factory, e.child(), varIndex);
				newEdges.add(new Edge(e.low(), e.high(), child));
			}
			return factory.getNode(fVar, newEdges);
		}

		if (fVar == varIndex) {
			// AND all distinct children.
			Set<IDD> children = new LinkedHashSet<>();
			for (Edge e : f.edges()) {
				children.add(e.child());
			}
			IDD result = IDD.TRUE;
			for (IDD child : children) {
				result = factory.and(result, child);
			}
			return result;
		}

		return f;
	}
}
