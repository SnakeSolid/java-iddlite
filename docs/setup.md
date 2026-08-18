# Development Setup

## Prerequisites

| Requirement | Version |
|---|---|
| JDK | 21+ |
| Maven | 3.8+ (or use the bundled wrapper `./mvnw`) |

## Building from source

```bash
# Clone
git clone <repo-url> && cd iddlite

# Compile
./mvnw compile

# Run all tests
./mvnw test

# Package JAR
./mvnw package
```

## IDE configuration

| IDE | Setup |
|---|---|
| IntelliJ IDEA | Open `pom.xml` as a project; Maven import is automatic |
| Eclipse | `File > Import > Maven > Existing Maven Projects` |
| VS Code | Install the Redhat Maven extension; run `./mvnw compile` |

## Project structure

```
iddlite/
├── pom.xml                          # Parent POM (multi-module)
├── mvnw / mvnw.cmd                 # Maven wrapper
├── .editorconfig                   # Editor configuration
├── AGENTS.md                       # Agent navigation hub
├── README.md                       # Project overview
├── LICENSE                         # MIT license
├── docs/                           # Documentation
│   ├── setup.md
│   ├── architecture.md
│   ├── coding-standards.md
│   ├── testing.md
│   ├── benchmark-results.md
│   ├── memory-benchmarks.md
│   └── deployment.md
├── idd-core/                       # Core library module
│   ├── pom.xml
│   └── src/
│       ├── main/java/ru/snake/collection/idd/
│       │   ├── core/               # Core data structures
│       │   │   ├── IDD.java
│       │   │   ├── Edge.java
│       │   │   ├── IDDFactory.java
│       │   │   ├── IDDBuilder.java
│       │   │   └── VariableOrder.java
│       │   ├── operation/          # IDD operations
│       │   │   ├── Apply.java
│       │   │   ├── Evaluate.java
│       │   │   ├── Quantify.java
│       │   │   └── Restrict.java
│       │   └── util/               # Utilities
│       │       ├── VariableRange.java
│       │       ├── ValueFormatter.java
│       │       ├── Formatters.java
│       │       ├── MermaidExporter.java
│       │       ├── IDDPrinter.java
│       │       └── IDDTraversal.java
│       └── test/java/ru/snake/collection/idd/
│           ├── unit/               # Unit tests (11 classes, 138 tests)
│           └── integration/        # Integration / stress tests (4 classes, 22 tests)
├── idd-firewall/                   # Firewall CLI module
│   ├── pom.xml
│   ├── rules.txt                   # Sample firewall rules
│   ├── packets.txt                 # Sample packets
│   └── src/
│       ├── main/java/ru/snake/collection/idd/firewall/
│       │   ├── FirewallCli.java
│       │   ├── FirewallParser.java
│       │   ├── FirewallBuilder.java
│       │   ├── FirewallRule.java
│       │   ├── FirewallPacket.java
│       │   ├── FirewallVars.java
│       │   ├── IpUtil.java
│       │   └── PacketParser.java
│       └── test/java/ru/snake/collection/idd/firewall/
│           ├── FirewallBuilderTest.java
│           ├── FirewallParserTest.java
│           ├── IpUtilTest.java
│           └── PacketParserTest.java
└── idd-benchmark/                  # JMH benchmark module
    ├── pom.xml
    └── src/main/java/ru/snake/collection/idd/benchmark/
        └── FirewallEvaluationBenchmark.java
```

## Useful Maven commands

```bash
./mvnw compile                  # Compile all modules
./mvnw test                     # Run all tests
./mvnw package                  # Build all JARs
./mvnw -pl idd-core test        # Test core module only
./mvnw -pl idd-benchmark package   # Build benchmark JAR
java -jar idd-benchmark/target/idd-benchmark-1.0.0.jar   # Run benchmarks
./mvnw javadoc:javadoc         # Generate Javadoc
./mvnw verify                  # Run full lifecycle
```
