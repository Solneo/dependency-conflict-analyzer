# Dependency Conflict Analyzer Gradle Plugin

This plugin scans dependencies
during gradle build. Throws an error if there is a possible dependency conflict. In the current version, the plugin
relies on major versions of artifacts. This may happen when
Uses the default Gradle dependency resolution strategy because it uses
artifact with the highest version.

[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/solneo/dependency-conflict-analyzer/blank.yml)](https://github.com/Solneo/dependency-conflict-analyzer/actions/workflows/blank.yml)
[![GitHub](https://img.shields.io/github/license/solneo/dependency-conflict-analyzer)](https://www.apache.org/licenses/LICENSE-2.0)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.solneo.dependency-conflict-analyzer)](https://plugins.gradle.org/plugin/io.github.solneo.dependency-conflict-analyzer)

## Usage in Groovy

### Using the plugins DSL

On your `build.gradle` add:

```groovy
plugins {
  id "io.github.solneo.dependency-conflict-analyzer" version "1.0.2"
}
```

### Using legacy plugin application

On your `build.gradle` add:

```groovy
apply plugin: 'io.github.solneo.dependency-conflict-analyzer'
```

In order to use this plugin, you will also need to add the following to your
buildscript classpath:

```groovy
classpath 'com.dchernyaev.dca:dependency-conflict-analyzer:1.0.2'
```

## Usage in KTS

### Using the plugins DSL

On your `build.gradle.kts` add:

```kotlin
plugins {
    id("io.github.solneo.dependency-conflict-analyzer") version "1.0.2"
}
```

### Using legacy plugin application

On your `build.gradle.kts` add:

```kotlin
apply(plugin = "io.github.solneo.dependency-conflict-analyzer")
```

In order to use this plugin, you will also need to add the following to your
buildscript classpath:

```kotlin
classpath("io.github.solneo:dependency-conflict-analyzer:1.0.2")
```

## Configuration

You can use `failOnConflict` extension for enable error in sync gradle:

#### In groovy:

```groovy
dependencyConflictAnalyzer {
   failOnConflict = false
}
```

#### In KTS:

```kotlin
dependencyConflictAnalyzer {
   failOnConflict.set(false)
}
```

Also you can exclude artifact group or concrete library:

#### In groovy:

```groovy
dependencyConflictAnalyzer {
   excludeCheckingLibrariesGroup = Arrays.asList("com.example.code.group")
   excludeCheckingLibraries = Arrays.asList("com.example.code.group:artifact")
}
```

#### In KTS:

```kotlin
dependencyConflictAnalyzer {
   excludeCheckingLibrariesGroup.set(listOf("com.example.code.group"))
   excludeCheckingLibrariesset(listOf("com.example.code.group:artifact"))
}
```

This displays a report to the console.


<details open>
<summary>Text Report</summary>

```
--------- Warning! ---------
Danger conflict with com.google.code.gson:gson between:
- version 1.7.1 from --- project :app
- version 2.8.9 from --- redis.clients:jedis:4.1.0
```
Note: dependencies used only fo log example
</details>
