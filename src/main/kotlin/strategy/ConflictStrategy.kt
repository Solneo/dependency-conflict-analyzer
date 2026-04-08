package strategy

import DependencyBucket

interface ConflictStrategy {
    fun analyzeConflict(depResult: DependencyBucket): AnalyzedConflict
}