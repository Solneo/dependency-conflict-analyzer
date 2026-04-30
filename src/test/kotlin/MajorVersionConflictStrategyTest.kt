import inspector.DependencyBucket
import inspector.DependencyRequested
import inspector.DependencySource
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import strategy.MajorVersionConflictStrategy

class MajorVersionConflictStrategyTest {

    private val strategy = MajorVersionConflictStrategy()

    private fun makeId(group: String, module: String, version: String) =
        DefaultModuleComponentIdentifier.newId(
            DefaultModuleIdentifier.newId(group, module), version
        )

    private fun makeBucket(
        group: String,
        name: String,
        versions: List<String>,
        selected: String
    ): DependencyBucket {
        val requested = versions.associateWith { version ->
            DependencyRequested(
                mutableSetOf(
                    DependencySource(
                        listOf(
                            makeId(
                                "project",
                                "app",
                                ""
                            )
                        )
                    )
                )
            )
        }.toMutableMap()
        return DependencyBucket(group, name, requested, selected)
    }

    @Test
    fun `detects major version conflict`() {
        val bucket = makeBucket("org.slf4j", "slf4j-api", listOf("1.7.25", "2.0.9"), "2.0.9")
        val result = strategy.analyzeConflict(bucket)
        assertTrue(result.danger)
        assertTrue(result.msg.contains("Version conflict detected: org.slf4j:slf4j-api"))
    }

    @Test
    fun `ignores minor version conflict`() {
        val bucket = makeBucket("ch.qos.logback", "logback-classic", listOf("1.4.11", "1.5.32"), "1.5.32")
        val result = strategy.analyzeConflict(bucket)
        assertFalse(result.danger)
    }

    @Test
    fun `no conflict when single version`() {
        val bucket = makeBucket("org.slf4j", "slf4j-api", listOf("2.0.9"), "2.0.9")
        val result = strategy.analyzeConflict(bucket)
        assertFalse(result.danger)
    }

    @Test
    fun `detects conflict with multiple versions across majors`() {
        val bucket = makeBucket("org.slf4j", "slf4j-api", listOf("1.7.25", "2.0.7", "2.0.9"), "2.0.9")
        val result = strategy.analyzeConflict(bucket)
        assertTrue(result.danger)
    }

    @Test
    fun `msg contains all conflicting versions`() {
        val bucket = makeBucket("org.slf4j", "slf4j-api", listOf("1.7.25", "2.0.9"), "2.0.9")
        val result = strategy.analyzeConflict(bucket)
        assertTrue(result.msg.contains("1.7.25"))
        assertTrue(result.msg.contains("2.0.9"))
    }

    @Test
    fun `msg contains selected version`() {
        val bucket = makeBucket("org.slf4j", "slf4j-api", listOf("1.7.25", "2.0.9"), "2.0.9")
        val result = strategy.analyzeConflict(bucket)
        assertTrue(result.msg.contains("→ using 2.0.9"))
    }

    @Test
    fun `ignores blank versions`() {
        val bucket = makeBucket("org.slf4j", "slf4j-api", listOf("", "2.0.9"), "2.0.9")
        val result = strategy.analyzeConflict(bucket)
        assertFalse(result.danger)
    }
}