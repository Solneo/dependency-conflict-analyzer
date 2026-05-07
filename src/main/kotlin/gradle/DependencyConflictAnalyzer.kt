package gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.util.GradleVersion
import reporting.task.DependencyConflictReportTask
import reporting.trigger.FlowActionReportTrigger
import reporting.trigger.LegacyReportTrigger

class DependencyConflictAnalyzer : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create<DependencyConflictAnalyzerExtension>("dependencyConflictAnalyzer")

        val inspectorProvider = project.gradle.sharedServices.registerIfAbsent(
            "dependencyInspector",
            DependencyInspectorService::class.java,
        ) {
            parameters.failOnConflict.set(extension.failOnConflict)
            parameters.excludeCheckingLibraries.set(extension.excludeCheckingLibraries)
            parameters.excludeCheckingLibrariesGroup.set(extension.excludeCheckingLibrariesGroup)
            parameters.printToConsole.set(extension.printToConsole)
        }

        project.tasks.register(
            "generateDependencyConflictReport",
            DependencyConflictReportTask::class.java,
        ) {
            inspector.set(inspectorProvider)
            outputFile.convention(
                extension.reportFile.orElse(
                    project.rootProject.layout.buildDirectory.file("reports/dependency-conflict-analyzer/report.md")
                )
            )
        }

        project.rootProject.allprojects {
            configurations
                .matching {
                    it.name.endsWith("CompileClasspath", ignoreCase = true) &&
                            !it.name.contains("Test") &&
                            !it.name.contains("AndroidTest")
                }
                .all {
                    if (isCanBeResolved) {
                        val service = inspectorProvider.get()
                        incoming.afterResolve(service::afterResolve)
                    }
                }
        }

        if (GradleVersion.current() >= GradleVersion.version("8.1")) {
            FlowActionReportTrigger.register(project, inspectorProvider)
        } else {
            LegacyReportTrigger.register(project, inspectorProvider)
        }
    }
}