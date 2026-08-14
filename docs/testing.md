# Testing Strategy

## Framework

- **JUnit 5** (Jupiter) — all tests use `@Test`, `@DisplayName`, `@BeforeEach`.
- Assertions via `org.junit.jupiter.api.Assertions.*` (static imports).

## Test structure

```
src/test/java/ru/snake/collection/idd/
├── unit/               — Unit tests for individual components
│   ├── VariableRangeTest — Valid range construction, singleton, contains, equality
│   ├── IDDTest         — Node creation, hash-consing, reduction, range-aware gap filling
│   ├── EdgeTest        — Edge invariants, equality, findEdge
│   ├── ApplyTest       — Boolean operations, De Morgan, cross-variable AND, ranged operations
│   ├── EvaluateTest    — Evaluation of terminals, intervals, multi-variable, ranged evaluation
│   ├── QuantifyTest    — Exists, forall, non-present variables, ranged quantification
│   ├── RestrictTest    — Restrict terminals, correct child, recurse, ranged restriction
│   ├── BuilderTest     — Fluent builder, canonicity, empty builder, ranged builder
│   ├── IntervalTest    — Interval invariants, contains, adjacency
│   ├── IDDPrinterTest  — Print modes, formatters, tree output
│   └── VariableOrderTest — Order basics, duplicates, bounds, compare, range lookup
└── integration/        — Integration and stress tests
    ├── ExtremeIntervalTest — MIN/MAX boundary correctness
    ├── StressTest        — Large-scale firewall simulation
    ├── FirewallRuleTest  — 54-rule firewall with 5 variables
    └── RangedFirewallTest — Firewall with port/protocol ranges, quantify, restrict, performance
```

## Coverage summary

| Category | Tests |
|---|---|
| **Unit tests** | 82 |
| **Integration tests** | 20 |
| **Total** | 102 |

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

### Variable range testing

Tests with custom `VariableRange` verify:
- **Gap filling** uses the variable's range boundaries instead of `Integer.MIN/MAX_VALUE`.
- **Reduction** eliminates nodes when a single edge covers the full custom range.
- **Edge validation** rejects edges that fall outside the variable's range.
- **Hash-consing** works correctly with ranged variables.
- **All operations** (AND, OR, NOT, XOR, IMPLIES, exists, forall, restrict) produce correct results with ranged variables.

## Running tests

```bash
./mvnw test                          # All tests
./mvnw test -Dtest=ApplyTest         # Single test class
./mvnw test -Dtest="*Stress*"        # Pattern match
```

## What is NOT tested

- `DotExporter` and `IDDPrinter` are output utilities tested implicitly through integration scenarios. Dedicated tests could be added for exact output format validation.
- `ConcurrentHashMap`-based factory (not yet implemented) would need concurrency-specific tests.
