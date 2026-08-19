package ru.snake.collection.idd.benchmark;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.core.VariableRanges;
import ru.snake.collection.idd.core.util.VariableRange;

/**
 * JMH benchmark for IDD firewall compilation.
 *
 * <p>
 * Measures the cost of building a firewall IDD from N rules via successive
 * {@code factory.or()} calls. This is where the algorithmic complexity of
 * decision diagram construction actually lives — far more expensive than
 * evaluation.
 *
 * <p>
 * Each invocation rebuilds the firewall from scratch using a freshly created
 * factory, so the unique table starts empty every time.
 */
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgsAppend = { "-Xms512m", "-Xmx512m" })
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class FirewallCompilationBenchmark {

	// ------------------------------------------------------------------
	// Parameters
	// ------------------------------------------------------------------

	/**
	 * Number of rules to compile into the firewall IDD.
	 */
	@Param({ "10", "30", "60", "120", "200" })
	public int ruleCount;

	// ------------------------------------------------------------------
	// State
	// ------------------------------------------------------------------

	private VariableOrder order;

	private VariableRanges ranges;

	// ==================================================================
	// Setup
	// ==================================================================

	@Setup(Level.Trial)
	public void setUp() {
		order = new VariableOrder(
			FirewallBenchmarkUtils.VAR_SRC_IP,
			FirewallBenchmarkUtils.VAR_DST_IP,
			FirewallBenchmarkUtils.VAR_SRC_PORT,
			FirewallBenchmarkUtils.VAR_DST_PORT,
			FirewallBenchmarkUtils.VAR_PROTOCOL
		);
		ranges = new VariableRanges(
			Map.ofEntries(
				Map.entry(FirewallBenchmarkUtils.VAR_SRC_PORT, VariableRange.of(0, 65535)),
				Map.entry(FirewallBenchmarkUtils.VAR_DST_PORT, VariableRange.of(0, 65535)),
				Map.entry(FirewallBenchmarkUtils.VAR_PROTOCOL, VariableRange.of(0, 255))
			),
			order
		);
	}

	// ==================================================================
	// Benchmarks
	// ==================================================================

	/**
	 * Compiles a firewall IDD from N rules.
	 *
	 * <p>
	 * Creates a fresh {@code IDDFactory} and {@code FirewallPolicyBuilder} on
	 * each invocation so the unique table is cold. Returns the resulting IDD
	 * node count to prevent dead-code elimination.
	 *
	 * @return the total number of nodes in the compiled IDD
	 */
	@Benchmark
	@BenchmarkMode({ Mode.AverageTime, Mode.Throughput })
	public int compileFirewall() {
		IDDFactory factory = new IDDFactory(order, ranges);
		FirewallPolicyBuilder builder = FirewallPolicyBuilder.of(factory);
		IDD firewall = builder.buildFirewall(ruleCount);
		return countNodes(firewall);
	}

	// ==================================================================
	// Helpers
	// ==================================================================

	/**
	 * Counts the total number of nodes in the IDD graph. This is a simple
	 * recursive walk — not optimized, used only to keep the result observable.
	 */
	private static int countNodes(IDD node) {
		java.util.IdentityHashMap<IDD, Boolean> visited = new java.util.IdentityHashMap<>();
		return countNodesDfs(node, visited);
	}

	private static int countNodesDfs(IDD node, java.util.IdentityHashMap<IDD, Boolean> visited) {
		if (visited.putIfAbsent(node, Boolean.TRUE) != null) {
			return 0;
		}
		return (1 + node.edges().stream().mapToInt(e -> countNodesDfs(e.child(), visited)).sum());
	}
}
