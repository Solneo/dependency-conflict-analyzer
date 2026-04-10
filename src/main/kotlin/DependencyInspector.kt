import java.util.concurrent.ConcurrentHashMap
import org.gradle.api.GradleException
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DependencyInspector(private val extension: DependencyConflictAnalyzerExtension) :
    DependencyResolutionListener {

    private val edgeRequestedVersion =
        ConcurrentHashMap<Pair<ComponentIdentifier, ComponentIdentifier>, String>()

    private val globalParents =
        ConcurrentHashMap<ComponentIdentifier, MutableSet<ComponentIdentifier>>()
    private val globalRequested =
        ConcurrentHashMap<String, ConcurrentHashMap<String, MutableSet<Requester>>>()
    private val globalSelected = ConcurrentHashMap<String, String>()
    private val pathCache = ConcurrentHashMap<ComponentIdentifier, List<DependencySource>>()

    private val logger: Logger = LoggerFactory.getLogger(DependencyInspector::class.java)
    override fun beforeResolve(dependencies: ResolvableDependencies) {
        //don't need this method
    }

    override fun afterResolve(dependencies: ResolvableDependencies) {
        val excludeLibrariesGroupSet = extension.excludeCheckingLibrariesGroup.get().toSet()
        val excludeLibrariesSet = extension.excludeCheckingLibraries.get().toSet()
        val rootId = dependencies.resolutionResult.root.id

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

            edgeRequestedVersion[fromId to toId] = version


            if (version.isBlank()) return@forEach
            if (IGNORED_ARTIFACTS.contains(key) ||
                excludeLibrariesGroupSet.contains(group) ||
                excludeLibrariesSet.contains(key)
            ) return@forEach
            val isDirect = fromId == rootId

            val requester = Requester(
                id = fromId,
                root = rootId,
                isDirect = isDirect
            )

            globalRequested
                .getOrPut(key) { ConcurrentHashMap() }
                .getOrPut(version) { ConcurrentHashMap.newKeySet() }
                .add(requester)

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
                val reversed = path.reversed()

                val versions = mutableMapOf<ComponentIdentifier, String>()

                for (i in 0 until reversed.size - 1) {
                    val from = reversed[i]
                    val to = reversed[i + 1]

                    val requested = edgeRequestedVersion[from to to]
                    if (requested != null) {
                        versions[to] = requested
                    }
                }

                result.add(DependencySource(reversed, versions))
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

    fun printConflicts() {
        globalRequested.forEach { (key, versionMap) ->
            versionMap.forEach { (version, requesters) ->
                requesters.forEach {
                    if (it.isDirect) {
                        println("DIRECT FOUND: $key:$version from ${it.root.displayName}")
                    }
                }
            }
        }


        val parents = globalParents.mapValues { it.value.toList() }

        globalRequested.forEach { (key, versionMap) ->
            val parts = key.split(":")
            val group = parts[0]
            val name = parts[1]


            val requested = versionMap.entries.associate { (version, requesters) ->
                val sources = requesters.flatMap { requester ->
                    pathCache.getOrPut(requester.id) {
                        findPathsToRoot(requester.id, parents)
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

data class Requester(
    val id: ComponentIdentifier,
    val root: ComponentIdentifier,
    val isDirect: Boolean
)
