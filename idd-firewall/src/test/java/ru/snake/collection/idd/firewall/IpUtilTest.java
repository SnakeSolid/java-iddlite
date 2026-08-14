package ru.snake.collection.idd.firewall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IpUtilTest {

	@Test
	@DisplayName("Parse simple IP")
	void testParseIp() {
		int ip = IpUtil.parseIp("192.168.1.1");
		assertEquals(0xC0A80101, ip);
	}

	@Test
	@DisplayName("Parse zero IP")
	void testParseZeroIp() {
		assertEquals(0, IpUtil.parseIp("0.0.0.0"));
	}

	@Test
	@DisplayName("Parse max IP")
	void testParseMaxIp() {
		assertEquals(0xFFFFFFFF, IpUtil.parseIp("255.255.255.255"));
	}

	@Test
	@DisplayName("CIDR range for /8")
	void testCidrRange8() {
		int[] range = IpUtil.cidrRange("10.0.0.0/8");
		assertEquals(IpUtil.parseIp("10.0.0.0"), range[0]);
		assertEquals(IpUtil.parseIp("10.255.255.255"), range[1]);
	}

	@Test
	@DisplayName("CIDR range for /16")
	void testCidrRange16() {
		int[] range = IpUtil.cidrRange("192.168.0.0/16");
		assertEquals(IpUtil.parseIp("192.168.0.0"), range[0]);
		assertEquals(IpUtil.parseIp("192.168.255.255"), range[1]);
	}

	@Test
	@DisplayName("CIDR range for /32")
	void testCidrRange32() {
		int[] range = IpUtil.cidrRange("10.0.0.1/32");
		assertEquals(IpUtil.parseIp("10.0.0.1"), range[0]);
		assertEquals(IpUtil.parseIp("10.0.0.1"), range[1]);
	}

	@Test
	@DisplayName("CIDR range for /0")
	void testCidrRange0() {
		int[] range = IpUtil.cidrRange("0.0.0.0/0");
		assertEquals(0, range[0]);
		// 0xFFFFFFFF is -1 as a signed int.
		assertEquals(-1, range[1]);
	}

	@Test
	@DisplayName("Invalid IP format throws")
	void testInvalidIp() {
		assertThrows(IllegalArgumentException.class, () ->
			IpUtil.parseIp("256.0.0.0")
		);
	}

	@Test
	@DisplayName("Invalid IP (missing octets) throws")
	void testMissingOctets() {
		assertThrows(IllegalArgumentException.class, () ->
			IpUtil.parseIp("10.0.0")
		);
	}

	@Test
	@DisplayName("Invalid CIDR prefix throws")
	void testInvalidCidrPrefix() {
		assertThrows(IllegalArgumentException.class, () ->
			IpUtil.cidrRange("10.0.0.0/33")
		);
	}
}
