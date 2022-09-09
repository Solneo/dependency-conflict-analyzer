import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.gradle.plugin-publish") version "1.0.0"
    kotlin("jvm") version "1.5.31"
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    signing
}

gradlePlugin {
    plugins {
        create("dependencyConflictAnalyzer") {
            id = "io.github.solneo.dependency-conflict-analyzer"
            displayName = "Dependency Conflict Analyzer"
            description = "Help find conflict in gradle"
            implementationClass = "DependencyConflictAnalyzer"
        }
    }
}

pluginBundle {
    website = "https://github.com/Solneo/dependency-conflict-analyzer"
    vcsUrl = "https://github.com/Solneo/dependency-conflict-analyzer"
    tags = listOf("dependency", "conflict", "analyzer", "plugins")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.solneo"
            artifactId = "dependency-conflict-analyzer"
            version = "${project.version}"

            from(components["java"])

            pom {
                name.set("Dependency conflict analyzer")
                description.set("Help find conflict in gradle")
                url.set("https://github.com/Solneo/dependency-conflict-analyzer")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("dchernyaev")
                        name.set("Daniil Chernyaev")
                        email.set("chdanilr@gmail.com")
                    }
                }
                scm {
                    connection.set("https://github.com/Solneo/dependency-conflict-analyzer.git")
                    developerConnection.set("git@github.com:Solneo/dependency-conflict-analyzer.git")
                    url.set("https://github.com/Solneo/dependency-conflict-analyzer")
                }
            }
        }
    }

    repositories {
        maven {
            url = if ("${project.version}".contains("SNAPSHOT")) {
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            } else {
                uri("https://s01.oss.sonatype.org/content/repositories/releases/")
            }
            credentials {
                username = project.properties["mavenCentralUsername"].toString()
                password = project.properties["mavenCentralPassword"].toString()
            }
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

signing {
    sign(publishing.publications["maven"])
}

group = "io.github.solneo"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}