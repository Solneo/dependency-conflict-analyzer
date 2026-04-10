import java.util.concurrent.ConcurrentHashMap
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentSelector
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DependencyInspector(private val extension: DependencyConflictAnalyzerExtension) :
    DependencyResolutionListener {

    val conflictSet = ConcurrentHashMap<String, DependencyBucket>()

    private val logger: Logger = LoggerFactory.getLogger(DependencyInspector::class.java)
    override fun beforeResolve(dependencies: ResolvableDependencies) {
        //don't need this method
    }

    override fun afterResolve(dependencies: ResolvableDependencies) {

        val buckets = mutableMapOf<String, DependencyBucket>()
        val excludeLibrariesGroupSet =
            extension.excludeCheckingLibrariesGroup.get().toSet()
        val excludeLibrariesSet = extension.excludeCheckingLibraries.get().toSet()
        val parents = buildParents(dependencies.resolutionResult.root)
        val pathCache = mutableMapOf<ComponentIdentifier, List<DependencySource>>()

        val directDependencies = dependencies.resolutionResult.root.dependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .filter { !it.isConstraint }
            .mapNotNull { it.requested as? ModuleComponentSelector }
            .map { "${it.group}:${it.module}" }
            .toSet()

        dependencies.resolutionResult.allDependencies.forEach { depResult ->
            when (val requested = depResult.requested) {
                is ModuleComponentSelector -> {
                    val group = requested.group
                    val name = requested.module
                    val version = requested.version
                    val key = "$group:$name"

                    if (version.isBlank()) return@forEach
                    if (IGNORED_ARTIFACTS.contains(key) ||
                        excludeLibrariesGroupSet.contains(group) ||
                        excludeLibrariesSet.contains(key)
                    ) return@forEach

                    val isFromRoot = depResult.from.id == dependencies.resolutionResult.root.id
                    val isConstraint =
                        (depResult as? ResolvedDependencyResult)?.isConstraint ?: false

                    if (isFromRoot) {
                        if (!directDependencies.contains(key) || isConstraint) return@forEach
                    } else {
                        if (isConstraint) return@forEach
                    }

                    val bucket = buckets.getOrPut(key) {
                        DependencyBucket(
                            group,
                            name,
                            requested = mutableMapOf(),
                            selected = version
                        )
                    }

                    val paths = pathCache.getOrPut(depResult.from.id) {
                        findPathsToRoot(depResult.from.id, parents)
                    }

                    bucket.requested
                        .getOrPut(version) { DependencyRequested(mutableSetOf()) }
                        .sources.addAll(paths)

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
                conflictSet.merge(conflict.key, bucket.value) { existing, new ->
                    new.requested.forEach { (version, requested) ->
                        existing.requested
                            .getOrPut(version) { DependencyRequested(mutableSetOf()) }
                            .sources.addAll(requested.sources)
                    }
                    existing
                }
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

    fun printConflicts() {
        logger.info("\n")
        if (conflictSet.isNotEmpty()) {
            logger.warn("--------- Warning! ---------")
        }
        conflictSet.values.forEach { bucket ->
            val conflict = extension.strategy.analyzeConflict(bucket)
            if (conflict.danger) println(conflict.msg)
        }
    }


    companion object {
        private const val MAX_PATHS = 3
        private const val MAX_DEPTH = 10
        val IGNORED_ARTIFACTS = setOf(
            "org.jetbrains:annotations",
            "org.jetbrains.kotlin:kotlin-stdlib",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8",
            "org.jetbrains.kotlin:kotlin-stdlib-common"
        )
    }

}
