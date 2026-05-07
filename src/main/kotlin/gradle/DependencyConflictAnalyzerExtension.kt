package gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class DependencyConflictAnalyzerExtension {
    abstract val failOnConflict: Property<Boolean>
    abstract val excludeCheckingLibraries: ListProperty<String>
    abstract val excludeCheckingLibrariesGroup: ListProperty<String>
    abstract val printToConsole: Property<Boolean>
    abstract val reportFile: RegularFileProperty

    init {
        failOnConflict.convention(false)
        excludeCheckingLibraries.convention(emptyList())
        excludeCheckingLibrariesGroup.convention(emptyList())
        printToConsole.convention(true)
    }
}