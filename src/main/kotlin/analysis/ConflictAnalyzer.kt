package analysis

import graph.DependencyBucket
import graph.DependencyGraphStorage
import graph.DependencyRequested
import graph.DependencySource
import org.gradle.api.artifacts.component.ComponentIdentifier
import strategy.ConflictStrategy

internal class ConflictAnalyzer(private val strategy: ConflictStrategy) {

    fun analyze(storage: DependencyGraphStorage): ConflictReport {
        val parents = storage.parents.mapValues { it.value.toList() }

        val buckets = storage.requested.map { (key, versionMap) ->
            val requested = versionMap.entries.associate { (version, requesters) ->
                val sources = requesters.flatMap { id ->
                    storage.pathCache.computeIfAbsent(id) {
                        findPathsToRoot(id, parents, storage.edgeRequestedVersion)
                    }
                }.toMutableSet()
                version to DependencyRequested(sources)
            }.toMutableMap()

            DependencyBucket(
                group = key.group,
                name = key.name,
                requested = requested,
                selected = storage.selected[key] ?: "",
            )
        }

        return ConflictReport(conflicts = strategy.findConflicts(buckets))
    }

    internal fun findPathsToRoot(
        target: ComponentIdentifier,
        parents: Map<ComponentIdentifier, List<ComponentIdentifier>>,
        edgeRequestedVersion: Map<Pair<ComponentIdentifier, ComponentIdentifier>, String>,
    ): List<DependencySource> {
        val result = mutableListOf<DependencySource>()
        val queue = ArrayDeque<List<ComponentIdentifier>>()
        queue.add(listOf(target))

        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            if (result.size >= MAX_PATHS) break
            if (path.size > MAX_DEPTH) continue

            val current = path.last()
            val parentList = parents[current]

            if (parentList.isNullOrEmpty()) {
                val reversed = path.reversed()
                val versions = buildMap {
                    for (i in 0 until reversed.size - 1) {
                        edgeRequestedVersion[reversed[i] to reversed[i + 1]]
                            ?.let { put(reversed[i + 1], it) }
                    }
                }
                result.add(DependencySource(reversed, versions))
                continue
            }

            for (parent in parentList) {
                if (parent in path) continue
                queue.add(path + parent)
            }
        }

        return result
    }

    companion object {
        internal const val MAX_PATHS = 10
        internal const val MAX_DEPTH = 15
    }
}