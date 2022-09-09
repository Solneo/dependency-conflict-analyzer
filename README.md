# Dependency Conflict Analyzer Gradle Plugin

This plugin scans dependencies
during gradle build. Throws an error if there is a possible dependency conflict. In the current version, the plugin
relies on major versions of artifacts. This may happen when
Uses the default Gradle dependency resolution strategy because it uses
artifact with the highest version.

## Usage in Groovy

On your `build.gradle` add:

```
apply plugin: 'io.github.solneo.dependency-conflict-analyzer'
```

In order to use this plugin, you will also need to add the following to your
buildscript classpath:

```
classpath 'com.dchernyaev.dca:dependency-conflict-analyzer:1.0.2'
```

## Usage in KTS

On your `build.gradle.kts` add:

```
plugins {
    ...
    id("io.github.solneo.dependency-conflict-analyzer")
}
```

In order to use this plugin, you will also need to add the following to your
buildscript classpath:

```
classpath("io.github.solneo:dependency-conflict-analyzer:1.0.2")
```

## Configuration

You can use `failOnConflict` extension for enable error in sync gradle:

#### In groovy:

```
dependencyConflictAnalyzer {
   failOnConflict = false
   ...
}
```

#### In KTS:

```
dependencyConflictAnalyzer {
   failOnConflict.set(false)
   ...
}
```

Also you can exclude artifact group or concrete library:

#### In groovy:

```
dependencyConflictAnalyzer {
   excludeCheckingLibrariesGroup = Arrays.asList("com.example.code.group")
   excludeCheckingLibraries = Arrays.asList("com.example.code.group:artifact")
   ...
}
```

#### In KTS:

```
dependencyConflictAnalyzer {
   excludeCheckingLibrariesGroup.set(listOf("com.example.code.group"))
   excludeCheckingLibrariesset(listOf("com.example.code.group:artifact"))
   ...
}
```

