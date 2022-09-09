import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import strategy.ConflictStrategy
import strategy.MajorVersionConflictStrategy

@Suppress("LeakingThis")
//check https://docs.gradle.org/current/userguide/custom_plugins.html#sec:getting_input_from_the_build
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