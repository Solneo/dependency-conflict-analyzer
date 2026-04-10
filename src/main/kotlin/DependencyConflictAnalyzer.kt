import java.util.concurrent.ConcurrentHashMap
import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.kotlin.dsl.create

class DependencyConflictAnalyzer : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create<DependencyConflictAnalyzerExtension>("dependencyConflictAnalyzer")

        val dependencyInspector = DependencyInspector(extension)

        project.rootProject.allprojects {
            val subproject = this
            configurations
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

        project.gradle.addBuildListener(object : BuildAdapter() {
            override fun buildFinished(result: BuildResult) {
                dependencyInspector.printConflicts()
                if (extension.failOnConflict.get()) {
                    dependencyInspector.conflictSet.values.firstOrNull()
                        ?.let { throw GradleException("$it\n you can ignore this issue with parameter failOnConflict") }
                }

                dependencyInspector.conflictSet.clear()
            }
        })
    }
}

