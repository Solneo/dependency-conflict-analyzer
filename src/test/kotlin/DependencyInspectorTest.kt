import inspector.DependencyInspectorService
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DependencyInspectorServiceTest {

    private lateinit var inspector: DependencyInspectorService

    @BeforeEach
    fun setup() {
        val project = ProjectBuilder.builder().build()
        val provider = project.gradle.sharedServices.registerIfAbsent(
            "test-inspector",
            DependencyInspectorService::class.java
        ) {
            parameters.failOnConflict.set(false)
            parameters.excludeCheckingLibraries.set(emptyList())
            parameters.excludeCheckingLibrariesGroup.set(emptyList())
        }
        inspector = provider.get()
    }

    private fun makeId(group: String, module: String, version: String): ComponentIdentifier =
        DefaultModuleComponentIdentifier.newId(
            DefaultModuleIdentifier.newId(group, module), version
        )

    @Test
    fun `findPathsToRoot returns single path for direct dependency`() {
        val root = makeId("project", "app", "")
        val lib = makeId("org.slf4j", "slf4j-api", "2.0.9")

        val parents = mapOf(lib to listOf(root))
        val paths = inspector.findPathsToRoot(lib, parents)

        assertEquals(1, paths.size)
        assertEquals(listOf(root, lib), paths.first().path)
    }

    @Test
    fun `findPathsToRoot returns multiple paths when lib has multiple parents`() {
        val root = makeId("project", "app", "")
        val profile = makeId("project", "profile", "")
        val lib = makeId("org.slf4j", "slf4j-api", "2.0.9")

        val parents = mapOf(
            lib to listOf(root, profile),
            profile to listOf(root)
        )
        val paths = inspector.findPathsToRoot(lib, parents)

        assertEquals(2, paths.size)
    }

    @Test
    fun `findPathsToRoot respects MAX_DEPTH`() {
        val ids = (0..DependencyInspectorService.MAX_DEPTH + 2).map { i ->
            makeId("org.test", "lib-$i", "1.0")
        }
        val parents = ids.zipWithNext()
            .associate { (child, parent) ->
                child to listOf(parent)
            }

        val paths = inspector.findPathsToRoot(ids.first(), parents)
        assertTrue(paths.isEmpty())
    }

    @Test
    fun `findPathsToRoot respects MAX_PATHS`() {
        val lib = makeId("org.slf4j", "slf4j-api", "2.0.9")
        val parents = mapOf<ComponentIdentifier, List<ComponentIdentifier>>(
            lib to (0..DependencyInspectorService.MAX_PATHS + 5).map { i ->
                makeId("project", "module-$i", "")
            }
        )
        val paths = inspector.findPathsToRoot(lib, parents)
        assertTrue(paths.size <= DependencyInspectorService.MAX_PATHS)
    }

    @Test
    fun `findPathsToRoot handles cycles without infinite loop`() {
        val a = makeId("org.test", "lib-a", "1.0")
        val b = makeId("org.test", "lib-b", "1.0")

        val parents = mapOf(
            a to listOf(b),
            b to listOf(a)
        )
        assertDoesNotThrow {
            inspector.findPathsToRoot(a, parents)
        }
    }

    @Test
    fun `findPathsToRoot returns empty when no parents found`() {
        val lib = makeId("org.slf4j", "slf4j-api", "2.0.9")
        val paths = inspector.findPathsToRoot(lib, emptyMap())
        assertEquals(1, paths.size)
        assertEquals(listOf(lib), paths.first().path)
    }

    @Test
    fun `clear resets all internal state`() {
        inspector.clear()
        assertDoesNotThrow {
            inspector.printConflicts()
        }
    }
}