package reporting.trigger

import gradle.DependencyInspectorService
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference

internal abstract class PrintConflictsFlowAction : FlowAction<PrintConflictsFlowAction.Params> {
    interface Params : FlowParameters {
        @get:ServiceReference
        val inspector: Property<DependencyInspectorService>
    }

    override fun execute(parameters: Params) {
        val inspector = parameters.inspector.get()
        inspector.runAndReport()
        inspector.clear()
    }
}