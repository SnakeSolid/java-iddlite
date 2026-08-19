package ru.snake.collection.idd.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains a fixed global variable order for the IDD.
 * <p>
 * Once constructed, the order is immutable.
 */
public final class VariableOrder {

	private final List<String> names;

	private final Map<String, Integer> indexBy;

	public VariableOrder(String... varNames) {
		this.names = new ArrayList<>();
		this.indexBy = new HashMap<>();

		for (String name : varNames) {
			if (indexBy.containsKey(name)) {
				throw new IllegalArgumentException("Duplicate variable: " + name);
			}

			indexBy.put(name, names.size());
			names.add(name);
		}
	}

	/**
	 * Returns the number of variables in the order.
	 */
	public int size() {
		return names.size();
	}

	/**
	 * Returns the name of the variable at the given index.
	 */
	public String name(int index) {
		if (index < 0 || index >= names.size()) {
			throw new IllegalArgumentException("Variable index out of range: " + index);
		}

		return names.get(index);
	}

	/**
	 * Returns the index of the variable with the given name.
	 */
	public int index(String name) {
		Integer idx = indexBy.get(name);

		if (idx == null) {
			throw new IllegalArgumentException("Unknown variable: " + name);
		}

		return idx;
	}

	/**
	 * Returns true if this order contains a variable with the given name.
	 */
	public boolean contains(String name) {
		return indexBy.containsKey(name);
	}

	/**
	 * Compares two variable indices according to the global order. A lower
	 * index means the variable appears earlier (higher) in the diagram.
	 *
	 * @return negative if v1 &lt; v2, zero if equal, positive if v1 &gt; v2
	 */
	public int compare(int v1, int v2) {
		return Integer.compare(v1, v2);
	}

	/**
	 * Returns true if v1 appears at or before v2 in the order.
	 */
	public boolean atOrBefore(int v1, int v2) {
		return v1 <= v2;
	}

	@Override
	public String toString() {
		return "VariableOrder[" + String.join(", ", names) + "]";
	}
}
