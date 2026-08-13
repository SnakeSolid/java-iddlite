package ru.snake.collection.idd.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.BiFunction;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;

/**
 * Applies a binary Boolean operation to two IDD operands.
 * <p>
 * Uses memoisation (computed cache with {@link WeakHashMap}) to avoid redundant
 * work.
 */
public final class Apply {

	private final IDDFactory factory;

	private final Map<ApplyKey, IDD> cache;

	public Apply(IDDFactory factory) {
		this.factory = factory;
		this.cache = new WeakHashMap<>();
	}

	/**
	 * Applies a binary connective to two IDDs.
	 */
	public IDD apply(IDD f, IDD g, BiFunction<Boolean, Boolean, Boolean> op) {
		ApplyKey key = new ApplyKey(f, g, op);
		return cache.computeIfAbsent(key, k -> applyBinary(f, g, op));
	}

	/**
	 * Applies a unary NOT operation.
	 */
	public IDD applyNot(IDD f) {
		return notRecursive(f);
	}

	// ---- Binary apply ----

	private IDD applyBinary(IDD f, IDD g, BiFunction<Boolean, Boolean, Boolean> op) {
		// Base: both terminals.
		if (f.isTerminal() && g.isTerminal()) {
			boolean result = op.apply(f.isTrue(), g.isTrue());
			return result ? IDD.TRUE : IDD.FALSE;
		}

		if (f.isTerminal()) {
			return applyTerminalLeft(f, g, op);
		}
		if (g.isTerminal()) {
			return applyTerminalRight(f, g, op);
		}

		int fVar = f.variable();
		int gVar = g.variable();

		if (fVar == gVar) {
			return applySameVar(f, g, fVar, op);
		} else if (fVar < gVar) {
			return applyHigherVar(f, g, op);
		} else {
			return applyLowerVar(f, g, op);
		}
	}

	private IDD applySameVar(IDD f, IDD g, int var, BiFunction<Boolean, Boolean, Boolean> op) {
		List<Edge> newEdges = new ArrayList<>();
		int i = 0, j = 0;
		List<Edge> fEdges = f.edges(), gEdges = g.edges();

		while (i < fEdges.size() && j < gEdges.size()) {
			Edge ef = fEdges.get(i), eg = gEdges.get(j);
			int lo = Math.max(ef.low(), eg.low());
			int hi = Math.min(ef.high(), eg.high());

			if (lo <= hi) {
				IDD child = applyBinary(ef.child(), eg.child(), op);
				newEdges.add(new Edge(lo, hi, child));
			}

			if (ef.high() < eg.high()) {
				i++;
			} else if (eg.high() < ef.high()) {
				j++;
			} else {
				i++;
				j++;
			}
		}

		if (newEdges.isEmpty()) {
			return IDD.FALSE;
		}
		return factory.getNode(var, newEdges);
	}

	/**
	 * f's variable is earlier in the order (higher in the diagram). For each
	 * edge of f, recurse with the child and g.
	 */
	private IDD applyHigherVar(IDD f, IDD g, BiFunction<Boolean, Boolean, Boolean> op) {
		List<Edge> newEdges = new ArrayList<>();
		for (Edge ef : f.edges()) {
			IDD child = applyBinary(ef.child(), g, op);
			newEdges.add(new Edge(ef.low(), ef.high(), child));
		}
		return factory.getNode(f.variable(), newEdges);
	}

	/**
	 * g's variable is earlier in the order (higher in the diagram). For each
	 * edge of g, recurse with f and the child.
	 */
	private IDD applyLowerVar(IDD f, IDD g, BiFunction<Boolean, Boolean, Boolean> op) {
		List<Edge> newEdges = new ArrayList<>();
		for (Edge eg : g.edges()) {
			IDD child = applyBinary(f, eg.child(), op);
			newEdges.add(new Edge(eg.low(), eg.high(), child));
		}
		return factory.getNode(g.variable(), newEdges);
	}

	private IDD applyTerminalLeft(IDD fTerm, IDD g, BiFunction<Boolean, Boolean, Boolean> op) {
		List<Edge> newEdges = new ArrayList<>();
		for (Edge eg : g.edges()) {
			IDD child = applyBinary(fTerm, eg.child(), op);
			newEdges.add(new Edge(eg.low(), eg.high(), child));
		}
		return factory.getNode(g.variable(), newEdges);
	}

	private IDD applyTerminalRight(IDD f, IDD gTerm, BiFunction<Boolean, Boolean, Boolean> op) {
		List<Edge> newEdges = new ArrayList<>();
		for (Edge ef : f.edges()) {
			IDD child = applyBinary(ef.child(), gTerm, op);
			newEdges.add(new Edge(ef.low(), ef.high(), child));
		}
		return factory.getNode(f.variable(), newEdges);
	}

	// ---- Unary NOT ----

	private IDD notRecursive(IDD f) {
		if (f.isTerminal()) {
			return f.isTrue() ? IDD.FALSE : IDD.TRUE;
		}
		List<Edge> newEdges = new ArrayList<>();
		for (Edge ef : f.edges()) {
			IDD child = notRecursive(ef.child());
			newEdges.add(new Edge(ef.low(), ef.high(), child));
		}
		return factory.getNode(f.variable(), newEdges);
	}

	// ---- Convenience static methods ----

	public static IDD and(IDDFactory factory, IDD f, IDD g) {
		return new Apply(factory).apply(f, g, (a, b) -> a && b);
	}

	public static IDD or(IDDFactory factory, IDD f, IDD g) {
		return new Apply(factory).apply(f, g, (a, b) -> a || b);
	}

	public static IDD xor(IDDFactory factory, IDD f, IDD g) {
		return new Apply(factory).apply(f, g, (a, b) -> a ^ b);
	}

	public static IDD implies(IDDFactory factory, IDD f, IDD g) {
		return new Apply(factory).apply(f, g, (a, b) -> !a || b);
	}

	public static IDD not(IDDFactory factory, IDD f) {
		return new Apply(factory).applyNot(f);
	}

	// ---- Cache key ----

	private static final class ApplyKey {

		private final IDD f, g;
		private final Object opId;

		ApplyKey(IDD f, IDD g, BiFunction<Boolean, Boolean, Boolean> op) {
			this.f = f;
			this.g = g;
			this.opId = System.identityHashCode(op);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ApplyKey other = (ApplyKey) o;
			return f == other.f && g == other.g && opId == other.opId;
		}

		@Override
		public int hashCode() {
			return Objects.hash(System.identityHashCode(f), System.identityHashCode(g), opId);
		}
	}
}
