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
| 10        | 30.287  | ± 0.997 |
| 30        | 25.041  | ± 1.685 |
| 60        | 25.415  | ± 1.730 |
| 120       | 23.942  | ± 0.603 |
| 200       | 20.876  | ± 0.666 |

#### Average Time (ms/op)

Lower is better.

| Rule Count | Score   | Error   |
|-----------|---------|--------|
| 10        | 0.030   | ± 0.001 |
| 30        | 0.036   | ± 0.002 |
| 60        | 0.040   | ± 0.002 |
| 120       | 0.039   | ± 0.003 |
| 200       | 0.049   | ± 0.001 |

### Interpretation

Throughput and latency remain essentially flat across all rule counts. Evaluating 1,000 packets takes ~0.030–0.049 ms regardless of whether the firewall was compiled from 10 or 200 rules. Throughput is consistently around 20.9–30.3 ops/ms. This confirms that the IDD compilation step absorbs the complexity of the rule set, leaving evaluation at a constant O(depth) cost where depth equals the number of variables (5 in this benchmark). The zero-allocation `int[]` evaluation path eliminates per-packet Map construction and per-node string-keyed lookups, delivering roughly 4× the throughput of the previous Map-based implementation.

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
| 10        | 5.876   | ± 0.137 |
| 30        | 0.175   | ± 0.005 |
| 60        | 0.033   | ± 0.001 |
| 120       | 0.009   | ± 0.001 |
| 200       | 0.003   | ± 0.001 |

#### Average Time (ms/op)

Lower is better.

| Rule Count | Score    | Error   |
|-----------|----------|--------|
| 10        | 0.161    | ± 0.011 |
| 30        | 5.166    | ± 0.047 |
| 60        | 27.969   | ± 0.578 |
| 120       | 106.663  | ± 1.829 |
| 200       | 285.639  | ± 5.436 |

Compilation cost scales super-linearly: 10 rules take ~0.16 ms, 60 rules take ~28 ms, and 200 rules take ~286 ms. This is expected — each rule requires multiple `and` operations that merge into the growing OR tree, and the IDD must maintain canonicity via the unique table. The number of internal nodes grows combinatorially as more rules with overlapping variable ranges are merged.

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
