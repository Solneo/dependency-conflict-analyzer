package graph

import org.gradle.api.artifacts.component.ComponentIdentifier

internal data class DependencyBucket(
    val group: String,
    val name: String,
    val requested: MutableMap<String, DependencyRequested>,
    var selected: String
)

internal data class DependencyRequested(
    val sources: MutableSet<DependencySource>
)

internal data class DependencySource(
    val path: List<ComponentIdentifier>,
    val requestedVersions: Map<ComponentIdentifier, String> = emptyMap()
)