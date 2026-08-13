# Benchmark Results

## Stress test: Firewall simulation

**Scenario**: Build a firewall from 50 random rules over 3 variables (`src_ip`, `dst_ip`, `src_port`), then evaluate 50,000 random packets.

| Metric | Value |
|---|---|
| Rules | 50 |
| Variables | 3 (`src_ip`, `dst_ip`, `src_port`) |
| Evaluations | 50,000 |
| Time | ~34 ms |
| Throughput | ~1.47M evaluations/second |

The evaluation is a simple O(depth) walk through the diagram. With 3 variables, depth is at most 3 hops per evaluation.

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

- Benchmarks run on the machine executing `./mvnw test`. Results vary by hardware.
- The stress tests are part of the test suite and run on every build.
- Timings include JVM warmup overhead (first run may be slower).
