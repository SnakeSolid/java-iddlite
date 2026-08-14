package ru.snake.collection.idd.firewall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
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
		assertEquals(-1, IpUtil.parseIp("255.255.255.255"));
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

	@Test
	@DisplayName("CIDR intervals for non-wrapping range")
	void testCidrIntervalsNormal() {
		List<int[]> intervals = IpUtil.cidrIntervals("10.0.0.0/8");
		assertEquals(1, intervals.size());
		assertEquals(IpUtil.parseIp("10.0.0.0"), intervals.get(0)[0]);
		assertEquals(IpUtil.parseIp("10.255.255.255"), intervals.get(0)[1]);
	}

	@Test
	@DisplayName("CIDR intervals for wrapping /0 range splits into two")
	void testCidrIntervalsWrap() {
		// 0.0.0.0/0 wraps: network=0, broadcast=-1 (as signed ints).
		List<int[]> intervals = IpUtil.cidrIntervals("0.0.0.0/0");
		assertEquals(2, intervals.size());
		assertEquals(0, intervals.get(0)[0]);
		assertEquals(Integer.MAX_VALUE, intervals.get(0)[1]);
		assertEquals(Integer.MIN_VALUE, intervals.get(1)[0]);
		assertEquals(IpUtil.parseIp("255.255.255.255"), intervals.get(1)[1]);
	}

	@Test
	@DisplayName("CIDR intervals for non-wrapping /1 range")
	void testCidrIntervalsNonWrap() {
		// 128.0.0.0/1: network=MIN_VALUE, broadcast=-1 -- no wrap in signed.
		List<int[]> intervals = IpUtil.cidrIntervals("128.0.0.0/1");
		assertEquals(1, intervals.size());
		assertEquals(Integer.MIN_VALUE, intervals.get(0)[0]);
		assertEquals(-1, intervals.get(0)[1]);
	}
}
