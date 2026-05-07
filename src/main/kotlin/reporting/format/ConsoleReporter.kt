package reporting.format

import analysis.ConflictReport
import org.slf4j.Logger

internal class ConsoleReporter(private val logger: Logger) : Reporter {

    override fun report(report: ConflictReport) {
        if (report.conflicts.isEmpty()) return
        logger.warn("${report.conflicts.size} conflict(s) detected:")
        report.conflicts.forEach { conflict ->
            val text = buildString {
                appendLine("Version conflict: ${conflict.group}:${conflict.name} (using ${conflict.selectedVersion})")
                val sortedVersions = conflict.requestedVersions.sortedWith { a, b ->
                    when {
                        a.version == conflict.selectedVersion -> -1
                        b.version == conflict.selectedVersion -> 1
                        else -> b.version.compareTo(a.version)
                    }
                }
                sortedVersions.forEach { requested ->
                    val inUse = if (requested.version == conflict.selectedVersion) " (in use)" else ""
                    appendLine("- version ${requested.version}$inUse via:")
                    requested.dependencyPaths.forEach { path ->
                        append("     - ")
                        path.nodes.forEachIndexed { index, node ->
                            if (index > 0) append(" -> ")
                            append(node.displayName)
                            if (node.requestedVersion != null) append(":${node.requestedVersion}")
                        }
                        appendLine(" -> ${conflict.group}:${conflict.name}:${requested.version}")
                    }
                }
            }
            logger.warn(text)
        }
    }
}