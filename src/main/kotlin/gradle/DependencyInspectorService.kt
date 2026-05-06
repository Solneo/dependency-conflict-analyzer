package gradle

import analysis.ConflictAnalyzer
import core.ExcludeRules
import core.InspectionOrchestrator
import graph.DependencyGraphStorage
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.slf4j.LoggerFactory
import reporting.format.ConsoleReporter
import strategy.MajorVersionConflictStrategy

abstract class DependencyInspectorService :
    BuildService<DependencyInspectorService.Params> {

    interface Params : BuildServiceParameters {
        val failOnConflict: Property<Boolean>
        val excludeCheckingLibraries: ListProperty<String>
        val excludeCheckingLibrariesGroup: ListProperty<String>
    }

    private val orchestrator: InspectionOrchestrator by lazy {
        InspectionOrchestrator(
            storage = DependencyGraphStorage(),
            analyzer = ConflictAnalyzer(MajorVersionConflictStrategy()),
            excludeRules = ExcludeRules(
                libraries = parameters.excludeCheckingLibraries.get().toSet(),
                groups = parameters.excludeCheckingLibrariesGroup.get().toSet(),
            ),
            failOnConflict = parameters.failOnConflict,
            reporters = listOf(ConsoleReporter(LoggerFactory.getLogger(DependencyInspectorService::class.java))),
        )
    }

    fun afterResolve(dependencies: ResolvableDependencies) = orchestrator.onResolved(dependencies)

    fun runAndReport() = orchestrator.runAndReport()

    fun clear() = orchestrator.clear()
}