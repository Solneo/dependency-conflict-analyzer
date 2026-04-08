# Dependency Conflict Analyzer Gradle Plugin

This plugin scans dependencies during Gradle sync and detects potential dependency conflicts.

Gradle resolves dependency conflicts by selecting the highest version, which can silently introduce incompatibilities. This plugin helps detect such cases early and understand their root cause.

It analyzes **major versions of artifacts** and highlights potentially incompatible upgrades selected by Gradle. The plugin also provides **dependency paths** for each conflicting version, helping you quickly identify which dependencies introduced the conflict and why it happened.

Optionally, you can enable build failure on detected conflicts.

[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/solneo/dependency-conflict-analyzer/blank.yml)](https://github.com/Solneo/dependency-conflict-analyzer/actions/workflows/blank.yml)
[![GitHub](https://img.shields.io/github/license/solneo/dependency-conflict-analyzer)](https://www.apache.org/licenses/LICENSE-2.0)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.solneo.dependency-conflict-analyzer)](https://plugins.gradle.org/plugin/io.github.solneo.dependency-conflict-analyzer)

## Usage

### Groovy

On your `build.gradle` add:

```groovy
plugins {
  id "io.github.solneo.dependency-conflict-analyzer" version "1.2.0"
}
```

### Kotlin DSL

On your `build.gradle.kts` add:

```kotlin 
plugins {
    id("io.github.solneo.dependency-conflict-analyzer") version "1.2.0"
}
```

## Configuration

You can enable build failure on detected conflicts using the `failOnConflict` option:

### Groovy

```groovy
dependencyConflictAnalyzer {
   failOnConflict = false
}
```

### Kotlin DSL

```kotlin
dependencyConflictAnalyzer {
   failOnConflict.set(false)
}
```

You can also exclude specific groups or artifacts from analysis:

### Groovy

```groovy
dependencyConflictAnalyzer {
   excludeCheckingLibrariesGroup = ["com.example.code.group"]
   excludeCheckingLibraries = ["com.example.code.group:artifact"]
}
```

### Kotlin DSL

```kotlin
dependencyConflictAnalyzer {
   excludeCheckingLibrariesGroup.set(listOf("com.example.code.group"))
   excludeCheckingLibraries.set(listOf("com.example.code.group:artifact"))
}
```

The plugin outputs a report to the console:


<details>
<summary>Text Report</summary>

```
Danger conflict with com.google.guava:guava between:
- version 32.1.3-jre via:
     - project :app -> com.google.guava:guava:32.1.3-jre
- version 19.0 via:
     - project :app -> com.google.inject:guice:4.1.0 -> com.google.guava:guava:19.0
→ using 32.1.3-jre
```
Note: dependencies are used only as an example
</details>
