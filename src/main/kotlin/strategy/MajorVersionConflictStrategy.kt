package strategy

import DependencyBucket
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MajorVersionConflictStrategy : ConflictStrategy {

    private val logger: Logger = LoggerFactory.getLogger(MajorVersionConflictStrategy::class.java)
    private val tempConflictHashMap = HashMap<String, DependencyNode>()

    override fun analyzeConflict(bucket: DependencyBucket): AnalyzedConflict {

        val uniqueVersions = bucket.requested.keys
            .filter { it.isNotBlank() }
            .toSet()

        if (uniqueVersions.size <= 1) {
            return AnalyzedConflict()
        }

        val majors = bucket.requested.keys
            .mapNotNull { ver -> ver.firstOrNull { it.isDigit() } }
            .toSet()

        if (majors.size <= 1) {
            return AnalyzedConflict()
        }

        val msg = buildString {
            append("Version conflict detected: ${bucket.group}:${bucket.name}\n")

            bucket.requested.forEach { (version, dependencyRequested) ->
                append("- version $version via:\n")
                dependencyRequested.sources.forEach { source ->
                    append("     - ")
                    source.pathList.forEach { pathElement ->
                        append("${pathElement.displayName} -> ")
                    }
                    append("${bucket.group}:${bucket.name}:$version")
                    append("\n")
                }
            }

            append("→ using ${bucket.selected}\n")
        }

        return AnalyzedConflict(true, msg)
    }

}