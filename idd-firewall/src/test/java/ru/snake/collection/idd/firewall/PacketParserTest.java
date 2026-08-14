package ru.snake.collection.idd.firewall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PacketParserTest {

	@Test
	@DisplayName("Parse simple packet with proto name")
	void testSimplePacket() throws Exception {
		String input = "192.168.1.10 10.0.0.1 12345 80 tcp\n";
		List<FirewallPacket> packets = PacketParser.parse(new StringReader(input));
		assertEquals(1, packets.size());
		FirewallPacket pkt = packets.get(0);
		assertEquals(IpUtil.parseIp("192.168.1.10"), pkt.srcIp());
		assertEquals(IpUtil.parseIp("10.0.0.1"), pkt.dstIp());
		assertEquals(12345, pkt.srcPort());
		assertEquals(80, pkt.dstPort());
		assertEquals(6, pkt.proto());
	}

	@Test
	@DisplayName("Parse packet with numeric protocol")
	void testNumericProto() throws Exception {
		String input = "10.0.0.5 10.0.0.1 54321 443 6\n";
		List<FirewallPacket> packets = PacketParser.parse(new StringReader(input));
		assertEquals(6, packets.get(0).proto());
	}

	@Test
	@DisplayName("Parse multiple packets")
	void testMultiplePackets() throws Exception {
		String input =
			"192.168.1.10 10.0.0.1 12345 80 tcp\n" +
			"10.0.0.5 10.0.0.1 54321 443 tcp\n" +
			"0.0.0.0 0.0.0.0 0 0 icmp\n";
		List<FirewallPacket> packets = PacketParser.parse(new StringReader(input));
		assertEquals(3, packets.size());
	}

	@Test
	@DisplayName("Skip comments and blank lines")
	void testComments() throws Exception {
		String input =
			"# comment\n" +
			"\n" +
			"192.168.1.10 10.0.0.1 12345 80 tcp\n" +
			"# another\n";
		List<FirewallPacket> packets = PacketParser.parse(new StringReader(input));
		assertEquals(1, packets.size());
	}

	@Test
	@DisplayName("Wrong number of fields throws")
	void testWrongFields() {
		assertThrows(IllegalArgumentException.class, () ->
			PacketParser.parse(new StringReader("192.168.1.10 10.0.0.1 12345 80\n"))
		);
	}

	@Test
	@DisplayName("Invalid IP throws")
	void testInvalidIp() {
		assertThrows(IllegalArgumentException.class, () ->
			PacketParser.parse(new StringReader("bad.ip.ad.dr 10.0.0.1 12345 80 tcp\n"))
		);
	}

	@Test
	@DisplayName("toAssignment produces correct map")
	void testToAssignment() throws Exception {
		String input = "10.0.0.1 172.16.0.1 1000 22 tcp\n";
		List<FirewallPacket> packets = PacketParser.parse(new StringReader(input));
		FirewallPacket pkt = packets.get(0);
		var map = pkt.toAssignment();
		assertEquals(1000, (int) map.get("src_port"));
		assertEquals(22, (int) map.get("dst_port"));
		assertEquals(6, (int) map.get("proto"));
	}
}
