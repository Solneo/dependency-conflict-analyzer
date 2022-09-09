# Dependency Conflict Analyzer Gradle Plugin

This plugin scans dependencies
during gradle build. Throws an error if there is a possible dependency conflict. In the current version, the plugin
relies on major versions of artifacts. This may happen when
Uses the default Gradle dependency resolution strategy because it uses
artifact with the highest version.

## To use

In your app's build.gradle:

```
apply plugin: 'io.github.solneo.dependency-conflict-analyzer'
```

In order to use this plugin, you will also need to add the following to your
buildscript classpath:

```
classpath 'com.dchernyaev.dca:dependency-conflict-analyzer:1.0.2'
```

## Configuration

You can use failOnConflict extension for enable error in sync gradle:

```
dependencyConflictAnalyzer {
   failOnConflict = false
   ...
}
```
also you can exclude artifact group or concrete library

```
dependencyConflictAnalyzer {
   excludeCheckingLibrariesGroup = Arrays.asList("com.example.code.group")
   excludeCheckingLibraries = Arrays.asList("com.example.code.group:artifact")
   ...
}
```
