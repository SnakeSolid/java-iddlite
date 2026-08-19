package ru.snake.collection.idd.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.snake.collection.idd.core.Edge;
import ru.snake.collection.idd.core.IDD;

class EdgeTest {

	@Test
	@DisplayName("Edge invariants: low <= high")
	void testLowAtMostHigh() {
		assertThrows(IllegalArgumentException.class, () -> new Edge(5, 3, IDD.TRUE));
	}

	@Test
	@DisplayName("Edge invariants: child must not be null")
	void testChildNotNull() {
		assertThrows(NullPointerException.class, () -> new Edge(1, 5, null));
	}

	@Test
	@DisplayName("Edge equality and hashCode")
	void testEquality() {
		Edge a = new Edge(1, 5, IDD.TRUE);
		Edge b = new Edge(1, 5, IDD.TRUE);
		Edge c = new Edge(1, 5, IDD.FALSE);
		assertEquals(a, b);
		assertNotEquals(a, c);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	@DisplayName("findEdge returns the correct edge")
	void testFindEdge() {
		List<Edge> edges = List.of(new Edge(1, 5, IDD.TRUE), new Edge(6, 10, IDD.FALSE));
		assertEquals(IDD.TRUE, Edge.findEdge(edges, 3).child());
		assertEquals(IDD.FALSE, Edge.findEdge(edges, 8).child());
	}
}
