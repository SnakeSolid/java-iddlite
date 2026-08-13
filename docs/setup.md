# Development Setup

## Prerequisites

| Requirement | Version |
|---|---|
| JDK | 17+ |
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
├── pom.xml                          # Maven build configuration
├── mvnw / mvnw.cmd                 # Maven wrapper
├── AGENTS.md                       # Agent navigation hub
├── README.md                       # Project overview
├── docs/                           # Documentation
│   ├── setup.md
│   ├── architecture.md
│   ├── coding-standards.md
│   ├── testing.md
│   ├── benchmark-results.md
│   ├── memory-benchmarks.md
│   └── deployment.md
└── src/
    ├── main/java/ru/snake/collection/idd/
    │   ├── core/                   # Core data structures
    │   │   ├── IDD.java
    │   │   ├── Edge.java
    │   │   ├── IDDFactory.java
    │   │   ├── IDDBuilder.java
    │   │   └── VariableOrder.java
    │   ├── operation/              # IDD operations
    │   │   ├── Apply.java
    │   │   ├── Evaluate.java
    │   │   ├── Quantify.java
    │   │   └── Restrict.java
    │   └── util/                   # Utilities
    │       ├── Interval.java
    │       ├── DotExporter.java
    │       └── IDDPrinter.java
    └── test/java/ru/snake/collection/idd/
        ├── unit/                   # Unit tests (11 classes)
        └── integration/            # Integration / stress tests (2 classes)
```

## Useful Maven commands

```bash
./mvnw compile                  # Compile source code
./mvnw test                    # Run all tests
./mvnw package                 # Build JAR
./mvnw javadoc:javadoc         # Generate Javadoc
./mvnw verify                  # Run full lifecycle
```
