# Coding Conventions

## Language and compilation

- **Java 21** — compiler source/target/release set to 21 in parent `pom.xml`.
- **Zero runtime dependencies** — only the JDK standard library. JUnit 5 is test-scoped.

## Naming

| Element | Convention | Example |
|---|---|---|
| Classes | `PascalCase` | `IDD`, `IDDFactory`, `Operation`, `VariableRange` |
| Methods / fields | `camelCase` | `getNode`, `uniqueTable`, `currentVarIndex` |
| Constants | `UPPER_SNAKE_CASE` | `TRUE`, `FALSE` |
| Package | lowercase, dot-separated | `ru.snake.collection.idd.core`, `ru.snake.collection.idd.operation`, `ru.snake.collection.idd.util` |
| Test classes | `<ClassName>Test` | `IDDTest`, `ApplyTest` |
| Test methods | `test<Description>` | `testInterning`, `testDeMorgan1` |

## Indentation and braces

- **Tab indentation** — every source file uses tabs (not spaces).
- **Braces always required** — every `if`, `else`, `for`, `while`, `switch` block must have braces, even for a single statement.

```java
// Correct
if (x < 0) {
    return false;
}

// Forbidden — no bare single-statement blocks
if (x < 0)
    return false;
```

## Line structure

- **Blank lines** separate logical groups: field declarations, constructors, method groups, and between methods.
- **One declaration per line** — fields and local variables are each on their own line.
- **Long expressions** are wrapped with the continuation indented one tab level deeper.

```java
IDD rule = factory.and(
    rule,
    factory.buildFromIntervals("dst_ip", List.of(new Edge(dstLow, dstHigh, factory.trueNode())))
);
```

## Class design

### Immutability

All core classes are `final` with `private final` fields. No setters exist.

| Class | Mutability |
|---|---|
| `IDD` | immutable |
| `Edge` | immutable |
| `VariableOrder` | immutable (variable ordering only) |
| `VariableRanges` | immutable (range lookup maps) |
| `VariableRange` | immutable |
| `Operation` | immutable (enum) |
| `IDDFactory` | mutable state (`uniqueTable`, `applyCache`) |
| `IDDBuilder` | mutable (builder pattern, consumed on `build()`) |

### Visibility

| Level | Usage |
|---|---|
| `public` | API surface — classes, factory methods, operations |
| package-private | `IDD` constructors, `IDDBuilder` constructor, `IDD.create()` |
| `private` | Internal helpers, empty constructors on utility classes |
| `static` | Convenience operations (`Evaluate.evaluate()`, `Quantify.exists()`, etc.) |
| `instance` | Factory methods (`factory.and()`, `factory.or()`, `factory.not()`, `factory.xor()`, `factory.implies()`) |

### Utility classes

Operation classes with only static methods have a `private` no-arg constructor to prevent instantiation:

```java
public final class Evaluate {
    private Evaluate() {
    }
    // ...
}
```

All Boolean apply operations (`and`, `or`, `xor`, `implies`, `not`) are instance methods on `IDDFactory` rather than a separate `Apply` class. This avoids the per-call cache allocation problem: every static call previously created a new `Apply` instance with a fresh, useless `WeakHashMap`.

### Accessor naming

Getter methods use the field name without a `get` prefix (property-style):

```java
public int variable() { return variable; }
public List<Edge> edges() { return edges; }
public int low() { return low; }
```

Boolean accessors use an `is` prefix:

```java
public boolean isTerminal() { return variable < 0; }
public boolean isTrue() { return this == TRUE; }
public boolean isFalse() { return this == FALSE; }
```

## Control flow

### `if-else` chains

Prefer `if-else if` chains for multi-path decisions rather than bare `if` sequences:

```java
if (this == o) {
    return true;
} else if (o == null || getClass() != o.getClass()) {
    return false;
}
```

### Guard clauses

Early returns for terminal or edge cases keep the happy path unindented:

```java
public static boolean evaluate(IDD f, VariableOrder order, Map<String, Integer> assignment) {
    if (f.isTerminal()) {
        return f.isTrue();
    }
    // ... main logic
}
```

## Error handling

| Exception | When |
|---|---|
| `IllegalArgumentException` | Invalid arguments (`low > high`, variable < 0, empty edges, unknown variable name, out-of-range index, edge outside variable's range) |
| `NullPointerException` | Null child in `Edge`, or null `VariableOrder` in `IDDFactory` |
| `IllegalStateException` | Internal invariant violated (e.g., no edge covers a value during evaluation) |

Fail fast — validate at construction time.

## Equality and hashing

### `IDD` — reference equality

Because hash-consing guarantees canonicity:

```java
@Override
public boolean equals(Object o) {
    if (this == o) {
        return true;
    } else if (o == null || getClass() != o.getClass()) {
        return false;
    }

    return this == o;  // reference equality is sufficient
}
```

### Other classes — structural equality

`Edge`, `VariableRange`, `NodeKey`, `OperationKey` implement structural `equals`/`hashCode` using `Objects.hash()` or the `31*h + field` pattern. Child references in `Edge` and `NodeKey` use `System.identityHashCode()` (reference-based) rather than structural hash, because `IDD` equality is by reference.

## Comments and Javadoc

- Every `public` class and method has a Javadoc comment.
- Class-level Javadoc explains the purpose and key invariants.
- Method-level Javadoc uses `@param`, `@return`, `@throws` tags.
- Inline comments explain non-obvious logic (algorithm steps, overflow protection).

```java
// Use long to detect overflow: high == Integer.MAX_VALUE means nextLow
// would overflow
return (long) high + 1 == nextLow;
```

## Imports

- Grouped by origin: `java.*` first, then project packages (`ru.snake.collection.*`).
- Blank line between groups.
- Static imports for assertions (`org.junit.jupiter.api.Assertions.*`).
- No wildcard imports except static assertion imports in tests.

## Records

Internal DTOs may use Java records for brevity:

```java
private record Rule(int varIndex, int low, int high, boolean isTrue) {
}
```

## Collections

- `List.of()` for immutable empty or small lists.
- `List.copyOf()` to return an unmodifiable copy from a computation.
- `ArrayList` for mutable accumulation.
- `WeakHashMap` for caches/unique tables that should be GC-friendly.
- `LinkedHashSet` when iteration order matters during deduplication.
- `IdentityHashMap` for reference-keyed maps (e.g., DOT export node labels).

## Tests

- Test class visibility is package-private (no `public`).
- `@BeforeEach` for fixture setup; `@DisplayName` for readable test labels.
- Static imports for all assertions.
- Prefer `assertSame` for hash-consing / canonicity checks.
- Prefer `assertThrows` with lambda for exception tests.
- Integration tests go in `integration` package; unit tests in `unit`.
