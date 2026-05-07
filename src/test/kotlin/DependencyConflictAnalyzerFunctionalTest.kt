import java.io.File
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DependencyConflictAnalyzerFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private fun setupProject() {
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "test-project"
            """.trimIndent()
        )

        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                `java-library`
                id("io.github.solneo.dependency-conflict-analyzer")
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation("com.google.code.gson:gson:1.7.2")
                implementation("com.google.code.gson:gson:2.10.1")
            }
            """.trimIndent()
        )

        val srcDir = File(projectDir, "src/main/java")
        srcDir.mkdirs()
        File(srcDir, "Dummy.java").writeText("public class Dummy {}")
    }

    @ParameterizedTest
    @ValueSource(strings = ["8.3", "8.5", "9.5.0"])
    fun `detects major version conflict`(gradleVersion: String) {
        setupProject()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withGradleVersion(gradleVersion)
            .withArguments("compileJava")
            .forwardOutput()
            .build()

        println("=== OUTPUT ===")
        println(result.output)
        println("=== END ===")

        assertTrue(
            result.output.contains("Version conflict: com.google.code.gson:gson"),
            "Expected conflict warning in output, got:\n${result.output}"
        )
    }
}