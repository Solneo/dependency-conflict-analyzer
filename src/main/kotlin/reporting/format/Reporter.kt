package reporting.format

import analysis.ConflictReport

internal interface Reporter {
    fun report(report: ConflictReport)
}