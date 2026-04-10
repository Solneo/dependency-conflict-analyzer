import java.util.concurrent.ConcurrentHashMap
import kotlin.text.contains
import org.gradle.api.GradleException
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentSelector
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DependencyInspector(private val extension: DependencyConflictAnalyzerExtension) :
    DependencyResolutionListener {

    val conflictSet = ConcurrentHashMap<String, DependencyBucket>()
    private val globalParents =
        ConcurrentHashMap<ComponentIdentifier, MutableSet<ComponentIdentifier>>()
    private val globalRequested =
        ConcurrentHashMap<String, ConcurrentHashMap<String, MutableSet<ComponentIdentifier>>>()
    private val globalSelected = ConcurrentHashMap<String, String>()
    private val pathCache = ConcurrentHashMap<ComponentIdentifier, List<DependencySource>>()

    private val logger: Logger = LoggerFactory.getLogger(DependencyInspector::class.java)
    override fun beforeResolve(dependencies: ResolvableDependencies) {
        //don't need this method
    }

    override fun afterResolve(dependencies: ResolvableDependencies) {
        val excludeLibrariesGroupSet = extension.excludeCheckingLibrariesGroup.get().toSet()
        val excludeLibrariesSet = extension.excludeCheckingLibraries.get().toSet()

        val directDependencies = dependencies.resolutionResult.root.dependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .filter { !it.isConstraint }
            .mapNotNull { it.requested as? ModuleComponentSelector }
            .map { "${it.group}:${it.module}" }
            .toSet()

        dependencies.resolutionResult.allDependencies.forEach { depResult ->
            if (depResult !is ResolvedDependencyResult) return@forEach
            if (depResult.isConstraint) return@forEach

            val fromId = depResult.from.id
            val toId = depResult.selected.id

            globalParents
                .getOrPut(toId) { ConcurrentHashMap.newKeySet() }
                .add(fromId)

            val requested = depResult.requested as? ModuleComponentSelector ?: return@forEach
            val group = requested.group
            val name = requested.module
            val version = requested.version
            val key = "$group:$name"

            if (version.isBlank()) return@forEach
            if (IGNORED_ARTIFACTS.contains(key) ||
                excludeLibrariesGroupSet.contains(group) ||
                excludeLibrariesSet.contains(key)
            ) return@forEach

            val isFromRootDebug = depResult.from.id == dependencies.resolutionResult.root.id
            if (isFromRootDebug) {
                val req = depResult.requested as? ModuleComponentSelector
                println("ALL FROM ROOT [${dependencies.resolutionResult.root.id.displayName}]: ${req?.group}:${req?.module}:${req?.version} isConstraint=${depResult.isConstraint}")
            }

            val isFromRoot = fromId == dependencies.resolutionResult.root.id
            if (isFromRoot && (!directDependencies.contains(key) || depResult.isConstraint)) return@forEach

            globalRequested
                .getOrPut(key) { ConcurrentHashMap() }
                .getOrPut(version) { ConcurrentHashMap.newKeySet() }
                .add(fromId)

            depResult.selected.moduleVersion?.version?.let { selected ->
                globalSelected[key] = selected
            }
        }
    }

    fun findPaths(
        node: ResolvedComponentResult,
        target: String,
        path: MutableList<String>,
        result: MutableList<List<String>>,
        visited: MutableSet<ComponentIdentifier>
    ) {
        val current = node.moduleVersion?.let {
            "${it.group}:${it.name}:${it.version}"
        } ?: node.id.displayName

        path.add(current)

        val key = node.moduleVersion?.let {
            "${it.group}:${it.name}"
        }

        if (key == target) {
            result.add(path.toList())
            path.removeAt(path.lastIndex)
            visited.remove(node.id)
            return
        }

        node.dependencies.forEach { dep ->
            if (dep is ResolvedDependencyResult) {
                findPaths(dep.selected, target, path, result, visited)
            }
        }

        path.removeAt(path.lastIndex)
        visited.remove(node.id)
    }

    fun findPathsToRoot(
        target: ComponentIdentifier,
        parents: Map<ComponentIdentifier, List<ComponentIdentifier>>
    ): List<DependencySource> {

        val result = mutableListOf<DependencySource>()

        val queue: ArrayDeque<List<ComponentIdentifier>> = ArrayDeque()
        queue.add(listOf(target))

        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()

            if (result.size >= MAX_PATHS) break

            if (path.size > MAX_DEPTH) continue

            val current = path.last()

            val parentList = parents[current]

            if (parentList.isNullOrEmpty()) {
                result.add(DependencySource(path.reversed()))
                continue
            }

            for (parent in parentList) {
                if (parent in path) continue

                val newPath = ArrayList<ComponentIdentifier>(path.size + 1)
                newPath.addAll(path)
                newPath.add(parent)

                queue.add(newPath)
            }
        }

        return result
    }

    fun buildParents(
        root: ResolvedComponentResult
    ): Map<ComponentIdentifier, List<ComponentIdentifier>> {

        val parents = mutableMapOf<ComponentIdentifier, MutableList<ComponentIdentifier>>()
        val visitedEdges = mutableSetOf<Pair<ComponentIdentifier, ComponentIdentifier>>()

        fun traverse(node: ResolvedComponentResult) {
            node.dependencies.forEach { dep ->
                if (dep is ResolvedDependencyResult && !dep.isConstraint) {
                    val child = dep.selected.id
                    val parent = node.id

                    if (visitedEdges.add(child to parent)) {
                        parents.getOrPut(child) { mutableListOf() }.add(parent)
                        traverse(dep.selected)
                    }
                }
            }
        }

        traverse(root)
        return parents
    }

    fun normalizeSources(sources: Set<DependencySource>): Set<DependencySource> {
        return sources
            .groupBy { source ->
                normalizePath(source.pathList)
            }
            .values
            .map { paths ->
                paths.maxWithOrNull(
                    compareBy<DependencySource> { normalizePath(it.pathList).size }
                        .thenBy { it.pathList.size }
                )!!
            }
            .toSet()
    }

    fun normalizePath(path: List<ComponentIdentifier>): List<String> {
        return path.mapNotNull { id ->
            val mv = (id as? ModuleComponentIdentifier)?.let {
                "${it.group}:${it.module}"
            }
            mv
        }
    }

    fun removeWeakerPaths(sources: Set<DependencySource>): Set<DependencySource> {
        val list = sources.toList()

        return list.filterNot { candidate ->
            list.any { other ->
                if (candidate === other) return@any false

                val c = candidate.pathList
                val o = other.pathList

                if (o.size <= c.size) return@any false

                // разные root
                val differentRoot = o.first() != c.first()
                if (!differentRoot) return@any false

                // candidate является суффиксом other
                val offset = o.size - c.size
                o.subList(offset, o.size) == c
            }
        }.toSet()
    }

    fun analyzeAndPrint() {
        // строим parents как Map<ComponentIdentifier, List<ComponentIdentifier>>
        val parents = globalParents.mapValues { it.value.toList() }

        val pathCache = mutableMapOf<ComponentIdentifier, List<DependencySource>>()

        val buckets = globalRequested.map { (key, versionMap) ->
            val (group, name) = key.split(":")
            val requested = versionMap.entries.associate { (version, requesters) ->
                val sources = requesters.flatMap { requester ->
                    pathCache.getOrPut(requester) {
                        findPathsToRoot(requester, parents)
                    }
                }.toMutableSet()
                version to DependencyRequested(sources)
            }.toMutableMap()

            DependencyBucket(group, name, requested, globalSelected[key] ?: "")
        }

        buckets.forEach { bucket ->
            val conflict = extension.strategy.analyzeConflict(bucket)
            if (conflict.danger) println(conflict.msg)
        }
    }

    fun printConflicts() {
        val parents = globalParents.mapValues { it.value.toList() }

        globalRequested.forEach { (key, versionMap) ->
            val parts = key.split(":")
            val group = parts[0]
            val name = parts[1]

            val requested = versionMap.entries.associate { (version, requesters) ->
                val sources = requesters.flatMap { requester ->
                    pathCache.getOrPut(requester) {
                        findPathsToRoot(requester, parents)
                    }
                }.toMutableSet()
                version to DependencyRequested(sources)
            }.toMutableMap()

            val bucket = DependencyBucket(group, name, requested, globalSelected[key] ?: "")
            val conflict = extension.strategy.analyzeConflict(bucket)
            if (conflict.danger) {
                logger.warn(conflict.msg)
                if (extension.failOnConflict.get()) {
                    throw GradleException("${conflict.msg}\n you can ignore this issue with parameter failOnConflict")
                }
            }
        }
    }

    fun clear() {
        globalParents.clear()
        globalRequested.clear()
        globalSelected.clear()
        pathCache.clear()
    }


    companion object {
        private const val MAX_PATHS = 10
        private const val MAX_DEPTH = 15
        val IGNORED_ARTIFACTS = setOf(
            "org.jetbrains:annotations",
            "org.jetbrains.kotlin:kotlin-stdlib",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8",
            "org.jetbrains.kotlin:kotlin-stdlib-common"
        )
    }

}