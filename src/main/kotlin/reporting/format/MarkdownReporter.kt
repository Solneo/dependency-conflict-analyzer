package reporting.format

import analysis.ConflictReport
import analysis.DependencyPath
import java.io.File

internal class MarkdownReporter(private val outputFile: File) : Reporter {

    override fun report(report: ConflictReport) {
        outputFile.parentFile.mkdirs()
        outputFile.writeText(buildString {
            appendLine("# Dependency Conflict Report")
            appendLine()
            if (report.conflicts.isEmpty()) {
                append("No conflicts detected.")
                return@buildString
            }
            appendLine("**${report.conflicts.size} conflict(s) detected:**")
            appendLine()
            report.conflicts.forEach { conflict ->
                appendLine("- [${conflict.group}:${conflict.name}](#${conflict.name}) — using ${conflict.selectedVersion}")
            }
            appendLine()
            appendLine("---")
            report.conflicts.forEach { conflict ->
                appendLine()
                appendLine("<a id=\"${conflict.name}\"></a>")
                appendLine("## ${conflict.group}:${conflict.name} (using ${conflict.selectedVersion})")
                val sortedVersions = conflict.requestedVersions.sortedWith { a, b ->
                    when {
                        a.version == conflict.selectedVersion -> -1
                        b.version == conflict.selectedVersion -> 1
                        else -> b.version.compareTo(a.version)
                    }
                }
                sortedVersions.forEach { requested ->
                    appendLine()
                    val inUse = if (requested.version == conflict.selectedVersion) " • in use" else ""
                    appendLine("### Version ${requested.version}$inUse")
                    appendLine()
                    requested.dependencyPaths.forEach { path ->
                        renderPath(path, conflict.group, conflict.name, requested.version)
                    }
                }
                appendLine()
                appendLine("---")
            }
        })
    }

    private fun StringBuilder.renderPath(
        path: DependencyPath,
        group: String,
        name: String,
        version: String,
    ) {
        append("- ")
        path.nodes.forEachIndexed { index, node ->
            if (index > 0) append(" → ")
            append(if (node.requestedVersion != null) "${node.displayName}:${node.requestedVersion}" else node.displayName)
        }
        appendLine(" → $group:$name:$version")
    }
}