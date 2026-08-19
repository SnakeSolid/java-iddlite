package ru.snake.collection.idd.core.util;

/**
 * Formats a raw integer value for a given variable into a human-readable
 * string.
 * <p>
 * Used by {@link IDDPrinter} to display edge intervals in readable formats such
 * as IP addresses (<code>10.0.0.1</code>) or protocol names (<code>TCP</code>).
 * <p>
 * Implementations must be thread-safe (stateless recommended).
 */
@FunctionalInterface
public interface ValueFormatter {
	/**
	 * Formats the raw integer value for the given variable index.
	 *
	 * @param varIndex the variable index in the
	 *                     {@link ru.snake.collection.idd.core.VariableOrder}
	 * @param value    the raw integer value
	 * @return a human-readable string
	 */
	String format(int varIndex, int value);

	/**
	 * Default formatter that returns the value as-is.
	 */
	ValueFormatter RAW = (varIndex, value) -> Integer.toString(value);

	/**
	 * Returns a formatter that delegates to variable-specific formatters
	 * registered by variable index.
	 * <p>
	 * Falls back to {@link #RAW} for unregistered variables.
	 *
	 * @param formatters a map from variable index to a formatter for that
	 *                       variable's values
	 * @return a composite formatter
	 */
	static ValueFormatter composite(
		java.util.Map<
			Integer,
			java.util.function.IntFunction<String>
		> formatters
	) {
		return (varIndex, value) -> {
			java.util.function.IntFunction<String> f = formatters.get(varIndex);

			if (f != null) {
				return f.apply(value);
			}

			return Integer.toString(value);
		};
	}
}
