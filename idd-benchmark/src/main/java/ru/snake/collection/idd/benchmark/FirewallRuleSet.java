package ru.snake.collection.idd.benchmark;

import java.util.List;

/**
 * Standard firewall rule set used across benchmarks.
 *
 * <p>
 * Contains 200 rules covering realistic enterprise, cloud-native, and
 * microservice network policies. The first 60 rules are the original
 * hand-crafted set; rules 61–200 are generated variations that exercise
 * different CIDR combinations, port ranges, and protocols.
 *
 * <p>
 * All rules share the same five-variable schema: src_ip, dst_ip, src_port,
 * dst_port, protocol.
 */
public final class FirewallRuleSet {

	private FirewallRuleSet() {
	}

	/**
	 * A single firewall rule specification.
	 */
	public record RuleSpec(String srcIp, String dstIp, String srcPort, String dstPort, Integer protocol) {
	}

	private static final int PROTO_ICMP = FirewallBenchmarkUtils.PROTO_ICMP;
	private static final int PROTO_TCP = FirewallBenchmarkUtils.PROTO_TCP;
	private static final int PROTO_UDP = FirewallBenchmarkUtils.PROTO_UDP;

	private static final List<RuleSpec> HAND_CRAFTED = List.of(
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

	/**
	 * Generated rules (61–200) — deterministic variations across subnets, port
	 * ranges, and protocols.
	 */
	private static final List<RuleSpec> GENERATED = generateExtraRules();

	private static List<RuleSpec> generateExtraRules() {
		String[] srcNets = { "10.0.0.0/8", "10.10.0.0/16", "10.20.0.0/16", "10.30.0.0/16", "10.40.0.0/16",
				"10.50.0.0/16", "10.60.0.0/16", "10.70.0.0/16", "10.80.0.0/16", "10.90.0.0/16", "172.16.0.0/12",
				"172.16.0.0/16", "172.32.0.0/16" };

		String[] dstNets = { "10.0.0.0/8", "10.10.0.0/16", "10.20.0.0/16", "10.30.0.0/16", "10.40.0.0/16",
				"10.50.0.0/16", "10.60.0.0/16", "10.70.0.0/16", "10.80.0.0/16", "10.90.0.0/16", "172.16.0.0/12",
				"0.0.0.0/0" };

		String[] portSpecs = { "22", "25", "53", "80", "443", "993", "3306", "5432", "6379", "8080", "8443", "9090",
				"9200", "27017", "50051", "49152-65535", "1024-65535", "1-1024", "*" };

		Integer[] protocols = { PROTO_TCP, PROTO_UDP, PROTO_ICMP, null };

		java.util.ArrayList<RuleSpec> rules = new java.util.ArrayList<>();

		// Deterministic LCG to produce reproducible rule combinations
		FirewallBenchmarkUtils.DetRng rng = new FirewallBenchmarkUtils.DetRng(99999);

		int count = 200 - HAND_CRAFTED.size();
		for (int i = 0; i < count; i++) {
			String src = srcNets[rng.nextInt(srcNets.length)];
			String dst = dstNets[rng.nextInt(dstNets.length)];
			String srcPort = portSpecs[rng.nextInt(portSpecs.length)];
			String dstPort = portSpecs[rng.nextInt(portSpecs.length)];
			Integer proto = protocols[rng.nextInt(protocols.length)];

			rules.add(new RuleSpec(src, dst, srcPort, dstPort, proto));
		}

		return rules;
	}

	/**
	 * Returns the complete rule set (hand-crafted + generated), guaranteed to
	 * contain at least 200 rules.
	 */
	public static List<RuleSpec> getAllRules() {
		return List.copyOf(java.util.stream.Stream.concat(HAND_CRAFTED.stream(), GENERATED.stream()).toList());
	}

	/**
	 * Returns only the hand-crafted rules (60 rules).
	 */
	public static List<RuleSpec> getHandCraftedRules() {
		return HAND_CRAFTED;
	}

	/**
	 * Returns the total number of available rules.
	 */
	public static int getRuleCount() {
		return HAND_CRAFTED.size() + GENERATED.size();
	}
}
