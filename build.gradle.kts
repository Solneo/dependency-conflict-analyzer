plugins {
    id("com.gradle.plugin-publish") version "2.1.1"
    kotlin("jvm") version "1.5.31"
    `java-gradle-plugin`
    `kotlin-dsl`
}

gradlePlugin {
    website.set("https://github.com/Solneo/dependency-conflict-analyzer")
    vcsUrl.set("https://github.com/Solneo/dependency-conflict-analyzer")

    plugins {
        create("dependencyConflictAnalyzer") {
            id = "io.github.solneo.dependency-conflict-analyzer"
            displayName = "Dependency Conflict Analyzer"
            description = "Help find conflict in gradle"
            implementationClass = "DependencyConflictAnalyzer"

            tags.set(listOf("dependency", "conflict", "analyzer", "diagnostics"))
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(17)
}

group = "io.github.solneo"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(gradleApi())
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
}