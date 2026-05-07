package strategy

import analysis.Conflict
import graph.DependencyBucket

internal interface ConflictStrategy {
    fun findConflicts(buckets: List<DependencyBucket>): List<Conflict>
}