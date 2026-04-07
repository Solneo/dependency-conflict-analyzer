import org.gradle.api.artifacts.component.ComponentIdentifier

data class DependencyBucket(
    val group: String,
    val name: String,
    val requested: MutableMap<String, DependencyRequested>,
    var selected: String
)

data class DependencyRequested(
    val sources: MutableSet<DependencySource>
)

data class DependencySource(
    val pathList: List<ComponentIdentifier>
)