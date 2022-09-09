import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class DependencyConflictAnalyzer : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create<DependencyConflictAnalyzerExtension>("dependencyConflictAnalyzer")

        project.task("setDependencyInspector") {
            group = "dependencies"
            val dependencyInspector = DependencyInspector(extension)

            project.configurations.all {
                if (name.contains("ompile")) {
                    incoming.afterResolve(dependencyInspector::afterResolve)
                }
            }
        }
    }
}
