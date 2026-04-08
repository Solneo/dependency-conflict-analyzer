import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class DependencyConflictAnalyzer : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create<DependencyConflictAnalyzerExtension>("dependencyConflictAnalyzer")

        val dependencyInspector = DependencyInspector(extension)

        project.configurations
            .matching {
                it.name.endsWith("CompileClasspath") &&
                        !it.name.contains("Test") &&
                        !it.name.contains("AndroidTest")
            }
            .all {
                if (isCanBeResolved) {
                    incoming.afterResolve(dependencyInspector::afterResolve)
                }
            }
    }
}
