# Testing Strategy

## Framework

- **JUnit 5** (Jupiter) — all tests use `@Test`, `@DisplayName`, `@BeforeEach`.
- Assertions via `org.junit.jupiter.api.Assertions.*` (static imports).

## Test structure

```
src/test/java/ru/snake/collection/idd/
├── unit/               — Unit tests for individual components
│   ├── IDDTest         — Node creation, hash-consing, reduction
│   ├── EdgeTest        — Edge invariants, equality, findEdge
│   ├── ApplyTest       — Boolean operations, De Morgan, cross-variable AND
│   ├── EvaluateTest    — Evaluation of terminals, intervals, multi-variable
│   ├── QuantifyTest    — Exists, forall, non-present variables
│   ├── RestrictTest    — Restrict terminals, correct child, recurse
│   ├── BuilderTest     — Fluent builder, canonicity, empty builder
│   ├── IntervalTest    — Interval invariants, contains, adjacency
│   └── VariableOrderTest — Order basics, duplicates, bounds, compare
└── integration/        — Integration and stress tests
    ├── ExtremeIntervalTest — MIN/MAX boundary correctness
    └── StressTest        — Large-scale firewall simulation
```

## Coverage summary

| Category | Tests |
|---|---|
| **Unit tests** | 58 |
| **Integration tests** | 11 |
| **Total** | 69 |

## Testing patterns used

### Structural assertions

- `assertSame()` — verifies hash-consing (identical nodes are the same object).
- `assertNotSame()` — verifies distinct nodes are different objects.
- `assertEquals()` — verifies structural properties (edge count, boundaries).

### Evaluation-based verification

For complex operations, tests evaluate the resulting IDD at multiple points to verify correctness:

```java
for (int v = 0; v <= 10; v++) {
    boolean l = Evaluate.evaluate(left, order, Map.of("x", v));
    boolean r = Evaluate.evaluate(right, order, Map.of("x", v));
    assertEquals(l, r, "Mismatch at x=" + v);
}
```

### Stress tests

- **Firewall simulation**: 50 random rules, 50,000 evaluations — must complete under 30 seconds.
- **Many boundaries**: 30 random intervals combined with OR, then ANDed — verifies apply scaling.
- **NOT stress**: 20 random intervals, then verify `f OR NOT(f) == TRUE`.

### Boundary testing

- `Integer.MIN_VALUE` and `Integer.MAX_VALUE` are exercised explicitly.
- Single-value intervals at `MAX`.
- Overflow-safe `nextLow()` calculation.

## Running tests

```bash
./mvnw test                          # All tests
./mvnw test -Dtest=ApplyTest         # Single test class
./mvnw test -Dtest="*Stress*"        # Pattern match
```

## What is NOT tested

- `DotExporter` and `IDDPrinter` are output utilities tested implicitly through integration scenarios. Dedicated tests could be added for exact output format validation.
- `ConcurrentHashMap`-based factory (not yet implemented) would need concurrency-specific tests.
