package strategy

import org.gradle.api.artifacts.result.DependencyResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MajorVersionConflictStrategy : ConflictStrategy {

    private val logger: Logger = LoggerFactory.getLogger(MajorVersionConflictStrategy::class.java)
    private val tempConflictHashMap = HashMap<String, DependencyNode>()

    override fun analyzeConflict(depResult: DependencyResult): AnalyzedConflict {
        val module = depResult.requested.displayName.split(":")

        try {
            val dep = tempConflictHashMap[module[0] + module[1]]
            val version = dep?.version
            if (version != null) {
                if (version.first().isDigit()
                    && module[2].first().isDigit()
                    && version.first() != module[2].first()
                ) {
                    val dependency = module[0] + ":" + module[1]
                    return AnalyzedConflict(
                        true, buildConflictMessage(
                            dependency,
                            DependencyNode(module[2], depResult.from.toString()),
                            DependencyNode(version, dep.from)
                        )
                    )
                }
            } else {
                tempConflictHashMap[module[0] + module[1]] =
                    DependencyNode(module[2], depResult.from.toString())
            }
        } catch (e: Exception) {
            logger.error("Unexpectable version convention. Fail with: ", e)
        }
        return AnalyzedConflict()
    }

    private fun buildConflictMessage(
        dependency: String,
        node: DependencyNode,
        node2: DependencyNode
    ): String {
        return "Danger conflict with $dependency between: " +
                "\n- version ${node.version} from --- ${node.from}" +
                "\n- version ${node2.version} from --- ${node2.from}" +
                "\n"
    }

}