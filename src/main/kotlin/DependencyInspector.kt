import org.gradle.api.GradleException
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
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
        dependencies.resolutionResult.allDependencies.forEach { depResult ->
            val excludeLibrariesGroupSet = extension.excludeCheckingLibrariesGroup.get().toSet()
            val excludeLibrariesSet = extension.excludeCheckingLibraries.get().toSet()
            val module = depResult.requested.displayName.split(":")
            if (!excludeLibrariesGroupSet.contains(module[0]) && !excludeLibrariesSet.contains(
                    module[0] + ":" + module[1]
                )
            ) {
                val conflict = extension.strategy.analyzeConflict(depResult)
                if (conflict.danger) {
                    conflictSet.add(conflict.msg)
                }
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

}
