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
import org.openjdk.jmh.infra.Blackhole;

import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.operation.Evaluate;
import ru.snake.collection.idd.util.VariableRange;

/**
 * JMH benchmark for IDD firewall evaluation.
 *
 * <p>
 * Builds a firewall policy from N rules (parameterised), then evaluates a fixed
 * set of deterministic packets against the compiled IDD.
 *
 * <p>
 * Run with:
 *
 * <pre>
 *   java -jar idd-benchmark-1.0.0.jar
 * </pre>
 *
 * Or via Maven:
 *
 * <pre>
 *   mvn -pl idd-benchmark package
 *   java -jar idd-benchmark/target/idd-benchmark-1.0.0.jar
 * </pre>
 */
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgsAppend = { "-Xms512m", "-Xmx512m" })
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class FirewallEvaluationBenchmark {

	// ------------------------------------------------------------------
	// Parameters
	// ------------------------------------------------------------------

	/**
	 * Number of rules to build the firewall from (first N rules of the standard
	 * set).
	 */
	@Param({ "10", "30", "60", "120", "200" })
	public int ruleCount;

	// ------------------------------------------------------------------
	// State -- shared across benchmark methods
	// ------------------------------------------------------------------

	private VariableOrder order;
	private IDDFactory factory;
	private IDD firewall;
	/** Deterministic packets: [srcIp, dstIp, srcPort, dstPort, protocol] */
	private int[][] packets;

	// ==================================================================
	// Setup
	// ==================================================================

	@Setup(Level.Trial)
	public void setUp() {
		order = new VariableOrder(
			Map.ofEntries(
				Map.entry(FirewallBenchmarkUtils.VAR_SRC_PORT, VariableRange.of(0, 65535)),
				Map.entry(FirewallBenchmarkUtils.VAR_DST_PORT, VariableRange.of(0, 65535)),
				Map.entry(FirewallBenchmarkUtils.VAR_PROTOCOL, VariableRange.of(0, 255))
			),
			FirewallBenchmarkUtils.VAR_SRC_IP,
			FirewallBenchmarkUtils.VAR_DST_IP,
			FirewallBenchmarkUtils.VAR_SRC_PORT,
			FirewallBenchmarkUtils.VAR_DST_PORT,
			FirewallBenchmarkUtils.VAR_PROTOCOL
		);
		factory = new IDDFactory(order);

		FirewallPolicyBuilder builder = FirewallPolicyBuilder.of(factory);
		firewall = builder.buildFirewall(ruleCount);
		packets = FirewallBenchmarkUtils.generateDeterministicPackets();
	}

	// ==================================================================
	// Benchmarks
	// ==================================================================

	/**
	 * Evaluates the entire deterministic packet set against the firewall IDD.
	 *
	 * <p>
	 * Measures wall-clock throughput for N sequential evaluations. Each
	 * iteration evaluates all 1000 packets using a reusable int[] buffer to
	 * avoid per-packet Map allocation. Results are consumed via Blackhole to
	 * prevent dead-code elimination.
	 */
	@Benchmark
	@BenchmarkMode({ Mode.AverageTime, Mode.Throughput })
	public void evaluateAll(Blackhole bh) {
		int[] values = new int[5];

		for (int[] pkt : packets) {
			values[0] = pkt[0];
			values[1] = pkt[1];
			values[2] = pkt[2];
			values[3] = pkt[3];
			values[4] = pkt[4];
			bh.consume(Evaluate.evaluate(firewall, values));
		}
	}
}
