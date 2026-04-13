package strategy

import DependencyBucket
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MajorVersionConflictStrategy : ConflictStrategy {

    private val logger: Logger = LoggerFactory.getLogger(MajorVersionConflictStrategy::class.java)

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
                    source.path.forEachIndexed { index, pathElement ->
                        if (index == 0) {
                            append("${pathElement.displayName} -> ")
                        } else {
                            val requested = source.requestedVersions[pathElement]
                            if (requested != null) {
                                append("${formatId(pathElement)}:$requested -> ")
                            } else {
                                append("${pathElement.displayName} -> ")
                            }
                        }
                    }
                    append("${bucket.group}:${bucket.name}:$version")
                    append("\n")
                }
            }
            append("→ using ${bucket.selected}\n")
        }

        return AnalyzedConflict(true, msg)
    }

    fun formatId(id: ComponentIdentifier): String {
        return when (id) {
            is ModuleComponentIdentifier -> "${id.group}:${id.module}"
            else -> id.displayName
        }
    }

}