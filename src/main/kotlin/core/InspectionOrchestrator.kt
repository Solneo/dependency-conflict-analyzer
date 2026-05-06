package core

import analysis.ConflictAnalyzer
import graph.DependencyGraphStorage
import graph.ModuleKey
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.Provider
import reporting.format.Reporter
import java.util.concurrent.ConcurrentHashMap

internal class InspectionOrchestrator(
    private val storage: DependencyGraphStorage,
    private val analyzer: ConflictAnalyzer,
    private val excludeRules: ExcludeRules,
    private val failOnConflict: Provider<Boolean>,
    private val reporters: List<Reporter>,
) {

    fun onResolved(dependencies: ResolvableDependencies) {
        dependencies.resolutionResult.allDependencies.forEach { depResult ->
            if (depResult !is ResolvedDependencyResult) return@forEach
            if (depResult.isConstraint) return@forEach

            val fromId = depResult.from.id
            val toId = depResult.selected.id

            storage.parents.getOrPut(toId) { ConcurrentHashMap.newKeySet() }.add(fromId)

            val requested = depResult.requested as? ModuleComponentSelector ?: return@forEach
            val group = requested.group
            val name = requested.module
            val version = requested.version
            val key = ModuleKey(group, name)

            storage.edgeRequestedVersion[fromId to toId] = version

            if (version.isBlank()) return@forEach
            if (excludeRules.isExcluded(group, name)) return@forEach

            storage.requested
                .getOrPut(key) { ConcurrentHashMap() }
                .getOrPut(version) { ConcurrentHashMap.newKeySet() }
                .add(fromId)

            depResult.selected.moduleVersion?.version?.let { storage.selected[key] = it }
        }
    }

    fun runAndReport() {
        val report = analyzer.analyze(storage)
        reporters.forEach { it.report(report) }
        if (failOnConflict.get() && report.conflicts.isNotEmpty()) {
            throw GradleException(
                "Dependency conflicts detected. Use failOnConflict=false to suppress."
            )
        }
    }

    fun clear() = storage.clear()
}

internal data class ExcludeRules(
    private val libraries: Set<String>,
    private val groups: Set<String>,
) {
    fun isExcluded(group: String, name: String): Boolean {
        val coordinates = "$group:$name"
        return IGNORED_ARTIFACTS.contains(coordinates) ||
                groups.contains(group) ||
                libraries.contains(coordinates)
    }

    companion object {
        private val IGNORED_ARTIFACTS = setOf(
            "org.jetbrains:annotations",
            "org.jetbrains.kotlin:kotlin-stdlib",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8",
            "org.jetbrains.kotlin:kotlin-stdlib-common",
        )
    }
}