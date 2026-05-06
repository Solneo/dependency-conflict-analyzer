package analysis

internal data class ConflictReport(
    val conflicts: List<Conflict>,
)

internal data class Conflict(
    val group: String,
    val name: String,
    val selectedVersion: String,
    val requestedVersions: List<RequestedVersion>,
)

internal data class RequestedVersion(
    val version: String,
    val dependencyPaths: List<DependencyPath>,
)

internal data class DependencyPath(
    val nodes: List<PathNode>,
)

internal data class PathNode(
    val displayName: String,
    val requestedVersion: String?,
)