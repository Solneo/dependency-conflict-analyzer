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

data class DependencySource(val pathList: List<ComponentIdentifier>) {
    override fun equals(other: Any?): Boolean {
        if (other !is DependencySource) return false
        return pathList.map { it.displayName } == other.pathList.map { it.displayName }
    }

    override fun hashCode(): Int {
        return pathList.map { it.displayName }.hashCode()
    }
}