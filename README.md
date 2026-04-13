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
  id "io.github.solneo.dependency-conflict-analyzer" version "1.2.4"
}
```

### Kotlin DSL

On your `build.gradle.kts` add:

```kotlin 
plugins {
    id("io.github.solneo.dependency-conflict-analyzer") version "1.2.4"
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
## Known Limitations

### Silently Upgraded Transitive Dependencies

Gradle's resolution result only includes edges that survive the final resolution process.
If a transitive dependency requests version `X`, but a higher version `Y` already exists
in the graph, Gradle may omit the edge for `X` entirely — it never appears in the resolved graph.

As a result, the plugin cannot report conflicts involving versions that were silently
upgraded by Gradle before the graph was finalized.

**Example:** if `library-a:1.0` transitively requires `slf4j:1.7.25`, but `slf4j:2.0.9`
is already present via another path, Gradle may drop the `slf4j:1.7.25` edge completely.
The plugin will not report a conflict in this case.

> **Workaround:** explicitly declare the dependency version in your module.
> This forces the edge to appear in the graph and makes the conflict visible.

### Dependencies Excluded from Analysis

The following are not included in conflict detection:

- Dependencies excluded via `excludeCheckingLibraries` or `excludeCheckingLibrariesGroup`
- Dependencies resolved via BOM (`platform()` / `enforcedPlatform()`) where versions are aligned before graph construction
- Dependencies suppressed via `resolutionStrategy.force()` or `resolutionStrategy.eachDependency {}`


The plugin outputs a report to the console:


<details>
<summary>Text Report</summary>

```
Version conflict detected: org.slf4j:slf4j-api
- version 2.0.17 via:
     - project :app -> ch.qos.logback:logback-classic:1.4.11 -> org.slf4j:slf4j-api:2.0.17
     - project :app -> project :profile -> project :favorites -> ch.qos.logback:logback-classic:1.5.32 -> org.slf4j:slf4j-api:2.0.17
     - project :app -> project :feed -> project :favorites -> ch.qos.logback:logback-classic:1.5.32 -> org.slf4j:slf4j-api:2.0.17
- version 1.7.25 via:
     - project :app -> org.apache.logging.log4j:log4j-slf4j-impl:2.17.1 -> org.slf4j:slf4j-api:1.7.25
- version 2.0.9 via:
     - project :app -> org.slf4j:slf4j-api:2.0.9
→ using 2.0.17
```
Note: dependencies are used only as an example
</details>
