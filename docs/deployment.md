# Deployment & Operations

## Building the artifact

```bash
./mvnw clean package
```

Produces:
- `idd-core/target/idd-core-1.0.0.jar` — core library
- `idd-firewall/target/idd-firewall-1.0.0.jar` — firewall CLI tool (fat JAR)
- `idd-benchmark/target/idd-benchmark-1.0.0.jar` — standalone benchmark JAR (fat JAR with all dependencies)

## Dependencies

The **core library** and **firewall module** have **zero runtime dependencies**. They require only Java 21+:

```xml
<dependency>
    <groupId>ru.snake.collection</groupId>
    <artifactId>idd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

The **benchmark module** depends on JMH (managed by the parent POM's `dependencyManagement`).

## Multi-module structure

The project uses a parent POM (`iddlite`) with three child modules. All dependency versions are centralised in the parent via `<dependencyManagement>`:

| Module | Description |
|---|---|
| `idd-core` | Core IDD library (zero runtime deps) |
| `idd-firewall` | Firewall rule analysis CLI (depends on `idd-core`) |
| `idd-benchmark` | JMH benchmarks against `idd-core` |

Versions of shared dependencies (`junit-jupiter`, `jmh-core`, `jmh-generator-annprocess`) are defined once in the parent POM properties and propagated via `<dependencyManagement>`.

## Installation

### Local install

```bash
./mvnw install
```

Makes the artifact available in the local Maven repository (`~/.m2/repository`).

### Remote deployment

Not yet configured. To deploy to a remote repository (e.g., Sonatype OSSRH, GitHub Packages), add a distribution management section to `pom.xml`:

```xml
<distributionManagement>
    <repository>
        <id>ossrh</id>
        <url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
    </repository>
</distributionManagement>
```

## Javadoc

Generate API documentation:

```bash
./mvnw javadoc:javadoc
```

Output: `target/site/apidocs/`.

## Usage as a library

```java
// 1. Define variable order
VariableOrder order = new VariableOrder("src_ip", "dst_ip", "port");

// 2. Create factory
IDDFactory factory = new IDDFactory(order);

// 3. Build rules
IDD rule1 = factory.buildFromIntervals("src_ip", List.of(
    new Edge(10_000_000, 10_000_100, factory.trueNode())
));

// 4. Combine
IDD policy = factory.and(rule1, rule2);

// 5. Evaluate
boolean allowed = Evaluate.evaluate(policy, order, Map.of(
    "src_ip", 10_000_050,
    "dst_ip", 20_000_000,
    "port", 8080
));
```

## Visualization

Export an IDD to a Mermaid diagram file:

```java
MermaidExporter.export(rootIdd, order, "diagram.mmd");
// The .mmd file renders as a flowchart in Mermaid-compatible viewers
// (GitHub, GitLab, Zed, etc.)
```

Or get the diagram as a string (useful for embedding or logging):

```java
String mermaid = MermaidExporter.toString(rootIdd, order);
String numeric = MermaidExporter.toString(rootIdd);  // numeric variable labels
```

All `toString` and `export` methods have overloads that accept a
`ValueFormatter` to customise how edge interval values are displayed
(e.g. IPv4 dotted-decimal, protocol names):

```java
ValueFormatter formatter = Formatters.builder(order)
    .forIndex("src_ip", Formatters.ipv4())
    .forIndex("dst_ip", Formatters.ipv4())
    .forIndex("protocol", Formatters.ipProtocol())
    .build();

String diagram = MermaidExporter.toString(rootIdd, order, formatter);
MermaidExporter.export(rootIdd, order, formatter, "diagram.mmd");
```

Or pretty-print to stdout:

```java
System.out.println(IDDPrinter.print(rootIdd, order));
```

## Thread safety

- Core classes (`IDD`, `Edge`, `Interval`, `VariableOrder`) are fully thread-safe (immutable).
- Operation classes (`Apply`, `Evaluate`, `Quantify`, `Restrict`) are stateless — thread-safe.
- **`IDDFactory` is NOT thread-safe** — it uses a `WeakHashMap`. Share a factory per thread, or use external synchronisation.

## Performance tips

1. **Reuse factories** — hash-consing is most effective when a single factory creates all nodes.
2. **Use `VariableOrder` wisely** — put the most selective variables first for faster evaluation.
3. **Batch operations** — creating many small IDDs and combining them is more memory-efficient than building one massive rule directly.
4. **Let GC do its job** — the `WeakHashMap` unique table self-evicts; don't add manual cache clearing.
