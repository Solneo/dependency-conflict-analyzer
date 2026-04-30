import inspector.DependencyInspectorService
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.util.GradleVersion
import reporting.FlowActionReportTrigger
import reporting.LegacyReportTrigger

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

