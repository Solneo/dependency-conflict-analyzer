package graph

import org.gradle.api.artifacts.component.ComponentIdentifier
import java.util.concurrent.ConcurrentHashMap

internal class DependencyGraphStorage {

    val parents = ConcurrentHashMap<ComponentIdentifier, MutableSet<ComponentIdentifier>>()
    val edgeRequestedVersion = ConcurrentHashMap<Pair<ComponentIdentifier, ComponentIdentifier>, String>()
    val requested = ConcurrentHashMap<ModuleKey, ConcurrentHashMap<String, MutableSet<ComponentIdentifier>>>()
    val selected = ConcurrentHashMap<ModuleKey, String>()
    val pathCache = ConcurrentHashMap<ComponentIdentifier, List<DependencySource>>()

    fun clear() {
        parents.clear()
        edgeRequestedVersion.clear()
        requested.clear()
        selected.clear()
        pathCache.clear()
    }
}