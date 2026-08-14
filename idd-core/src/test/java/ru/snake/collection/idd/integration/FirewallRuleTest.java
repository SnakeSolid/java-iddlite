package ru.snake.collection.idd.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;
import ru.snake.collection.idd.core.IDDFactory;
import ru.snake.collection.idd.core.VariableOrder;
import ru.snake.collection.idd.operation.Apply;
import ru.snake.collection.idd.operation.Evaluate;

/**
 * Demonstrates building a realistic firewall rule set (60 rules) with five
 * variables -- src_ip, dst_ip, src_port, dst_port, protocol -- compiling them
 * into an IDD, and testing the diagram against random packets.
 * <p>
 * Each rule specifies a source/destination IP range, source/destination port
 * range, and an IP protocol number. The complete policy is the OR of all
 * individual rules; any packet matching at least one rule is ACCEPTED.
 */
class FirewallRuleTest {

	// ------------------------------------------------------------------
	// Variable names used throughout the test
	// ------------------------------------------------------------------

	private static final String VAR_SRC_IP = "src_ip";

	private static final String VAR_DST_IP = "dst_ip";

	private static final String VAR_SRC_PORT = "src_port";

	private static final String VAR_DST_PORT = "dst_port";

	private static final String VAR_PROTOCOL = "protocol";

	// ------------------------------------------------------------------
	// Well-known IP protocol constants (RFC 1700)
	// ------------------------------------------------------------------

	private static final int PROTO_ICMP = 1;

	private static final int PROTO_TCP = 6;

	private static final int PROTO_UDP = 17;

	// ------------------------------------------------------------------
	// Helper: convert a dotted-decimal IPv4 address to an int (host byte
	// order: a &lt;&lt; 24 | b &lt;&lt; 16 | c &lt;&lt; 8 | d).
	// ------------------------------------------------------------------

	private static int ip(String addr) {
		String[] parts = addr.split("\\.");
		return (((Integer.parseInt(parts[0]) & 0xFF) << 24) | ((Integer.parseInt(parts[1]) & 0xFF) << 16)
				| ((Integer.parseInt(parts[2]) & 0xFF) << 8) | (Integer.parseInt(parts[3]) & 0xFF));
	}

	// ------------------------------------------------------------------
	// Helper: convert a CIDR prefix to its network address (int).
	// ------------------------------------------------------------------

	private static int cidrNetwork(String cidr) {
		int host = ip(cidr.split("/")[0]);
		int prefix = Integer.parseInt(cidr.split("/")[1]);
		int mask = prefix == 0 ? 0 : ~0 << (32 - prefix);
		return host & mask;
	}

	// ------------------------------------------------------------------
	// Helper: convert a CIDR prefix to its broadcast address (int).
	// ------------------------------------------------------------------

	private static int cidrBroadcast(String cidr) {
		int network = cidrNetwork(cidr);
		int prefix = Integer.parseInt(cidr.split("/")[1]);
		int hostBits = prefix == 32 ? 0 : (1 << (32 - prefix)) - 1;
		return network | hostBits;
	}

	// ------------------------------------------------------------------
	// Helper: parse a CIDR string into [network, broadcast] pair
	// ------------------------------------------------------------------

	private static int[] cidrRange(String cidr) {
		int net = cidrNetwork(cidr);
		int brd = cidrBroadcast(cidr);
		return new int[] { net, brd };
	}

	// ------------------------------------------------------------------
	// Helper: parse a port specification into [low, high] pair.
	// Formats: "*" -> full range, "80" -> single port, "8000-9000" -> range
	// ------------------------------------------------------------------

