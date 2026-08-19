# IDD Lite

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Interval Decision Diagram** library — a lightweight, canonical representation for Boolean functions over integer interval domains.

## What it does

An IDD represents a Boolean function whose variables range over integer intervals. Internally it uses a reduced, hash-consed ordered graph (similar to BDDs) so that:

- **Structural sharing** — identical sub-expressions are the same object.
- **Canonicity** — two IDDs are equal iff they are the same reference.
- **Fast evaluation** — O(depth) walk through the diagram.
- **Variable ranges** — each variable can have a custom valid range (e.g., `0..65535` for ports, `0..255` for protocols).

Ideal for firewall rule sets, policy evaluation, and any domain with interval-based predicates.

## Quick start

```java
// 1. Define variable order
VariableOrder order = new VariableOrder("x", "y");
IDDFactory factory = new IDDFactory(order);

// 2. Build with the fluent builder
IDD rule = factory.builder()
    .when("x").in(1, 10).then(true)
    .when("x").in(11, 20).then(false)
    .build();

// 3. Combine with Boolean operations
IDD combined = factory.and(rule, anotherRule);
IDD negated = factory.not(rule);

// 4. Restrict a variable to a concrete value
IDD restricted = Restrict.restrict(factory, rule, "x", 5);

// 5. Evaluate — Map-based (convenient)
boolean result = Evaluate.evaluate(rule, order, Map.of("x", 7));

//    Evaluate — int[] (zero-allocation, high-throughput)
boolean fast = Evaluate.evaluate(rule, new int[] { 7, 3 });
```

### Variable ranges

Constrain variables to their semantic domains:

```java
VariableOrder order = new VariableOrder("proto", "port");
VariableRanges ranges = new VariableRanges(
    Map.of(
        "port",  VariableRange.of(0, 65535),
        "proto", VariableRange.of(0, 255)
    ),
    order
);
IDDFactory factory = new IDDFactory(order, ranges);
```

Gap filling and reduction use each variable's range instead of `Integer.MIN_VALUE..Integer.MAX_VALUE`.

## Key features

| Feature | Description |
|---|---|
| Hash-consing | Unique table via `WeakHashMap` — unreachable nodes are GC'd |
| Edge normalisation | Gaps filled with FALSE edges; adjacent same-child edges merged |
| Reduction | Nodes with a single full-range edge are eliminated |
| Variable ranges | Custom valid ranges per variable (e.g., ports: 0..65535) |
| Boolean operations | AND, OR, NOT, XOR, IMPLIES (via `IDDFactory` instance methods) |
| Quantification | Existential (`exists`) and universal (`forall`) |
| Restriction / cofactoring | Fix a variable to a concrete value |
| Fast evaluation | O(depth) walk — `int[]` path avoids per-call Map allocation |
| Visualization | Mermaid diagram export + pretty-printer (3 modes) |

## Modules

| Module | Package | Description |
|---|---|---|
| `idd-core` | `ru.snake.collection.idd.core.*` | Core IDD library — nodes, factory, operations, visualization |
| `idd-firewall` | `ru.snake.collection.idd.firewall` | Firewall rule analysis CLI — parses rules, builds policy IDD, evaluates packets |
| `idd-benchmark` | `ru.snake.collection.idd.benchmark` | JMH benchmarks for compilation and evaluation performance |

## Building

```bash
./mvnw compile
```

## Running tests

```bash
./mvnw test
```

## Running benchmarks

The project includes a JMH benchmark module that can be built into a standalone JAR:

```bash
./mvnw -pl idd-benchmark package
java -jar idd-benchmark/target/idd-benchmark-1.0.0.jar
```

See [Benchmark results](./docs/benchmark-results.md) for details on running with specific parameters.

## Documentation

- [Development setup](./docs/setup.md)
- [Architecture & design](./docs/architecture.md)
- [Coding conventions](./docs/coding-standards.md)
- [Testing strategy](./docs/testing.md)
- [Benchmark results](./docs/benchmark-results.md)
- [Memory footprint](./docs/memory-benchmarks.md)
- [Deployment & operations](./docs/deployment.md)

## License

This project is licensed under the [MIT License](LICENSE).
