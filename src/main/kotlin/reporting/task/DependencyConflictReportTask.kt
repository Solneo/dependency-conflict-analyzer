package reporting.task

import gradle.DependencyInspectorService
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import reporting.format.MarkdownReporter

abstract class DependencyConflictReportTask : DefaultTask() {

    @get:Internal
    abstract val outputFile: RegularFileProperty

    @get:ServiceReference
    abstract val inspector: Property<DependencyInspectorService>

    init {
        notCompatibleWithConfigurationCache("Resolves project configurations at execution time")
    }

    @TaskAction
    fun generate() {
        project.rootProject.allprojects {
            configurations
                .matching {
                    it.name.endsWith("CompileClasspath", ignoreCase = true) &&
                            !it.name.contains("Test") &&
                            !it.name.contains("AndroidTest")
                }
                .filter { it.isCanBeResolved }
                .forEach { configuration ->
                    runCatching { configuration.incoming.resolutionResult.allComponents }
                }
        }
        inspector.get().addReporter(MarkdownReporter(outputFile.get().asFile))
    }
}