	private static int[] parsePort(String spec) {
		if (spec == null || spec.equals("*")) {
			return new int[] { 0, 65535 };
		}

		if (spec.contains("-")) {
			String[] parts = spec.split("-");
			return new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), };
		}

		int port = Integer.parseInt(spec);

		return new int[] { port, port };
	}

	// ------------------------------------------------------------------
	// Factory &amp; order -- shared across tests
	// ------------------------------------------------------------------

	private VariableOrder order;
	private IDDFactory factory;

	@BeforeEach
	void setUp() {
		order = new VariableOrder(VAR_SRC_IP, VAR_DST_IP, VAR_SRC_PORT, VAR_DST_PORT, VAR_PROTOCOL);
		factory = new IDDFactory(order);
	}

	// ==================================================================
	// Rule definition helper
	// ==================================================================

	// ==================================================================
	// DSL: readable rule definition
	// ==================================================================
	// Syntax: rule(srcIp, dstIp, srcPort, dstPort, protocol)
	// - IP: CIDR string (e.g. "10.0.0.0/8"), null = any
	// - Port: "*" = any, "80" = exact, "8000-9000" = range, null = any
	// - Protocol: integer (e.g. PROTO_TCP), null = any

	private IDD rule(String srcIp, String dstIp, String srcPort, String dstPort, Integer protocol) {
		int[] s = srcIp != null && !srcIp.equals("*") ? cidrRange(srcIp)
				: new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE };
		int[] d = dstIp != null && !dstIp.equals("*") ? cidrRange(dstIp)
				: new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE };
		int[] sp = srcPort != null ? parsePort(srcPort) : new int[] { 0, 65535 };
		int[] dp = dstPort != null ? parsePort(dstPort) : new int[] { 0, 65535 };
		int[] pr = protocol != null ? new int[] { protocol, protocol } : new int[] { 0, 255 };

		// Build each variable IDD independently, then AND them together.
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
	// Build a realistic firewall policy of 54 rules
	// ==================================================================

	private IDD buildFirewall() {
		IDD firewall = IDD.FALSE;

		// ----------------------------------------------------------------
		// 1-4: Loopback -- allow all traffic on 127.0.0.0/8
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("127.0.0.0/8", "127.0.0.0/8", "*", "*", null));

		// ----------------------------------------------------------------
		// 5-8: Established TCP from internal -- ephemeral src to well-known dst
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "*", "49152-65535", "1-1024", PROTO_TCP));

		// ----------------------------------------------------------------
		// 9-12: HTTP (80) -- internal to any server
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "*", "*", "80", PROTO_TCP));

		// ----------------------------------------------------------------
		// 13-16: HTTPS (443) -- internal to any server
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "*", "*", "443", PROTO_TCP));

		// ----------------------------------------------------------------
		// 17-20: DNS (UDP 53) -- internal to any resolver
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "*", "*", "53", PROTO_UDP));

		// ----------------------------------------------------------------
		// 21-24: DNS (TCP 53) -- zone transfers / large responses
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "*", "*", "53", PROTO_TCP));

		// ----------------------------------------------------------------
		// 25-28: ICMP -- internal to internal
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "10.0.0.0/8", "*", "*", PROTO_ICMP));

		// ----------------------------------------------------------------
		// 29-32: ICMP -- internal to DMZ
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "172.16.0.0/12", "*", "*", PROTO_ICMP));

		// ----------------------------------------------------------------
		// 33-36: SMTP (25) -- internal to DMZ mail servers
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "172.16.0.0/12", "*", "25", PROTO_TCP));

		// ----------------------------------------------------------------
		// 37-40: SSH (22) -- admin subnet to DMZ
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.10.0.0/16", "172.16.0.0/12", "*", "22", PROTO_TCP));

		// ----------------------------------------------------------------
		// 41-44: FTP control (21) -- internal to DMZ
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "172.16.0.0/12", "*", "21", PROTO_TCP));

		// ----------------------------------------------------------------
		// 45-48: FTP data (20) -- internal to DMZ
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "172.16.0.0/12", "*", "20", PROTO_TCP));

		// ----------------------------------------------------------------
		// 49-52: NTP (UDP 123) -- internal to any NTP server
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "*", "*", "123", PROTO_UDP));

		// ----------------------------------------------------------------
		// 53-56: SNMP (UDP 161) -- management to any device
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.10.0.0/16", "*", "*", "161", PROTO_UDP));

		// ----------------------------------------------------------------
		// 57-60: SIP (UDP 5060) -- VoIP signaling
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.20.0.0/16", "10.20.0.0/16", "*", "5060", PROTO_UDP));

		// ----------------------------------------------------------------
		// 61-64: RTP media (UDP 10000-20000) -- VoIP media
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.20.0.0/16", "10.20.0.0/16", "*", "10000-20000", PROTO_UDP));

		// ----------------------------------------------------------------
		// 65-68: IMAP (143) -- internal to DMZ mail
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "172.16.0.0/12", "*", "143", PROTO_TCP));

		// ----------------------------------------------------------------
		// 69-72: IMAPS (993) -- internal to DMZ mail
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "172.16.0.0/12", "*", "993", PROTO_TCP));

		// ----------------------------------------------------------------
		// 73-76: PostgreSQL (5432) -- app to DB
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.40.0.0/16", "*", "5432", PROTO_TCP));

		// ----------------------------------------------------------------
		// 77-80: MySQL (3306) -- app to DB
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.40.0.0/16", "*", "3306", PROTO_TCP));

		// ----------------------------------------------------------------
		// 81-84: Redis (6379) -- app to cache
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.50.0.0/16", "*", "6379", PROTO_TCP));

		// ----------------------------------------------------------------
		// 85-88: MongoDB (27017) -- app to DB
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.40.0.0/16", "*", "27017", PROTO_TCP));

		// ----------------------------------------------------------------
		// 89-92: LDAP (389) -- internal to directory
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "10.10.0.0/16", "*", "389", PROTO_TCP));

		// ----------------------------------------------------------------
		// 93-96: LDAPS (636) -- internal to directory
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "10.10.0.0/16", "*", "636", PROTO_TCP));

		// ----------------------------------------------------------------
		// 97-100: Kerberos (UDP 88) -- internal to KDC
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "10.10.0.0/16", "*", "88", PROTO_UDP));

		// ----------------------------------------------------------------
		// 101-104: RDP (3389) -- admin to management
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.10.0.0/16", "10.10.0.0/16", "*", "3389", PROTO_TCP));

		// ----------------------------------------------------------------
		// 105-108: Docker API (2376) -- orchestrator to nodes
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.60.0.0/16", "10.70.0.0/16", "*", "2376", PROTO_TCP));

		// ----------------------------------------------------------------
		// 109-112: Prometheus (9090) -- monitoring to any
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.80.0.0/16", "*", "*", "9090", PROTO_TCP));

		// ----------------------------------------------------------------
		// 113-116: Elasticsearch (9200) -- app to search cluster
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.90.0.0/16", "*", "9200", PROTO_TCP));

		// ----------------------------------------------------------------
		// 117-120: Kafka (9092) -- app to message broker
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.70.0.0/16", "*", "9092", PROTO_TCP));

		// ----------------------------------------------------------------
		// 121-124: gRPC (50051) -- app to microservices
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.30.0.0/16", "*", "50051", PROTO_TCP));

		// ----------------------------------------------------------------
		// 125-128: RabbitMQ AMQP (5672) -- app to message broker
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.70.0.0/16", "*", "5672", PROTO_TCP));

		// ----------------------------------------------------------------
		// 129-132: RabbitMQ management (15672) -- admin to broker mgmt
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.10.0.0/16", "10.70.0.0/16", "*", "15672", PROTO_TCP));

		// ----------------------------------------------------------------
		// 133-136: Memcached (11211) -- app to cache
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.50.0.0/16", "*", "11211", PROTO_TCP));

		// ----------------------------------------------------------------
		// 137-140: ZooKeeper (2181) -- app to coordination service
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.70.0.0/16", "*", "2181", PROTO_TCP));

		// ----------------------------------------------------------------
		// 141-144: Consistent hashing RPC (8080) -- app to gateway
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.0.0.0/8", "*", "8080", PROTO_TCP));

		// ----------------------------------------------------------------
		// 145-148: syslog (UDP 514) -- any internal to logging
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "10.80.0.0/16", "*", "514", PROTO_UDP));

		// ----------------------------------------------------------------
		// 149-152: HTTPS management (8443) -- admin to app mgmt endpoints
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.10.0.0/16", "10.30.0.0/16", "*", "8443", PROTO_TCP));

		// ----------------------------------------------------------------
		// 153-156: SNMP Trap (UDP 162) -- devices to NMS
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "10.10.0.0/16", "*", "162", PROTO_UDP));

		// ----------------------------------------------------------------
		// 157-160: TFTP (UDP 69) -- network mgmt to device firmware transfer
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.10.0.0/16", "10.0.0.0/8", "*", "69", PROTO_UDP));

		// ----------------------------------------------------------------
		// 161-164: GraphQL (4000) -- app to BFF
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.30.0.0/16", "*", "4000", PROTO_TCP));

		// ----------------------------------------------------------------
		// 165-168: NATS (4222) -- app to event bus
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.70.0.0/16", "*", "4222", PROTO_TCP));

		// ----------------------------------------------------------------
		// 169-172: Istio sidecar (15001) -- service mesh
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.30.0.0/16", "*", "15001", PROTO_TCP));

		// ----------------------------------------------------------------
		// 173-176: Istio pilot (15010) -- control plane
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.60.0.0/16", "10.30.0.0/16", "*", "15010", PROTO_TCP));

		// ----------------------------------------------------------------
		// 177-180: etcd (2379) -- Kubernetes API to etcd
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.60.0.0/16", "10.70.0.0/16", "*", "2379", PROTO_TCP));

		// ----------------------------------------------------------------
		// 181-184: Kubernetes API (6443) -- control plane to nodes
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.60.0.0/16", "10.70.0.0/16", "*", "6443", PROTO_TCP));

		// ----------------------------------------------------------------
		// 185-188: Container runtime (10250) -- kubelet API
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.60.0.0/16", "10.70.0.0/16", "*", "10250", PROTO_TCP));

		// ----------------------------------------------------------------
		// 189-192: Node exporter (9100) -- Prometheus scraping targets
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.80.0.0/16", "10.0.0.0/8", "*", "9100", PROTO_TCP));

		// ----------------------------------------------------------------
		// 193-196: cAdvisor (8080) -- container metrics
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.80.0.0/16", "10.70.0.0/16", "*", "8080", PROTO_TCP));

		// ----------------------------------------------------------------
		// 197-200: Grafana (3000) -- monitoring dashboard
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.10.0.0/16", "10.80.0.0/16", "*", "3000", PROTO_TCP));

		// ----------------------------------------------------------------
		// 201-204: Alertmanager (9093) -- alerts
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.80.0.0/16", "10.80.0.0/16", "*", "9093", PROTO_TCP));

		// ----------------------------------------------------------------
		// 205-208: ClickHouse (9000) -- app to analytics DB
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.90.0.0/16", "*", "9000", PROTO_TCP));

		// ----------------------------------------------------------------
		// 209-212: Fluentd (24224) -- logging pipeline
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.80.0.0/16", "*", "24224", PROTO_TCP));

		// ----------------------------------------------------------------
		// 213-216: Vault (8200) -- app to secrets manager
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.10.0.0/16", "*", "8200", PROTO_TCP));

		// ----------------------------------------------------------------
		// 217-220: Consul (8500) -- service discovery
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.30.0.0/16", "10.70.0.0/16", "*", "8500", PROTO_TCP));

		// ----------------------------------------------------------------
		// 221-224: DNS over HTTPS (443) -- internal DOH to public resolvers
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "*", "*", "443", PROTO_TCP));

		// ----------------------------------------------------------------
		// 225-228: SMTP submission (587) -- internal mail relay
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "172.16.0.0/12", "*", "587", PROTO_TCP));

		// ----------------------------------------------------------------
		// 229-232: HTTPS alternate (8443) -- internal to DMZ alternate
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "172.16.0.0/12", "*", "8443", PROTO_TCP));

		// ----------------------------------------------------------------
		// 233-236: HTTPS management (9443) -- JMX / management endpoints
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.10.0.0/16", "10.30.0.0/16", "*", "9443", PROTO_TCP));

		// ----------------------------------------------------------------
		// 237-240: ICMPv6 echo -- internal to internal (IPv6 ICMP, proto 1)
		// ----------------------------------------------------------------
		firewall = Apply.or(factory, firewall, rule("10.0.0.0/8", "10.0.0.0/8", "*", "*", PROTO_ICMP));

		return firewall;
	}

	// ==================================================================
	// Tests
	// ==================================================================

	@Test
	@DisplayName("Build 60-rule firewall IDD and verify structure")
	void testFirewallBuild() {
		IDD firewall = buildFirewall();

		// The diagram must be non-trivial.
		assertNotNull(firewall);
		assertTrue(!firewall.isTerminal(), "Firewall IDD should not be a terminal node");
	}

	// ------------------------------------------------------------------
	// Helper: pick a random IP within the given CIDR range
	// ------------------------------------------------------------------

	private static int randomInNetwork(Random rng, String cidr, int prefix) {
		int network = ip(cidr);
		int hostMask = prefix == 32 ? 0 : (1 << (32 - prefix)) - 1;
		return network | (rng.nextInt() & hostMask);
	}

	@Test
	@DisplayName("Evaluate 10000 random packets against the firewall")
	void testFirewallEvaluation() {
		IDD firewall = buildFirewall();
		Random rng = new Random(12345);

		long accepted = 0;
		long rejected = 0;
		int packetCount = 10_000;

		long start = System.currentTimeMillis();

		for (int i = 0; i < packetCount; i++) {
			// Generate a realistic mix of packets:
			// - 30% from known-allowed ranges (should be accepted)
			// - 30% from known-blocked ranges (should be rejected)
			// - 40% fully random across the int domain
			int srcIp, dstIp, srcPort, dstPort, proto;
			int roll = rng.nextInt(100);

			if (roll < 30) {
				// Crafted from rule-covered ranges -- should match
				srcIp = randomInNetwork(rng, "10.0.0.0", 8);
				dstIp = randomInNetwork(rng, "8.8.8.0", 24);
				srcPort = rng.nextInt(65536);
				int[] allowedPorts = { 80, 443, 53, 123, 161 };
				dstPort = allowedPorts[rng.nextInt(allowedPorts.length)];
				proto = PROTO_TCP;
			} else if (roll < 60) {
				// Crafted from clearly blocked ranges
				srcIp = randomInNetwork(rng, "203.0.113.0", 24);
				dstIp = randomInNetwork(rng, "10.0.1.0", 24);
				srcPort = rng.nextInt(65536);
				dstPort = rng.nextInt(65536);
				proto = PROTO_TCP;
			} else {
				// Fully random -- includes negatives (won't match)
				srcIp = rng.nextInt();
				dstIp = rng.nextInt();
				srcPort = rng.nextInt(65536);
				dstPort = rng.nextInt(65536);
				proto = rng.nextInt(256);
			}

			boolean match = Evaluate.evaluate(
				firewall,
				order,
				Map.of(
					VAR_SRC_IP,
					srcIp,
					VAR_DST_IP,
					dstIp,
					VAR_SRC_PORT,
					srcPort,
					VAR_DST_PORT,
					dstPort,
					VAR_PROTOCOL,
					proto
				)
			);

			if (match) {
				accepted++;
			} else {
				rejected++;
			}
		}

		long elapsed = System.currentTimeMillis() - start;

		System.out.println("=== Firewall Evaluation Results ===");
		System.out.println("Packets tested:  " + packetCount);
		System.out.println(
			"Accepted:        " + accepted + " (" + String.format("%.1f", (accepted * 100.0) / packetCount) + "%)"
		);
		System.out.println(
			"Rejected:        " + rejected + " (" + String.format("%.1f", (rejected * 100.0) / packetCount) + "%)"
		);
		System.out.println("Elapsed time:    " + elapsed + " ms");
		System.out.println(
			"Throughput:       " + String.format("%,d", (packetCount * 1000L) / Math.max(elapsed, 1)) + " packets/sec"
		);

		// Accepted and rejected should both be non-zero.
		assertTrue(accepted > 0, "Expected some packets to be accepted");
		assertTrue(rejected > 0, "Expected some packets to be rejected");
		assertTrue(elapsed < 30_000, "Evaluation took too long: " + elapsed + " ms");
	}

	@Test
	@DisplayName("Verify specific packets match expected rules")
	void testFirewallKnownPackets() {
		IDD firewall = buildFirewall();

		// Loopback traffic -- must be accepted
		assertAccepted(firewall, "127.0.0.1", "127.0.0.1", 12345, 80, PROTO_TCP, "loopback HTTP");

		// Internal -> any HTTP -- must be accepted
		assertAccepted(firewall, "10.0.1.5", "8.8.8.8", 54321, 80, PROTO_TCP, "internal -> external HTTP");

		// Internal -> any HTTPS -- must be accepted
		assertAccepted(firewall, "10.0.1.5", "1.1.1.1", 54322, 443, PROTO_TCP, "internal -> external HTTPS");

		// Internal -> any DNS (UDP) -- must be accepted
		assertAccepted(firewall, "10.1.2.3", "8.8.4.4", 12345, 53, PROTO_UDP, "internal -> DNS");

		// Internal -> internal ICMP -- must be accepted
		assertAccepted(firewall, "10.0.0.1", "10.0.0.2", 0, 0, PROTO_ICMP, "internal -> internal ping");

		// App -> DB PostgreSQL -- must be accepted
		assertAccepted(firewall, "10.30.1.10", "10.40.1.20", 49200, 5432, PROTO_TCP, "app -> postgresql");

		// Admin -> DMZ SSH -- must be accepted
		assertAccepted(firewall, "10.10.5.5", "172.16.1.1", 55555, 22, PROTO_TCP, "admin -> DMZ SSH");

		// Random external -> internal on random port -- must be rejected
		assertRejected(firewall, "203.0.113.50", "10.0.1.5", 12345, 8080, PROTO_TCP, "external -> internal (no rule)");

		// Internal -> external on unusual port/protocol -- must be rejected
		assertRejected(firewall, "10.0.1.5", "8.8.8.8", 12345, 31337, PROTO_TCP, "internal -> external (no rule)");

		// VoIP subnet RTP -- must be accepted
		assertAccepted(firewall, "10.20.1.1", "10.20.2.2", 5060, 15000, PROTO_UDP, "VoIP RTP media");

		// Monitoring -> any Prometheus -- must be accepted
		assertAccepted(firewall, "10.80.1.1", "10.30.1.1", 40000, 9090, PROTO_TCP, "monitoring -> prometheus");

		// ---- Mail protocols ----

		// SMTP (25) -- internal to DMZ
		assertAccepted(firewall, "10.0.1.5", "172.16.1.10", 54000, 25, PROTO_TCP, "internal -> DMZ SMTP");

		// SMTP submission (587) -- internal to DMZ
		assertAccepted(firewall, "10.0.1.5", "172.16.5.10", 54001, 587, PROTO_TCP, "internal -> DMZ SMTP submission");

		// IMAP (143) -- internal to DMZ
		assertAccepted(firewall, "10.0.1.5", "172.16.1.10", 54002, 143, PROTO_TCP, "internal -> DMZ IMAP");

		// IMAPS (993) -- internal to DMZ
		assertAccepted(firewall, "10.0.1.5", "172.16.1.10", 54003, 993, PROTO_TCP, "internal -> DMZ IMAPS");

		// ---- FTP ----

		// FTP control (21) -- internal to DMZ
		assertAccepted(firewall, "10.0.1.5", "172.16.2.10", 54004, 21, PROTO_TCP, "internal -> DMZ FTP control");

		// FTP data (20) -- internal to DMZ
		assertAccepted(firewall, "10.0.1.5", "172.16.2.10", 54005, 20, PROTO_TCP, "internal -> DMZ FTP data");

		// ---- Database protocols ----

		// MySQL (3306) -- app to DB
		assertAccepted(firewall, "10.30.1.10", "10.40.1.20", 49300, 3306, PROTO_TCP, "app -> mysql");

		// MongoDB (27017) -- app to DB
		assertAccepted(firewall, "10.30.1.10", "10.40.1.20", 49301, 27017, PROTO_TCP, "app -> mongodb");

		// Redis (6379) -- app to cache
		assertAccepted(firewall, "10.30.1.10", "10.50.1.20", 49302, 6379, PROTO_TCP, "app -> redis");

		// ---- Directory / auth ----

		// LDAP (389) -- internal to directory
		assertAccepted(firewall, "10.0.1.5", "10.10.1.10", 54100, 389, PROTO_TCP, "internal -> LDAP");

		// LDAPS (636) -- internal to directory
		assertAccepted(firewall, "10.0.1.5", "10.10.1.10", 54101, 636, PROTO_TCP, "internal -> LDAPS");

		// Kerberos (UDP 88) -- internal to KDC
		assertAccepted(firewall, "10.0.1.5", "10.10.1.10", 54102, 88, PROTO_UDP, "internal -> Kerberos");

		// ---- Time / discovery ----

		// NTP (UDP 123) -- internal to any NTP server
		assertAccepted(firewall, "10.0.1.5", "8.8.8.8", 54200, 123, PROTO_UDP, "internal -> NTP");

		// SNMP (UDP 161) -- management to any
		assertAccepted(firewall, "10.10.1.5", "192.168.1.1", 54201, 161, PROTO_UDP, "management -> SNMP");

		// ---- VoIP ----

		// SIP (UDP 5060) -- VoIP signaling within VoIP subnet
		assertAccepted(firewall, "10.20.1.1", "10.20.2.2", 5060, 5060, PROTO_UDP, "VoIP SIP signaling");

		// ---- Message brokers ----

		// Kafka (9092) -- app to broker
		assertAccepted(firewall, "10.30.1.10", "10.70.1.20", 49400, 9092, PROTO_TCP, "app -> Kafka");

		// RabbitMQ AMQP (5672) -- app to broker
		assertAccepted(firewall, "10.30.1.10", "10.70.1.20", 49401, 5672, PROTO_TCP, "app -> RabbitMQ AMQP");

		// RabbitMQ management (15672) -- admin to broker mgmt
		assertAccepted(firewall, "10.10.1.5", "10.70.1.20", 54300, 15672, PROTO_TCP, "admin -> RabbitMQ management");

		// NATS (4222) -- app to event bus
		assertAccepted(firewall, "10.30.1.10", "10.70.1.20", 49402, 4222, PROTO_TCP, "app -> NATS");

		// ---- Cache / coordination ----

		// Memcached (11211) -- app to cache
		assertAccepted(firewall, "10.30.1.10", "10.50.1.20", 49500, 11211, PROTO_TCP, "app -> Memcached");

		// ZooKeeper (2181) -- app to coordination
		assertAccepted(firewall, "10.30.1.10", "10.70.1.20", 49501, 2181, PROTO_TCP, "app -> ZooKeeper");

		// ---- Microservices / API ----

		// gRPC (50051) -- app to microservices
		assertAccepted(firewall, "10.30.1.10", "10.30.2.20", 49600, 50051, PROTO_TCP, "app -> gRPC");

		// GraphQL (4000) -- app to BFF
		assertAccepted(firewall, "10.30.1.10", "10.30.2.20", 49601, 4000, PROTO_TCP, "app -> GraphQL");

		// ---- Search / analytics ----

		// Elasticsearch (9200) -- app to search
		assertAccepted(firewall, "10.30.1.10", "10.90.1.20", 49700, 9200, PROTO_TCP, "app -> Elasticsearch");

		// ClickHouse (9000) -- app to analytics DB
		assertAccepted(firewall, "10.30.1.10", "10.90.1.20", 49701, 9000, PROTO_TCP, "app -> ClickHouse");

		// ---- Kubernetes ----

		// etcd (2379) -- K8s API to etcd
		assertAccepted(firewall, "10.60.1.10", "10.70.1.20", 49800, 2379, PROTO_TCP, "K8s API -> etcd");

		// Kubernetes API (6443) -- control plane to nodes
		assertAccepted(firewall, "10.60.1.10", "10.70.1.20", 49801, 6443, PROTO_TCP, "K8s control plane -> nodes");

		// Container runtime (10250) -- kubelet API
		assertAccepted(firewall, "10.60.1.10", "10.70.1.20", 49802, 10250, PROTO_TCP, "K8s -> kubelet API");

		// ---- Monitoring stack ----

		// Node exporter (9100) -- Prometheus scraping
		assertAccepted(firewall, "10.80.1.1", "10.30.1.10", 49900, 9100, PROTO_TCP, "monitoring -> node exporter");

		// cAdvisor (8080) -- container metrics
		assertAccepted(firewall, "10.80.1.1", "10.70.1.10", 49901, 8080, PROTO_TCP, "monitoring -> cAdvisor");

		// Grafana (3000) -- admin to dashboard
		assertAccepted(firewall, "10.10.1.5", "10.80.1.10", 54400, 3000, PROTO_TCP, "admin -> Grafana");

		// Alertmanager (9093) -- monitoring internal
		assertAccepted(firewall, "10.80.1.1", "10.80.1.20", 49902, 9093, PROTO_TCP, "monitoring -> Alertmanager");

		// ---- Logging ----

		// Fluentd (24224) -- app to logging
		assertAccepted(firewall, "10.30.1.10", "10.80.1.20", 49910, 24224, PROTO_TCP, "app -> Fluentd");

		// syslog (UDP 514) -- internal to logging
		assertAccepted(firewall, "10.0.1.5", "10.80.1.20", 54500, 514, PROTO_UDP, "internal -> syslog");

		// ---- Service mesh ----

		// Istio sidecar (15001) -- service mesh data plane
		assertAccepted(firewall, "10.30.1.10", "10.30.2.20", 49950, 15001, PROTO_TCP, "app -> Istio sidecar");

		// Istio pilot (15010) -- control plane
		assertAccepted(firewall, "10.60.1.10", "10.30.1.10", 49951, 15010, PROTO_TCP, "K8s -> Istio pilot");

		// ---- Secrets / discovery ----

		// Vault (8200) -- app to secrets
		assertAccepted(firewall, "10.30.1.10", "10.10.1.20", 49960, 8200, PROTO_TCP, "app -> Vault");

		// Consul (8500) -- app to service discovery
		assertAccepted(firewall, "10.30.1.10", "10.70.1.20", 49961, 8500, PROTO_TCP, "app -> Consul");

		// ---- Management protocols ----

		// RDP (3389) -- admin to management
		assertAccepted(firewall, "10.10.1.5", "10.10.2.10", 54600, 3389, PROTO_TCP, "admin -> RDP");

		// Docker API (2376) -- orchestrator to nodes
		assertAccepted(firewall, "10.60.1.10", "10.70.1.20", 49970, 2376, PROTO_TCP, "orchestrator -> Docker API");

		// HTTPS management (8443) -- admin to app mgmt
		assertAccepted(firewall, "10.10.1.5", "10.30.1.10", 54601, 8443, PROTO_TCP, "admin -> HTTPS management");

		// HTTPS management (9443) -- JMX / management endpoints
		assertAccepted(firewall, "10.10.1.5", "10.30.1.10", 54602, 9443, PROTO_TCP, "admin -> HTTPS JMX management");

		// HTTPS alternate (8443) -- internal to DMZ
		assertAccepted(firewall, "10.0.1.5", "172.16.1.10", 54603, 8443, PROTO_TCP, "internal -> DMZ HTTPS alternate");

		// TFTP (UDP 69) -- network mgmt to devices
		assertAccepted(firewall, "10.10.1.5", "10.0.1.5", 54700, 69, PROTO_UDP, "mgmt -> TFTP");

		// SNMP Trap (UDP 162) -- devices to NMS
		assertAccepted(firewall, "10.0.1.5", "10.10.1.20", 54701, 162, PROTO_UDP, "internal -> SNMP Trap");

		// DNS (TCP 53) -- zone transfers
		assertAccepted(firewall, "10.0.1.5", "8.8.8.8", 54702, 53, PROTO_TCP, "internal -> DNS TCP");

		// Established TCP (ephemeral -> well-known) -- internal to any
		assertAccepted(
			firewall,
			"10.0.1.5",
			"192.168.1.1",
			50000,
			80,
			PROTO_TCP,
			"established TCP ephemeral -> well-known"
		);

		// ---- Rejection cases ----

		// Wrong protocol: DNS over TCP from internal (not matching UDP rule)
		assertRejected(firewall, "10.0.1.5", "8.8.8.8", 12345, 53, PROTO_ICMP, "internal -> DNS (wrong protocol ICMP)");

		// Wrong source subnet for app->DB: non-app subnet to DB
		assertRejected(
			firewall,
			"10.20.1.5",
			"10.40.1.20",
			49200,
			5432,
			PROTO_TCP,
			"non-app -> postgresql (wrong subnet)"
		);

		// Wrong source subnet for app->Redis: non-app subnet to cache
		assertRejected(firewall, "10.20.1.5", "10.50.1.20", 49200, 6379, PROTO_TCP, "non-app -> redis (wrong subnet)");

		// Non-admin SSH to DMZ (use srcPort outside ephemeral range so it
		// doesn't match established TCP rule)
		assertRejected(firewall, "10.30.1.5", "172.16.1.1", 12345, 22, PROTO_TCP, "non-admin -> DMZ SSH");

		// External ICMP to internal (no rule)
		assertRejected(firewall, "203.0.113.50", "10.0.1.5", 0, 0, PROTO_ICMP, "external -> internal ICMP");

		// VoIP from non-VoIP subnet
		assertRejected(firewall, "10.30.1.5", "10.20.1.1", 5060, 5060, PROTO_UDP, "non-VoIP -> VoIP SIP");

		// Wrong port for NTP (TCP instead of UDP) -- use non-ephemeral srcPort
		// to avoid matching established TCP rule
		assertRejected(firewall, "10.0.1.5", "8.8.8.8", 123, 123, PROTO_TCP, "internal -> NTP (wrong protocol TCP)");

		// Non-admin RDP attempt
		assertRejected(firewall, "10.30.1.5", "10.10.1.10", 54600, 3389, PROTO_TCP, "non-admin -> RDP");

		// DMZ to internal (reverse direction, no rule)
		assertRejected(firewall, "172.16.1.1", "10.0.1.5", 80, 54321, PROTO_TCP, "DMZ -> internal (no rule)");

		// Wrong port for SMTP submission (port 25 instead of 587 on a
		// submission rule)
		// Note: port 25 IS allowed by the SMTP(25) rule to DMZ, so test a
		// clearly unmatched combo
		assertRejected(
			firewall,
			"10.0.1.5",
			"172.16.1.10",
			54000,
			993,
			PROTO_UDP,
			"internal -> DMZ IMAPS (wrong protocol UDP)"
		);

		// Monitoring scraping from non-monitoring subnet
		assertRejected(firewall, "10.30.1.5", "10.40.1.20", 40000, 9090, PROTO_TCP, "non-monitoring -> prometheus");

		// Non-K8s control plane trying etcd
		assertRejected(firewall, "10.30.1.5", "10.70.1.20", 49800, 2379, PROTO_TCP, "non-K8s -> etcd");

		// Loopback mismatch (src is loopback but dst is not)
		assertRejected(firewall, "127.0.0.1", "10.0.1.5", 12345, 80, PROTO_TCP, "loopback src -> non-loopback dst");

		// Established rule wrong direction: well-known src to ephemeral dst
		// (reversed)
		assertRejected(
			firewall,
			"10.0.1.5",
			"8.8.8.8",
			80,
			50000,
			PROTO_TCP,
			"established TCP reversed (well-known src)"
		);
	}

	// ------------------------------------------------------------------
	// Assertion helpers
	// ------------------------------------------------------------------

	private void
			assertAccepted(IDD firewall, String src, String dst, int srcPort, int dstPort, int proto, String label) {
		boolean result = Evaluate.evaluate(
			firewall,
			order,
			Map.of(
				VAR_SRC_IP,
				ip(src),
				VAR_DST_IP,
				ip(dst),
				VAR_SRC_PORT,
				srcPort,
				VAR_DST_PORT,
				dstPort,
				VAR_PROTOCOL,
				proto
			)
		);
		assertTrue(
			result,
			"Expected ACCEPT for: " + label + " (" + src + ":" + srcPort + " -> " + dst + ":" + dstPort + " proto="
					+ proto + ")"
		);
	}

	private void
			assertRejected(IDD firewall, String src, String dst, int srcPort, int dstPort, int proto, String label) {
		boolean result = Evaluate.evaluate(
			firewall,
			order,
			Map.of(
				VAR_SRC_IP,
				ip(src),
				VAR_DST_IP,
				ip(dst),
				VAR_SRC_PORT,
				srcPort,
				VAR_DST_PORT,
				dstPort,
				VAR_PROTOCOL,
				proto
			)
		);
		assertTrue(
			!result,
			"Expected REJECT for: " + label + " (" + src + ":" + srcPort + " -> " + dst + ":" + dstPort + " proto="
					+ proto + ")"
		);
	}
}
