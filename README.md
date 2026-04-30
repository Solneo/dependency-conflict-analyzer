# Dependency Conflict Analyzer Gradle Plugin

[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/solneo/dependency-conflict-analyzer/blank.yml)](https://github.com/Solneo/dependency-conflict-analyzer/actions/workflows/blank.yml)
[![GitHub](https://img.shields.io/github/license/solneo/dependency-conflict-analyzer)](https://www.apache.org/licenses/LICENSE-2.0)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.solneo.dependency-conflict-analyzer)](https://plugins.gradle.org/plugin/io.github.solneo.dependency-conflict-analyzer)

A Gradle plugin that detects dependency version conflicts during sync and shows where each conflicting version came from.

## Why this plugin

Gradle already has tools for exploring dependency conflicts: the `dependencyInsight` task and the `failOnVersionConflict` resolution strategy. Both are reactive — you use them when you already know (or suspect) there's a problem, and you have to ask about a specific artifact.

This plugin takes a different angle. It analyzes the resolved dependency graph on every sync and reports conflicts automatically, with full paths showing where each version came from. Conflicts surface on their own, without having to query the build.

## What it does

On every Gradle sync, the plugin traverses the resolved dependency graph and identifies artifacts that appear in more than one version. For each conflict it reports:

- All versions present in the graph
- The full dependency path for each version — through which modules and libraries it was pulled in
- Which version Gradle ended up selecting

By default, the plugin only prints the report. You can also configure it to fail the build when conflicts are detected.

## Scope: major versions only

The plugin reports conflicts between **different major versions** of an artifact (for example, `1.7.25` vs `2.0.17`). Minor and patch differences are not reported.

This is by design. Under semantic versioning, major version bumps are where breaking API changes happen — these are the conflicts most likely to cause real runtime issues. Minor and patch differences are usually safe, and reporting them would generate noise without clear signal.

## Compatibility

- Gradle **8.3+** (tested with 8.3, 8.5, and 9.5)
- Configuration cache: **supported**
- Works with Java, Kotlin, and Android projects

## Usage

### Groovy

On your `build.gradle` add:

```groovy
plugins {
  id "io.github.solneo.dependency-conflict-analyzer" version "1.4.0"
}
```

### Kotlin DSL

On your `build.gradle.kts` add:

```kotlin 
plugins {
    id("io.github.solneo.dependency-conflict-analyzer") version "1.4.0"
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

Gradle's `ResolutionResult` only contains edges that survived the final resolution.
If a transitive dependency requests version `X`, but a higher version `Y` is already present
in the graph via another path, Gradle may drop the edge for `X` entirely — it never appears in the resolved graph.

As a result, the plugin cannot report conflicts involving versions that were dropped by Gradle before the graph was finalized.

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

## Example output

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

For more context, see the [article on Medium](https://medium.com/@chdanilr/gradle-plugin-to-catch-version-conflicts-and-their-sources-early-bcc75f509766).