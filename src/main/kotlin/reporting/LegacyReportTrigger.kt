package reporting

import inspector.DependencyInspectorService
import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.provider.Provider

internal object LegacyReportTrigger {
    fun register(
        project: Project,
        inspectorProvider: Provider<DependencyInspectorService>,
    ) {
        project.gradle.addBuildListener(object : BuildAdapter() {
            override fun buildFinished(result: BuildResult) {
                val inspector = inspectorProvider.get()
                inspector.printConflicts()
                inspector.clear()
            }
        })
    }
}