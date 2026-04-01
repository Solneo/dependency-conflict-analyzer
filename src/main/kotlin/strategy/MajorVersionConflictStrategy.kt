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
            append("Danger conflict with ${bucket.group}:${bucket.name} between:\n")

            bucket.requested.forEach { (version, sources) ->
                sources.forEach { source ->
                    append("- version $version from --- $source\n")
                }
            }

            append("→ using ${bucket.selected}\n")
        }

        return AnalyzedConflict(true, msg)
    }

}