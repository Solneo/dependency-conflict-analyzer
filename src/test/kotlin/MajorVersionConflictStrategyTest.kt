import graph.DependencyBucket
import graph.DependencyRequested
import graph.DependencySource
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
        selected: String,
    ): DependencyBucket {
        val root = makeId("project", "app", "")
        val requested = versions.associateWith {
            DependencyRequested(mutableSetOf(DependencySource(listOf(root))))
        }.toMutableMap()
        return DependencyBucket(group, name, requested, selected)
    }

    @Test
    fun `detects major version conflict`() {
        val bucket = makeBucket("org.slf4j", "slf4j-api", listOf("1.7.25", "2.0.9"), "2.0.9")
        val conflicts = strategy.findConflicts(listOf(bucket))
        assertEquals(1, conflicts.size)
        assertEquals("org.slf4j", conflicts.first().group)
        assertEquals("slf4j-api", conflicts.first().name)
    }

    @Test
    fun `ignores minor version conflict`() {
        val bucket = makeBucket("ch.qos.logback", "logback-classic", listOf("1.4.11", "1.5.32"), "1.5.32")
        assertTrue(strategy.findConflicts(listOf(bucket)).isEmpty())
    }

    @Test
    fun `no conflict when single version`() {
        val bucket = makeBucket("org.slf4j", "slf4j-api", listOf("2.0.9"), "2.0.9")
        assertTrue(strategy.findConflicts(listOf(bucket)).isEmpty())
    }

    @Test
    fun `detects conflict across multiple major versions`() {
        val bucket = makeBucket("org.slf4j", "slf4j-api", listOf("1.7.25", "2.0.7", "2.0.9"), "2.0.9")
        assertEquals(1, strategy.findConflicts(listOf(bucket)).size)
    }

    @Test
    fun `conflict contains all requested versions`() {
        val bucket = makeBucket("org.slf4j", "slf4j-api", listOf("1.7.25", "2.0.9"), "2.0.9")
        val conflict = strategy.findConflicts(listOf(bucket)).first()
        val versions = conflict.requestedVersions.map { it.version }
        assertTrue(versions.contains("1.7.25"))
        assertTrue(versions.contains("2.0.9"))
    }

    @Test
    fun `conflict contains selected version`() {
        val bucket = makeBucket("org.slf4j", "slf4j-api", listOf("1.7.25", "2.0.9"), "2.0.9")
        val conflict = strategy.findConflicts(listOf(bucket)).first()
        assertEquals("2.0.9", conflict.selectedVersion)
    }

    @Test
    fun `ignores blank versions`() {
        val bucket = makeBucket("org.slf4j", "slf4j-api", listOf("", "2.0.9"), "2.0.9")
        assertTrue(strategy.findConflicts(listOf(bucket)).isEmpty())
    }

    @Test
    fun `returns conflicts for multiple buckets`() {
        val b1 = makeBucket("org.slf4j", "slf4j-api", listOf("1.7.25", "2.0.9"), "2.0.9")
        val b2 = makeBucket("com.example", "lib", listOf("1.0.0", "2.0.0"), "2.0.0")
        val b3 = makeBucket("org.other", "stable", listOf("1.0.0", "1.1.0"), "1.1.0")
        assertEquals(2, strategy.findConflicts(listOf(b1, b2, b3)).size)
    }
}