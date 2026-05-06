import analysis.ConflictAnalyzer
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strategy.MajorVersionConflictStrategy

class ConflictAnalyzerTest {

    private lateinit var analyzer: ConflictAnalyzer

    @BeforeEach
    fun setup() {
        analyzer = ConflictAnalyzer(MajorVersionConflictStrategy())
    }

    private fun makeId(group: String, module: String, version: String): ComponentIdentifier =
        DefaultModuleComponentIdentifier.newId(
            DefaultModuleIdentifier.newId(group, module), version
        )

    @Test
    fun `findPathsToRoot returns single path for direct dependency`() {
        val root = makeId("project", "app", "")
        val lib = makeId("org.slf4j", "slf4j-api", "2.0.9")

        val paths = analyzer.findPathsToRoot(lib, mapOf(lib to listOf(root)), emptyMap())

        assertEquals(1, paths.size)
        assertEquals(listOf(root, lib), paths.first().path)
    }

    @Test
    fun `findPathsToRoot returns multiple paths when lib has multiple parents`() {
        val root = makeId("project", "app", "")
        val middle = makeId("project", "profile", "")
        val lib = makeId("org.slf4j", "slf4j-api", "2.0.9")

        val paths = analyzer.findPathsToRoot(
            lib,
            mapOf(lib to listOf(root, middle), middle to listOf(root)),
            emptyMap(),
        )

        assertEquals(2, paths.size)
    }

    @Test
    fun `findPathsToRoot respects MAX_DEPTH`() {
        val ids = (0..ConflictAnalyzer.MAX_DEPTH + 2).map { makeId("org.test", "lib-$it", "1.0") }
        val parents = ids.zipWithNext().associate { (child, parent) -> child to listOf(parent) }

        val paths = analyzer.findPathsToRoot(ids.first(), parents, emptyMap())

        assertTrue(paths.isEmpty())
    }

    @Test
    fun `findPathsToRoot respects MAX_PATHS`() {
        val lib = makeId("org.slf4j", "slf4j-api", "2.0.9")
        val manyParents = (0..ConflictAnalyzer.MAX_PATHS + 5).map { makeId("project", "module-$it", "") }

        val paths = analyzer.findPathsToRoot(lib, mapOf(lib to manyParents), emptyMap())

        assertTrue(paths.size <= ConflictAnalyzer.MAX_PATHS)
    }

    @Test
    fun `findPathsToRoot handles cycles without infinite loop`() {
        val a = makeId("org.test", "lib-a", "1.0")
        val b = makeId("org.test", "lib-b", "1.0")

        assertDoesNotThrow {
            analyzer.findPathsToRoot(a, mapOf(a to listOf(b), b to listOf(a)), emptyMap())
        }
    }

    @Test
    fun `findPathsToRoot returns single-element path when lib has no parents`() {
        val lib = makeId("org.slf4j", "slf4j-api", "2.0.9")

        val paths = analyzer.findPathsToRoot(lib, emptyMap(), emptyMap())

        assertEquals(1, paths.size)
        assertEquals(listOf(lib), paths.first().path)
    }
}
