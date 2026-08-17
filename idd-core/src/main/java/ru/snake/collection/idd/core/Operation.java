package ru.snake.collection.idd.core;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Supported Boolean operations.
 * <p>
 * Replaces the previous use of {@code BiFunction<Boolean,Boolean,Boolean>}
 * lambdas, which required {@code System.identityHashCode} for caching.
 * An {@code enum} provides stable, collision-free identity.
 */
public enum Operation {
	/** Logical AND */
	AND,

	/** Logical OR */
	OR,

	/** Logical XOR */
	XOR,

	/** Logical IMPLIES (f → g ≡ ¬f ∨ g) */
	IMPLIES;

	/**
	 * Evaluates this connective on two Boolean operands.
	 */
	public boolean apply(boolean a, boolean b) {
		return switch (this) {
			case AND -> a && b;
			case OR -> a || b;
			case XOR -> a ^ b;
			case IMPLIES -> !a || b;
		};
	}
}
