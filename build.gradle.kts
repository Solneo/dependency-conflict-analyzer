import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    signing
}

gradlePlugin {
    plugins {
        create("dependencyConflictAnalyzer") {
            id = "com.dchernyaev.dependency-conflict-analyzer"
            implementationClass = "DependencyConflictAnalyzer"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.dchernyaev.dca"
            artifactId = "dependency-conflict-analyzer"
            version = "1.0.0-SNAPSHOT"

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
            val releasesRepoUrl = layout.buildDirectory.dir("repos/releases")
            val snapshotsRepoUrl = layout.buildDirectory.dir("repos/snapshots")
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
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

group = "com.dchernyaev.dca"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(gradleApi())
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}