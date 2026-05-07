package reporting.trigger

import gradle.DependencyInspectorService
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.flow.FlowActionSpec
import org.gradle.api.flow.FlowScope
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.provider.Provider

internal object FlowActionReportTrigger {
    fun register(
        project: Project,
        inspectorProvider: Provider<DependencyInspectorService>,
    ) {
        val flowScope = (project as ProjectInternal).services.get(FlowScope::class.java)
        val configure = object : Action<FlowActionSpec<PrintConflictsFlowAction.Params>> {
            override fun execute(spec: FlowActionSpec<PrintConflictsFlowAction.Params>) {
                spec.parameters.inspector.set(inspectorProvider)
            }
        }

        flowScope.always(PrintConflictsFlowAction::class.java, configure)
    }
}