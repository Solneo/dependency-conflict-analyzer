package reporting.format

import analysis.Conflict
import analysis.ConflictReport
import org.slf4j.Logger

internal class ConsoleReporter(private val logger: Logger) : Reporter {

    override fun report(report: ConflictReport) {
        report.conflicts.forEach { renderConflict(it) }
    }

    private fun renderConflict(conflict: Conflict) {
        val text = buildString {
            appendLine("Version conflict detected: ${conflict.group}:${conflict.name}")
            conflict.requestedVersions.forEach { requested ->
                appendLine("- version ${requested.version} via:")
                requested.dependencyPaths.forEach { path ->
                    append("     - ")
                    path.nodes.forEachIndexed { index, node ->
                        if (index > 0) append(" -> ")
                        append(node.displayName)
                        if (node.requestedVersion != null) append(":${node.requestedVersion}")
                    }
                    append(" -> ${conflict.group}:${conflict.name}:${requested.version}")
                    appendLine()
                }
            }
            append("→ using ${conflict.selectedVersion}\n")
        }
        logger.warn(text)
    }
}