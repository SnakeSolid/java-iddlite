package ru.snake.collection.idd.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.snake.collection.idd.core.util.VariableRange;

/**
 * Maps each variable to its valid value range.
 * <p>
 * Variables not explicitly mapped default to the full integer range
 * {@code [MIN_VALUE, MAX_VALUE]}.
 * <p>
 * Immutable and thread-safe.
 */
public final class VariableRanges {

	private final Map<String, VariableRange> rangesByName;

	private final List<VariableRange> rangeByIndex;

	/**
	 * Constructs ranges from a name-to-range map and a variable order.
	 * <p>
	 * The variable order is used to build an index-based lookup list. Variables
	 * not present in the ranges map default to the full integer range.
	 *
	 * @param ranges   mapping from variable name to its valid range; may be
	 *                     empty
	 * @param varOrder the variable order (to determine the index list)
	 */
	public VariableRanges(
		Map<String, VariableRange> ranges,
		VariableOrder varOrder
	) {
		this.rangesByName = new HashMap<>(ranges);
		this.rangeByIndex = new ArrayList<>();

		for (int i = 0; i < varOrder.size(); i++) {
			String name = varOrder.name(i);
			VariableRange r = ranges.get(name);
			rangeByIndex.add(r != null ? r : VariableRange.fullRange());
		}
	}

	/**
	 * Returns the valid range for the variable with the given name. Variables
	 * without an explicit range default to the full integer range.
	 *
	 * @param name     the variable name
	 * @param varOrder the variable order (used to validate the name)
	 * @return the valid range for this variable
	 * @throws IllegalArgumentException if the variable name is unknown
	 */
	public VariableRange range(String name, VariableOrder varOrder) {
		if (!varOrder.contains(name)) {
			throw new IllegalArgumentException("Unknown variable: " + name);
		}

		VariableRange r = rangesByName.get(name);

		if (r == null) {
			return VariableRange.fullRange();
		}

		return r;
	}

	/**
	 * Returns the valid range for the variable at the given index. Variables
	 * without an explicit range default to the full integer range.
	 * <p>
	 * This is an O(1) direct list lookup.
	 *
	 * @param index the variable index
	 * @return the valid range for this variable
	 * @throws IllegalArgumentException if the index is out of range
	 */
	public VariableRange range(int index) {
		if (index < 0 || index >= rangeByIndex.size()) {
			throw new IllegalArgumentException(
				"Variable index out of range: " + index
			);
		}

		return rangeByIndex.get(index);
	}

	@Override
	public String toString() {
		return "VariableRanges[" + rangesByName + "]";
	}
}
