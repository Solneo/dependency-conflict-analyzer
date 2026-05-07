# Dependency Conflict Analyzer Gradle Plugin

[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/solneo/dependency-conflict-analyzer/blank.yml)](https://github.com/Solneo/dependency-conflict-analyzer/actions/workflows/blank.yml)
[![GitHub](https://img.shields.io/github/license/solneo/dependency-conflict-analyzer)](https://www.apache.org/licenses/LICENSE-2.0)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.solneo.dependency-conflict-analyzer)](https://plugins.gradle.org/plugin/io.github.solneo.dependency-conflict-analyzer)

A Gradle plugin that detects dependency version conflicts and shows where each conflicting version came from.

## Why this plugin

Gradle already has tools for exploring dependency conflicts: the `dependencyInsight` task and the `failOnVersionConflict` resolution strategy. Both are reactive — you use them when you already know (or suspect) there's a problem, and you have to ask about a specific artifact.

This plugin takes a different angle. It analyzes the resolved dependency graph on every sync and reports conflicts automatically, with full paths showing where each version came from. Conflicts surface on their own, without having to query the build.

## What it does

The plugin traverses the resolved dependency graph and identifies artifacts that appear in more than one version. For each conflict it reports:

- All versions present in the graph
- The full dependency path for each version — through which modules and libraries it was pulled in
- Which version Gradle ended up selecting

Two report formats are available:
- **Console** — printed automatically on every sync
- **Markdown** — generated on demand via [`generateDependencyConflictReport`](#markdown-report)

You can also configure the build to fail when conflicts are detected.

## Scope: major versions only

The plugin reports conflicts between **different major versions** of an artifact (for example, `1.7.25` vs `2.0.17`). Minor and patch differences are not reported.

This is by design. Under semantic versioning, major version bumps are where breaking API changes happen — these are the conflicts most likely to cause real runtime issues. Minor and patch differences are usually safe, and reporting them would generate noise without clear signal.

## Compatibility

- Gradle **8.3+** (tested with 8.3, 8.5, and 9.5)
- Configuration cache: **supported** (console report only; see [Markdown report](#markdown-report) for caveats)
- Works with Java, Kotlin, and Android projects

## Usage

### Groovy

On your `build.gradle` add:

```groovy
plugins {
  id "io.github.solneo.dependency-conflict-analyzer" version "1.5.0"
}
```

### Kotlin DSL

On your `build.gradle.kts` add:

```kotlin
plugins {
    id("io.github.solneo.dependency-conflict-analyzer") version "1.5.0"
}
```

## Markdown report

To generate a standalone Markdown report, run:

```
./gradlew generateDependencyConflictReport
```

The report is saved to `build/reports/dependency-conflict-analyzer/report.md` by default. Use `reportFile` in the extension to change the path.

> **Note:** `generateDependencyConflictReport` is not compatible with the configuration cache. This is consistent with Gradle's own dependency analysis tasks such as `dependencyInsight`.

## Configuration

All configuration is optional — the plugin works out of the box with no setup required.

### Kotlin DSL

```kotlin
dependencyConflictAnalyzer {
    failOnConflict.set(true)                                            // optional, default: false
    printToConsole.set(false)                                           // optional, default: true
    reportFile.set(layout.buildDirectory.file("reports/conflicts.md"))  // optional, type: RegularFile
    excludeCheckingLibrariesGroup.set(listOf("com.example.group"))      // optional
    excludeCheckingLibraries.set(listOf("com.example.group:artifact"))  // optional
}
```

### Groovy

```groovy
dependencyConflictAnalyzer {
    failOnConflict = true                                               // optional, default: false
    printToConsole = false                                              // optional, default: true
    reportFile = layout.buildDirectory.file("reports/conflicts.md")     // optional, type: RegularFile
    excludeCheckingLibrariesGroup = ["com.example.group"]               // optional
    excludeCheckingLibraries = ["com.example.group:artifact"]           // optional
}
```

<details>
<summary>All options</summary>

Everything below is optional.

| Option | Type | Default | Description |
|---|---|---|---|
| `failOnConflict` | `Boolean` | `false` | Fail the build when conflicts are detected |
| `printToConsole` | `Boolean` | `true` | Print the conflict report to the console |
| `reportFile` | `RegularFile` | `build/reports/dependency-conflict-analyzer/report.md` | Output path for the Markdown report |
| `excludeCheckingLibraries` | `List<String>` | `[]` | Artifacts to exclude (`"group:name"`) |
| `excludeCheckingLibrariesGroup` | `List<String>` | `[]` | Groups to exclude entirely |

</details>

## CI integration

The Markdown report can be surfaced in pull requests via [GitHub Actions job summary](https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/workflow-commands-for-github-actions#adding-a-job-summary) or posted as a PR comment using the [GitHub CLI](https://cli.github.com/manual/gh_pr_comment).

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

<details>
<summary>Console report</summary>

```
1 conflict(s) detected:
Version conflict: org.slf4j:slf4j-api (using 2.0.17)
- version 2.0.17 (in use) via:
     - :app -> ch.qos.logback:logback-classic:1.4.11 -> org.slf4j:slf4j-api:2.0.17
     - :app -> project :profile -> ch.qos.logback:logback-classic:1.5.32 -> org.slf4j:slf4j-api:2.0.17
- version 1.7.25 via:
     - :app -> org.apache.logging.log4j:log4j-slf4j-impl:2.17.1 -> org.slf4j:slf4j-api:1.7.25
```
Note: dependencies are used only as an example
</details>

<details>
<summary>Markdown report</summary>

```markdown
# Dependency Conflict Report

**1 conflict(s) detected:**

- [org.slf4j:slf4j-api](#slf4j-api) — using 2.0.17

---

<a id="slf4j-api"></a>
## `org.slf4j:slf4j-api` (using 2.0.17)

### Version 2.0.17 • in use

- :app -> ch.qos.logback:logback-classic:1.4.11 -> org.slf4j:slf4j-api:2.0.17
- :app -> project :profile -> ch.qos.logback:logback-classic:1.5.32 -> org.slf4j:slf4j-api:2.0.17

### Version 1.7.25

- :app -> org.apache.logging.log4j:log4j-slf4j-impl:2.17.1 -> org.slf4j:slf4j-api:1.7.25

---
```
Note: dependencies are used only as an example
</details>

For more context, see the [article on Medium](https://medium.com/@chdanilr/gradle-plugin-to-catch-version-conflicts-and-their-sources-early-bcc75f509766).