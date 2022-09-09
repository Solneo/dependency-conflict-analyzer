package strategy

import org.gradle.api.artifacts.result.DependencyResult

interface ConflictStrategy {
    fun analyzeConflict(depResult: DependencyResult): AnalyzedConflict
}