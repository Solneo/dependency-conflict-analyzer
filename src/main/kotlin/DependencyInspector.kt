import org.gradle.api.GradleException
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DependencyInspector(private val extension: DependencyConflictAnalyzerExtension) :
    DependencyResolutionListener {

    private val conflictSet = mutableSetOf<String>()

    private val logger: Logger = LoggerFactory.getLogger(DependencyInspector::class.java)
    override fun beforeResolve(dependencies: ResolvableDependencies) {
        //don't need this method
    }

    override fun afterResolve(dependencies: ResolvableDependencies) {
        val buckets = mutableMapOf<String, DependencyBucket>()
        val excludeLibrariesGroupSet =
            extension.excludeCheckingLibrariesGroup.get().toSet()
        val excludeLibrariesSet = extension.excludeCheckingLibraries.get().toSet()

        dependencies.resolutionResult.allDependencies.forEach { depResult ->
            when (val requested = depResult.requested) {
                is ModuleComponentSelector -> {
                    val group = requested.group
                    val name = requested.module
                    val version = requested.version

                    val key = "$group:$name".lowercase()

                    if (version.isBlank()) return@forEach
                    if (IGNORED_ARTIFACTS.contains(key) ||
                        excludeLibrariesGroupSet.contains(group) ||
                        excludeLibrariesSet.contains(key)
                    ) return@forEach

                    val bucket = buckets.getOrPut(key) {
                        DependencyBucket(
                            group,
                            name,
                            selected = version
                        )
                    }
                    val source = when (val id = depResult.from.id) {
                        is ProjectComponentIdentifier -> id.projectPath
                        is ModuleComponentIdentifier -> "${id.group}:${id.module}:${id.version}"
                        else -> id.displayName
                    }

                    bucket.requested
                        .getOrPut(version) { mutableSetOf() }
                        .add(source)
                    val selected = (depResult as? ResolvedDependencyResult)
                        ?.selected
                        ?.moduleVersion

                    if (selected != null) {
                        bucket.selected = selected.version
                    }
                }

                is ProjectComponentSelector -> {
                    val projectPath = requested.projectPath
                    //add later module dep problems
                }

                else -> {
                    // ignore
                }
            }
        }

        buckets.forEach { bucket ->
            val conflict = extension.strategy.analyzeConflict(bucket.value)
            if (conflict.danger) {
                conflictSet.add(conflict.msg)
            }
        }

        printConflicts()
        if (extension.failOnConflict.get()) {
            conflictSet.firstOrNull()
                ?.let { throw GradleException("$it\n you can ignore this issue with parameter failOnConflict") }
        }
        conflictSet.clear()
    }

    private fun printConflicts() {
        logger.info("\n")
        if (conflictSet.isNotEmpty()) {
            logger.error("--------- Warning! ---------")
        }
        conflictSet.forEach { logger.error(it) }
    }


    companion object {
        val IGNORED_ARTIFACTS = setOf(
            "org.jetbrains.kotlin:kotlin-stdlib",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8",
            "org.jetbrains.kotlin:kotlin-stdlib-common"
        )
    }

}

data class DependencyBucket(
    val group: String,
    val name: String,
    val requested: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    var selected: String
)
