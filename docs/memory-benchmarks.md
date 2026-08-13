# Memory Footprint

## Object sizes (estimated)

| Class | Fields | Approximate size |
|---|---|---|
| `IDD` | `int variable`, `List<Edge> edges`, `int hashCode` | ~40 bytes (plus list + edges) |
| `Edge` | `int low`, `int high`, `IDD child` | ~28 bytes |
| `Interval` | `int low`, `int high` | ~20 bytes (created on-demand) |
| `VariableOrder` | `List<String>`, `Map<String, Integer>` | Depends on variable count |

## Unique table

The `IDDFactory` uses a `WeakHashMap<NodeKey, IDD>` as the unique table. Key properties:

- **Weak keys** — unreachable `NodeKey` objects are garbage-collected, freeing their associated `IDD` entries.
- **Memory pressure** — under heavy churn (creating many temporary IDDs), the GC will reclaim unused nodes automatically.
- **Long-lived factories** — safe for application lifetime; the unique table acts as a cache that self-evicts.

## Sharing benefits

Hash-consing means:
- Sub-expressions that appear multiple times share a single object.
- `TRUE` and `FALSE` are singletons — zero overhead.
- For a typical firewall with many rules sharing the same sub-predicates, the IDD is significantly smaller than the sum of individual rules.

## Example: 50-rule firewall

The stress test builds 50 random rules and ORs them. The resulting IDD contains shared sub-nodes — each unique interval partition is stored once. The exact node count depends on interval overlap, but is typically far less than `50 × 3` (the naive count of 150 nodes).

## Memory considerations

- For very large diagrams (thousands of nodes), the `WeakHashMap` overhead is the dominant factor.
- If tight memory control is needed, a custom unique table with size-bounded eviction could replace `WeakHashMap`.
- The `NodeKey` object is short-lived (created during `getNode()` calls) and eligible for GC once the entry is in the map.

## Future work

- Benchmark with `ConcurrentHashMap` for thread-safe factories.
- Measure heap usage with JFR (`jdk.jfr.event.GC`) for long-running processes.
- Evaluate alternative unique table implementations (e.g., interned node keys).
