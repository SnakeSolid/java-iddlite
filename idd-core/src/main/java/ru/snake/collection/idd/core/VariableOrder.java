package ru.snake.collection.idd.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.snake.collection.idd.util.VariableRange;

/**
 * Maintains a fixed global variable order for the IDD.
 * <p>
 * Once constructed, the order is immutable.
 */
public final class VariableOrder {

	private final List<String> names;

	private final Map<String, Integer> indexBy;

	private final Map<String, VariableRange> ranges;

	public VariableOrder(String... varNames) {
		this(Map.of(), varNames);
	}

	/**
	 * Constructs a variable order with the given variable names and optional
	 * ranges. Variables not present in the ranges map default to the full
	 * integer range.
	 *
	 * @param ranges mapping from variable name to its valid range; may be empty
	 * @param varNames the variable names in order
	 * @throws IllegalArgumentException if a name is duplicated
	 */
	public VariableOrder(
		Map<String, VariableRange> ranges,
		String... varNames
	) {
		this.names = new ArrayList<>();
		this.indexBy = new HashMap<>();
		this.ranges = new HashMap<>(Map.copyOf(ranges));

		for (String name : varNames) {
			if (indexBy.containsKey(name)) {
				throw new IllegalArgumentException(
					"Duplicate variable: " + name
				);
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
			throw new IllegalArgumentException(
				"Variable index out of range: " + index
			);
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
	 * Returns the valid range for the variable with the given name.
	 * Variables without an explicit range default to the full integer range.
	 *
	 * @param name the variable name
	 * @return the valid range for this variable
	 * @throws IllegalArgumentException if the variable name is unknown
	 */
	public VariableRange range(String name) {
		Integer idx = indexBy.get(name);

		if (idx == null) {
			throw new IllegalArgumentException("Unknown variable: " + name);
		}

		VariableRange r = ranges.get(name);

		if (r == null) {
			return VariableRange.fullRange();
		}

		return r;
	}

	/**
	 * Returns the valid range for the variable at the given index.
	 * Variables without an explicit range default to the full integer range.
	 *
	 * @param index the variable index
	 * @return the valid range for this variable
	 * @throws IllegalArgumentException if the index is out of range
	 */
	public VariableRange range(int index) {
		String name = name(index);
		return range(name);
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
}
