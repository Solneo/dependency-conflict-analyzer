import java.util.concurrent.ConcurrentHashMap
import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.kotlin.dsl.create

class DependencyConflictAnalyzer : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create<DependencyConflictAnalyzerExtension>("dependencyConflictAnalyzer")

        val dependencyInspector = DependencyInspector(extension)

        project.rootProject.allprojects {
            println("${this.displayName}")
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
                dependencyInspector.clear()
            }
        })
    }
}

