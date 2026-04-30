import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import strategy.ConflictStrategy
import strategy.MajorVersionConflictStrategy

abstract class DependencyConflictAnalyzerExtension {
    abstract val failOnConflict: Property<Boolean>
    val strategy: ConflictStrategy = MajorVersionConflictStrategy()
    abstract val excludeCheckingLibraries: ListProperty<String>
    abstract val excludeCheckingLibrariesGroup: ListProperty<String>

    init {
        excludeCheckingLibraries.convention(emptyList())
        excludeCheckingLibrariesGroup.convention(emptyList())
        failOnConflict.convention(false)
    }
}