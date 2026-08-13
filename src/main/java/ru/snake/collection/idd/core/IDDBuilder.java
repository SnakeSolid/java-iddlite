package ru.snake.collection.idd.core;

import java.util.ArrayList;
import java.util.List;

import ru.snake.collection.idd.operation.Apply;

/**
 * Fluent builder for constructing IDDs from interval rules.
 * <p>
 * Example:
 * 
 * <pre>{@code
 * IDDBuilder b = factory.builder();
 * b.when("x").in(1, 5).then(true);
 * b.when("x").in(6, 10).then(false);
 * IDD idd = b.build();
 * }</pre>
 */
public final class IDDBuilder {

	private final IDDFactory factory;

	private int currentVarIndex = -1;

	private int currentLow = -1;

	private int currentHigh = -1;

	private final List<Rule> rules = new ArrayList<>();

	IDDBuilder(IDDFactory factory) {
		this.factory = factory;
	}

	/**
	 * Start a new rule clause for the given variable.
	 */
	public IntervalCondition when(String varName) {
		currentVarIndex = factory.order().index(varName);
		return new IntervalCondition();
	}

	/**
	 * Builds the IDD from the accumulated rules. Rules for the same variable
	 * are merged into a single node.
	 */
	public IDD build() {
		if (rules.isEmpty()) {
			return factory.trueNode();
		}

		// Collect distinct variable indices from the rules.
		List<Integer> distinctVars = new ArrayList<>();
		for (Rule r : rules) {
			if (!distinctVars.contains(r.varIndex)) {
				distinctVars.add(r.varIndex);
			}
		}
		distinctVars.sort(Integer::compareTo);

		if (distinctVars.isEmpty()) {
			return factory.trueNode();
		}

		if (distinctVars.size() == 1) {
			return buildSingleVar(distinctVars.get(0));
		}

		// For multiple variables, build independent single-variable IDDs and
		// AND them.
		IDD result = factory.trueNode();
		for (int varIdx : distinctVars) {
			IDD single = buildSingleVar(varIdx);
			result = Apply.and(factory, result, single);
		}
		return result;
	}

	private IDD buildSingleVar(int varIndex) {
		List<Edge> edges = new ArrayList<>();
		for (Rule r : rules) {
			if (r.varIndex == varIndex) {
				IDD child = r.isTrue ? factory.trueNode() : factory.falseNode();
				edges.add(new Edge(r.low, r.high, child));
			}
		}

		if (edges.isEmpty()) {
			return factory.trueNode();
		}

		return factory.getNode(varIndex, edges);
	}

	public class IntervalCondition {

		public ThenCondition in(int low, int high) {
			currentLow = low;
			currentHigh = high;
			return new ThenCondition();
		}
	}

	public class ThenCondition {

		public IDDBuilder then(boolean value) {
			rules.add(new Rule(currentVarIndex, currentLow, currentHigh, value));
			return IDDBuilder.this;
		}
	}

	private record Rule(int varIndex, int low, int high, boolean isTrue) {
	}

	static IDDBuilder create(IDDFactory factory) {
		return new IDDBuilder(factory);
	}
}
