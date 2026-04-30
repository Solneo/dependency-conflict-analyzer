package strategy

import inspector.DependencyBucket

interface ConflictStrategy {
    fun analyzeConflict(depResult: DependencyBucket): AnalyzedConflict
}