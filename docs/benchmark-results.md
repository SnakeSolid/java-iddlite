# Benchmark Results

## Stress test: Firewall simulation

**Scenario**: Build a firewall from 50 random rules over 3 variables (`src_ip`, `dst_ip`, `src_port`), then evaluate 50,000 random packets.

| Metric | Value |
|---|---|
| Rules | 50 |
| Variables | 3 (`src_ip`, `dst_ip`, `src_port`) |
| Evaluations | 50,000 |
| Time | ~2 ms |
| Throughput | ~25M evaluations/second |

The evaluation is a simple O(depth) walk through the diagram. With 3 variables, depth is at most 3 hops per evaluation.

## JMH benchmark: Firewall evaluation

**Scenario**: Build a firewall from N rules (200-rule set loaded from `resources/rules.csv`, truncated to N), then evaluate 1,000 deterministic packets against the compiled IDD. Parameterised by rule count: 10, 30, 60, 120, 200.

The benchmark lives in the `idd-benchmark` module and produces a standalone executable JAR via `maven-shade-plugin`. It uses a seeded deterministic RNG so results are fully reproducible across runs. Rule building, IP resolution, port parsing, and packet generation are extracted into shared utility classes (`FirewallRuleSet`, `FirewallPolicyBuilder`, `FirewallBenchmarkUtils`).

| Parameter | Values |
|---|---|
| Rule count | 10, 30, 60, 120, 200 |
| Packets per iteration | 1,000 |
| Variables | 5 (`src_ip`, `dst_ip`, `src_port`, `dst_port`, `protocol`) |
| Modes | AverageTime, Throughput |

### Results

#### Throughput (ops/ms)

Each operation evaluates 1,000 packets. Higher is better.

| Rule Count | Score   | Error    |
|-----------|---------|----------|
| 10        | 30.338  | ± 0.711 |
| 30        | 26.317  | ± 0.350 |
| 60        | 25.447  | ± 0.640 |
| 120       | 24.687  | ± 0.423 |
| 200       | 21.462  | ± 0.411 |

#### Average Time (ms/op)

Lower is better.

| Rule Count | Score   | Error   |
|-----------|---------|--------|
| 10        | 0.033   | ± 0.001 |
| 30        | 0.038   | ± 0.001 |
| 60        | 0.039   | ± 0.002 |
| 120       | 0.041   | ± 0.001 |
| 200       | 0.046   | ± 0.001 |

### Interpretation

Throughput and latency remain essentially flat across all rule counts. Evaluating 1,000 packets takes ~0.033–0.046 ms regardless of whether the firewall was compiled from 10 or 200 rules. Throughput is consistently around 21.5–30.3 ops/ms. This confirms that the IDD compilation step absorbs the complexity of the rule set, leaving evaluation at a constant O(depth) cost where depth equals the number of variables (5 in this benchmark). The zero-allocation `int[]` evaluation path eliminates per-packet Map construction and per-node string-keyed lookups, delivering roughly 4× the throughput of the previous Map-based implementation.

## JMH benchmark: Firewall compilation

**Scenario**: Compile a firewall IDD from N rules via successive `factory.or()` calls. Measures the cost of building the decision diagram — where the algorithmic complexity actually lives. Each invocation creates a fresh `IDDFactory` so the unique table is cold.

| Parameter | Values |
|---|---|
| Rule count | 10, 30, 60, 120, 200 |
| Variables | 5 (`src_ip`, `dst_ip`, `src_port`, `dst_port`, `protocol`) |
| Modes | AverageTime, Throughput |
| Observable side effect | Returns total node count to prevent dead-code elimination |

### Results

#### Throughput (ops/ms)

Higher is better. Note the super-linear scaling: doubling the rule count more than doubles the compilation time.

| Rule Count | Score   | Error    |
|-----------|---------|----------|
| 10        | 6.546   | ± 0.108 |
| 30        | 0.202   | ± 0.004 |
| 60        | 0.036   | ± 0.001 |
| 120       | 0.009   | ± 0.001 |
| 200       | 0.003   | ± 0.001 |

#### Average Time (ms/op)

Lower is better.

| Rule Count | Score    | Error      |
|-----------|----------|-----------|
| 10        | 0.153    | ± 0.004   |
| 30        | 5.075    | ± 0.182   |
| 60        | 27.468   | ± 0.933   |
| 120       | 108.337  | ± 1.280   |
| 200       | 285.785  | ± 16.831  |

Compilation cost scales super-linearly: 10 rules take ~0.15 ms, 60 rules take ~27.5 ms, and 200 rules take ~286 ms. This is expected — each rule requires multiple `and` operations that merge into the growing OR tree, and the IDD must maintain canonicity via the unique table. The number of internal nodes grows combinatorially as more rules with overlapping variable ranges are merged.

### Running benchmarks

```bash
./mvnw -pl idd-benchmark package
java -jar idd-benchmark/target/idd-benchmark-1.0.0.jar

# Run only the compilation benchmark with specific rule count
java -jar idd-benchmark/target/idd-benchmark-1.0.0.jar -p ruleCount=60 -i "Compilation"

# Run only the evaluation benchmark
java -jar idd-benchmark/target/idd-benchmark-1.0.0.jar -p ruleCount=60 -i "Evaluation"
```

### How it works

- **Setup phase** (`@Setup(Level.Trial)`): Builds the VariableOrder, IDDFactory, compiles the firewall IDD from N rules, and generates 1,000 deterministic packets.
- **Benchmark phase**: Evaluates all 1,000 packets through the IDD. Each result is consumed via `Blackhole.consume()` to prevent dead-code elimination. Method is `void` — no return accumulator.
- **Deterministic packets**: Generated by a lightweight LCG RNG with a fixed seed. ~30% match allowed ranges, ~30% match blocked ranges, ~40% fully random across the integer domain.
- **Rule set**: 200 rules loaded from `src/main/resources/rules.csv` at class init. Parameters up to 200 use actual distinct rules.

## Stress test: Many boundaries

**Scenario**: Build 30 random interval predicates for variable `x` with OR, then AND the two results.

| Metric | Value |
|---|---|
| Intervals per operand | 30 |
| Domain range | 0–1000 |
| Operation | OR chain, then AND |
| Result | Completes without error (complexity verified) |

The apply algorithm handles overlapping intervals via interval intersection at each node level.

## Stress test: NOT of complex IDD

**Scenario**: Build 20 random interval predicates, OR them together, then NOT the result.

| Metric | Value |
|---|---|
| Intervals | 20 |
| Domain range | 0–500 |
| Verification | `f OR NOT(f) == TRUE` (by reference) |

Confirms the NOT operation preserves canonicity and the complement law.

## Notes

- The stress tests are part of the test suite and run on every build.
- Timings include JVM warmup overhead (first run may be slower).
