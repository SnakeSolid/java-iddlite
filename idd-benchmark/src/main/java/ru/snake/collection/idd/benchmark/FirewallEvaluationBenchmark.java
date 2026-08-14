package ru.snake.collection.idd.benchmark;

import java.util.List;
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

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.operation.Apply;
import ru.snake.collection.idd.operation.Evaluate;
import ru.snake.collection.idd.util.VariableRange;

/**
 * JMH benchmark for IDD firewall evaluation.
 *
 * <p>
 * Builds a firewall policy from N rules (parameterised), then evaluates a fixed
 * set of deterministic packets against the compiled IDD. This mirrors the logic
 * from {@code FirewallRuleTest} but runs under proper JMH controls.
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
	// Variable names
	// ------------------------------------------------------------------

	private static final String VAR_SRC_IP = "src_ip";

	private static final String VAR_DST_IP = "dst_ip";

	private static final String VAR_SRC_PORT = "src_port";

	private static final String VAR_DST_PORT = "dst_port";

	private static final String VAR_PROTOCOL = "protocol";

	// ------------------------------------------------------------------
	// Protocol constants (RFC 1700)
	// ------------------------------------------------------------------

	private static final int PROTO_ICMP = 1;

	private static final int PROTO_TCP = 6;

	private static final int PROTO_UDP = 17;

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
				Map.entry(VAR_SRC_PORT, VariableRange.of(0, 65535)),
				Map.entry(VAR_DST_PORT, VariableRange.of(0, 65535)),
				Map.entry(VAR_PROTOCOL, VariableRange.of(0, 255))
			),
			VAR_SRC_IP,
			VAR_DST_IP,
			VAR_SRC_PORT,
			VAR_DST_PORT,
			VAR_PROTOCOL
		);
		factory = new IDDFactory(order);

		firewall = buildFirewall(ruleCount);
		packets = generateDeterministicPackets();
	}

	// ==================================================================
	// Benchmarks
	// ==================================================================

	/**
	 * Evaluates the entire deterministic packet set against the firewall IDD.
	 *
	 * <p>
	 * Measures wall-clock throughput for N sequential evaluations. Each
	 * iteration evaluates all 1000 packets.
	 */
	@Benchmark
	@BenchmarkMode({ Mode.AverageTime, Mode.Throughput })
	public boolean evaluateAll() {
		boolean acceptedCount = false;

		for (int[] pkt : packets) {
			boolean result = Evaluate.evaluate(
				firewall,
				order,
				Map.of(
					VAR_SRC_IP,
					pkt[0],
					VAR_DST_IP,
					pkt[1],
					VAR_SRC_PORT,
					pkt[2],
					VAR_DST_PORT,
					pkt[3],
					VAR_PROTOCOL,
					pkt[4]
				)
			);

			if (result) {
				acceptedCount = true;
			}
		}

		return acceptedCount;
	}

	// ==================================================================
	// Firewall rule building (mirrors FirewallRuleTest)
	// ==================================================================

	/**
	 * A single firewall rule specification.
	 */
	private record RuleSpec(String srcIp, String dstIp, String srcPort, String dstPort, Integer protocol) {
	}

	private static final List<RuleSpec> ALL_RULES = List.of(
		// 1: Loopback
		new RuleSpec("127.0.0.0/8", "127.0.0.0/8", "*", "*", null),
		// 2: Established TCP from internal
		new RuleSpec("10.0.0.0/8", "*", "49152-65535", "1-1024", PROTO_TCP),
		// 3: HTTP
		new RuleSpec("10.0.0.0/8", "*", "*", "80", PROTO_TCP),
		// 4: HTTPS
		new RuleSpec("10.0.0.0/8", "*", "*", "443", PROTO_TCP),
		// 5: DNS UDP
		new RuleSpec("10.0.0.0/8", "*", "*", "53", PROTO_UDP),
		// 6: DNS TCP
		new RuleSpec("10.0.0.0/8", "*", "*", "53", PROTO_TCP),
		// 7: ICMP internal
		new RuleSpec("10.0.0.0/8", "10.0.0.0/8", "*", "*", PROTO_ICMP),
		// 8: ICMP to DMZ
		new RuleSpec("10.0.0.0/8", "172.16.0.0/12", "*", "*", PROTO_ICMP),
		// 9: SMTP
		new RuleSpec("10.0.0.0/8", "172.16.0.0/12", "*", "25", PROTO_TCP),
		// 10: SSH admin->DMZ
		new RuleSpec("10.10.0.0/16", "172.16.0.0/12", "*", "22", PROTO_TCP),
		// 11: FTP control
		new RuleSpec("10.0.0.0/8", "172.16.0.0/12", "*", "21", PROTO_TCP),
		// 12: FTP data
		new RuleSpec("10.0.0.0/8", "172.16.0.0/12", "*", "20", PROTO_TCP),
		// 13: NTP
		new RuleSpec("10.0.0.0/8", "*", "*", "123", PROTO_UDP),
		// 14: SNMP
		new RuleSpec("10.10.0.0/16", "*", "*", "161", PROTO_UDP),
		// 15: SIP
		new RuleSpec("10.20.0.0/16", "10.20.0.0/16", "*", "5060", PROTO_UDP),
		// 16: RTP media
		new RuleSpec("10.20.0.0/16", "10.20.0.0/16", "*", "10000-20000", PROTO_UDP),
		// 17: IMAP
		new RuleSpec("10.0.0.0/8", "172.16.0.0/12", "*", "143", PROTO_TCP),
		// 18: IMAPS
		new RuleSpec("10.0.0.0/8", "172.16.0.0/12", "*", "993", PROTO_TCP),
		// 19: PostgreSQL
		new RuleSpec("10.30.0.0/16", "10.40.0.0/16", "*", "5432", PROTO_TCP),
		// 20: MySQL
		new RuleSpec("10.30.0.0/16", "10.40.0.0/16", "*", "3306", PROTO_TCP),
		// 21: Redis
		new RuleSpec("10.30.0.0/16", "10.50.0.0/16", "*", "6379", PROTO_TCP),
		// 22: MongoDB
		new RuleSpec("10.30.0.0/16", "10.40.0.0/16", "*", "27017", PROTO_TCP),
		// 23: LDAP
		new RuleSpec("10.0.0.0/8", "10.10.0.0/16", "*", "389", PROTO_TCP),
		// 24: LDAPS
		new RuleSpec("10.0.0.0/8", "10.10.0.0/16", "*", "636", PROTO_TCP),
		// 25: Kerberos
		new RuleSpec("10.0.0.0/8", "10.10.0.0/16", "*", "88", PROTO_UDP),
		// 26: RDP
		new RuleSpec("10.10.0.0/16", "10.10.0.0/16", "*", "3389", PROTO_TCP),
		// 27: Docker API
		new RuleSpec("10.60.0.0/16", "10.70.0.0/16", "*", "2376", PROTO_TCP),
		// 28: Prometheus
		new RuleSpec("10.80.0.0/16", "*", "*", "9090", PROTO_TCP),
		// 29: Elasticsearch
		new RuleSpec("10.30.0.0/16", "10.90.0.0/16", "*", "9200", PROTO_TCP),
		// 30: Kafka
		new RuleSpec("10.30.0.0/16", "10.70.0.0/16", "*", "9092", PROTO_TCP),
		// 31: gRPC
		new RuleSpec("10.30.0.0/16", "10.30.0.0/16", "*", "50051", PROTO_TCP),
		// 32: RabbitMQ AMQP
		new RuleSpec("10.30.0.0/16", "10.70.0.0/16", "*", "5672", PROTO_TCP),
		// 33: RabbitMQ management
		new RuleSpec("10.10.0.0/16", "10.70.0.0/16", "*", "15672", PROTO_TCP),
		// 34: Memcached
		new RuleSpec("10.30.0.0/16", "10.50.0.0/16", "*", "11211", PROTO_TCP),
		// 35: ZooKeeper
		new RuleSpec("10.30.0.0/16", "10.70.0.0/16", "*", "2181", PROTO_TCP),
		// 36: Consistent hashing RPC
		new RuleSpec("10.30.0.0/16", "10.0.0.0/8", "*", "8080", PROTO_TCP),
		// 37: syslog
		new RuleSpec("10.0.0.0/8", "10.80.0.0/16", "*", "514", PROTO_UDP),
		// 38: HTTPS management
		new RuleSpec("10.10.0.0/16", "10.30.0.0/16", "*", "8443", PROTO_TCP),
		// 39: SNMP Trap
		new RuleSpec("10.0.0.0/8", "10.10.0.0/16", "*", "162", PROTO_UDP),
		// 40: TFTP
		new RuleSpec("10.10.0.0/16", "10.0.0.0/8", "*", "69", PROTO_UDP),
		// 41: GraphQL
		new RuleSpec("10.30.0.0/16", "10.30.0.0/16", "*", "4000", PROTO_TCP),
		// 42: NATS
		new RuleSpec("10.30.0.0/16", "10.70.0.0/16", "*", "4222", PROTO_TCP),
		// 43: Istio sidecar
		new RuleSpec("10.30.0.0/16", "10.30.0.0/16", "*", "15001", PROTO_TCP),
		// 44: Istio pilot
		new RuleSpec("10.60.0.0/16", "10.30.0.0/16", "*", "15010", PROTO_TCP),
		// 45: etcd
		new RuleSpec("10.60.0.0/16", "10.70.0.0/16", "*", "2379", PROTO_TCP),
		// 46: Kubernetes API
		new RuleSpec("10.60.0.0/16", "10.70.0.0/16", "*", "6443", PROTO_TCP),
		// 47: Container runtime
		new RuleSpec("10.60.0.0/16", "10.70.0.0/16", "*", "10250", PROTO_TCP),
		// 48: Node exporter
		new RuleSpec("10.80.0.0/16", "10.0.0.0/8", "*", "9100", PROTO_TCP),
		// 49: cAdvisor
		new RuleSpec("10.80.0.0/16", "10.70.0.0/16", "*", "8080", PROTO_TCP),
		// 50: Grafana
		new RuleSpec("10.10.0.0/16", "10.80.0.0/16", "*", "3000", PROTO_TCP),
		// 51: Alertmanager
		new RuleSpec("10.80.0.0/16", "10.80.0.0/16", "*", "9093", PROTO_TCP),
		// 52: ClickHouse
		new RuleSpec("10.30.0.0/16", "10.90.0.0/16", "*", "9000", PROTO_TCP),
		// 53: Fluentd
		new RuleSpec("10.30.0.0/16", "10.80.0.0/16", "*", "24224", PROTO_TCP),
		// 54: Vault
		new RuleSpec("10.30.0.0/16", "10.10.0.0/16", "*", "8200", PROTO_TCP),
		// 55: Consul
		new RuleSpec("10.30.0.0/16", "10.70.0.0/16", "*", "8500", PROTO_TCP),
		// 56: DNS over HTTPS (duplicate of HTTPS 443, intentionally redundant)
		new RuleSpec("10.0.0.0/8", "*", "*", "443", PROTO_TCP),
		// 57: SMTP submission
		new RuleSpec("10.0.0.0/8", "172.16.0.0/12", "*", "587", PROTO_TCP),
		// 58: HTTPS alternate
		new RuleSpec("10.0.0.0/8", "172.16.0.0/12", "*", "8443", PROTO_TCP),
		// 59: HTTPS management
		new RuleSpec("10.10.0.0/16", "10.30.0.0/16", "*", "9443", PROTO_TCP),
		// 60: ICMPv6 echo (duplicate, intentionally)
		new RuleSpec("10.0.0.0/8", "10.0.0.0/8", "*", "*", PROTO_ICMP)
	);

	private IDD buildFirewall(int ruleCount) {
		IDD firewall = IDD.FALSE;
		int count = Math.min(ruleCount, ALL_RULES.size());

		for (int i = 0; i < count; i++) {
			RuleSpec spec = ALL_RULES.get(i);
			firewall = Apply.or(factory, firewall, buildRule(spec));
		}

		return firewall;
	}

	private IDD buildRule(RuleSpec spec) {
		int[] s = resolveIp(spec.srcIp());
		int[] d = resolveIp(spec.dstIp());
		int[] sp = resolvePort(spec.srcPort());
		int[] dp = resolvePort(spec.dstPort());
		int[] pr = spec.protocol() != null ? new int[] { spec.protocol(), spec.protocol() } : new int[] { 0, 255 };

		IDD result = factory.buildFromIntervals(VAR_SRC_IP, List.of(new Edge(s[0], s[1], factory.trueNode())));
		result = Apply.and(
			factory,
			result,
			factory.buildFromIntervals(VAR_DST_IP, List.of(new Edge(d[0], d[1], factory.trueNode())))
		);
		result = Apply.and(
			factory,
			result,
			factory.buildFromIntervals(VAR_SRC_PORT, List.of(new Edge(sp[0], sp[1], factory.trueNode())))
		);
		result = Apply.and(
			factory,
			result,
			factory.buildFromIntervals(VAR_DST_PORT, List.of(new Edge(dp[0], dp[1], factory.trueNode())))
		);
		result = Apply.and(
			factory,
			result,
			factory.buildFromIntervals(VAR_PROTOCOL, List.of(new Edge(pr[0], pr[1], factory.trueNode())))
		);

		return result;
	}

	// ==================================================================
	// Deterministic packet generation
	// ==================================================================

	/**
	 * Generates a fixed set of 1000 deterministic packets using a seeded LCG
	 * (linear congruential generator). Same seeds always produce the same
	 * packets, making benchmark results fully reproducible.
	 */
	private static int[][] generateDeterministicPackets() {
		DetRng rng = new DetRng(12345);
		int[][] packets = new int[1000][5];
		int[] allowedPorts = { 80, 443, 53, 123, 161 };

		for (int i = 0; i < packets.length; i++) {
			int srcIp, dstIp, srcPort, dstPort, proto;
			int roll = rng.nextInt(100);

			if (roll < 30) {
				// Packets from rule-covered ranges
				srcIp = ip("10.0.0.0") | (rng.nextInt() & 0x00FFFFFF);
				dstIp = rng.nextInt() & 0xFFFFFFFF;
				srcPort = rng.nextInt(65536);
				dstPort = allowedPorts[rng.nextInt(allowedPorts.length)];
				proto = PROTO_TCP;
			} else if (roll < 60) {
				// Packets from blocked ranges
				srcIp = ip("203.0.113.0") | (rng.nextInt() & 0x000000FF);
				dstIp = ip("10.0.1.0") | (rng.nextInt() & 0x000000FF);
				srcPort = rng.nextInt(65536);
				dstPort = rng.nextInt(65536);
				proto = PROTO_TCP;
			} else {
				// Fully random across the int domain
				srcIp = rng.nextInt();
				dstIp = rng.nextInt();
				srcPort = rng.nextInt(65536);
				dstPort = rng.nextInt(65536);
				proto = rng.nextInt(256);
			}

			packets[i] = new int[] { srcIp, dstIp, srcPort, dstPort, proto };
		}

		return packets;
	}

	/** Minimal deterministic RNG (LCG) -- no external dependencies. */
	private static class DetRng {

		private long seed;

		DetRng(long seed) {
			this.seed = seed;
		}

		int nextInt() {
			seed = seed * 6364136223846793005L + 1442695040888963407L;
			return (int) (seed ^ (seed >>> 33));
		}

		int nextInt(int bound) {
			return Math.abs(nextInt()) % bound;
		}
	}

	// ==================================================================
	// Helpers (ported from FirewallRuleTest)
	// ==================================================================

	private static int ip(String addr) {
		String[] parts = addr.split("\\.");
		return (((Integer.parseInt(parts[0]) & 0xFF) << 24) | ((Integer.parseInt(parts[1]) & 0xFF) << 16)
				| ((Integer.parseInt(parts[2]) & 0xFF) << 8) | (Integer.parseInt(parts[3]) & 0xFF));
	}

	private static int[] resolveIp(String cidr) {
		if (cidr == null || cidr.equals("*")) {
			return new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE };
		}

		return cidrRange(cidr);
	}

	private static int[] cidrRange(String cidr) {
		String[] parts = cidr.split("/");
		int host = ip(parts[0]);
		int prefix = Integer.parseInt(parts[1]);
		int mask = prefix == 0 ? 0 : ~0 << (32 - prefix);
		int network = host & mask;
		int hostBits = prefix == 32 ? 0 : (1 << (32 - prefix)) - 1;
		return new int[] { network, network | hostBits };
	}

	private static int[] resolvePort(String spec) {
		if (spec == null || spec.equals("*")) {
			return new int[] { 0, 65535 };
		}

		if (spec.contains("-")) {
			String[] parts = spec.split("-");
			return new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), };
		}

		return new int[] { Integer.parseInt(spec), Integer.parseInt(spec) };
	}
}
