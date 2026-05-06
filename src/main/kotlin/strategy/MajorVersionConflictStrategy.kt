package strategy

import analysis.Conflict
import analysis.DependencyPath
import analysis.PathNode
import analysis.RequestedVersion
import graph.DependencyBucket
import org.gradle.api.artifacts.component.ModuleComponentIdentifier

internal class MajorVersionConflictStrategy : ConflictStrategy {

    override fun findConflicts(buckets: List<DependencyBucket>): List<Conflict> =
        buckets.mapNotNull { bucket -> toConflict(bucket) }

    private fun toConflict(bucket: DependencyBucket): Conflict? {
        val versions = bucket.requested.keys.filter { it.isNotBlank() }.toSet()
        if (versions.size <= 1) return null

        val majors = versions.mapNotNull { it.firstOrNull { c -> c.isDigit() } }.toSet()
        if (majors.size <= 1) return null

        return Conflict(
            group = bucket.group,
            name = bucket.name,
            selectedVersion = bucket.selected,
            requestedVersions = bucket.requested.filter { (version, _) -> version.isNotBlank() }.map { (version, requested) ->
                RequestedVersion(
                    version = version,
                    dependencyPaths = requested.sources.map { source ->
                        DependencyPath(
                            nodes = source.path.mapIndexed { index, id ->
                                PathNode(
                                    displayName = if (id is ModuleComponentIdentifier) {
                                        "${id.group}:${id.module}"
                                    } else {
                                        id.displayName
                                    },
                                    requestedVersion = if (index > 0) {
                                        source.requestedVersions[id]
                                    } else {
                                        null
                                    },
                                )
                            },
                        )
                    },
                )
            },
        )
    }
}