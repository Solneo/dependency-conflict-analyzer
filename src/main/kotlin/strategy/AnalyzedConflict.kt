package strategy

data class AnalyzedConflict(
    val danger: Boolean = false,
    val msg: String = "",
    val key: String = "",
    val sourcesCount: Int = 0
)