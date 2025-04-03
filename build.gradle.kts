import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

import java.io.File
import java.io.FileInputStream
import java.util.*

val compileKotlin: KotlinCompile by tasks

val kotlinVersion: String by project
val ktorVersion: String by project
val kotlinpoetVersion: String by project
val googleAutoServiceVersion: String by project

val prop = Properties().apply {
    load(FileInputStream(File(project.gradle.gradleUserHomeDir, "local.properties")))
}

plugins {
    `java-library`
    `maven-publish`
    signing
    kotlin("jvm") version "2.1.0"
    kotlin("kapt") version "2.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("com.squareup:kotlinpoet:$kotlinpoetVersion")
    implementation("com.squareup:kotlinpoet-metadata:$kotlinpoetVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    dependsOn("javadoc")
    from(tasks.javadoc.get().destinationDir)
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    repositories {
        maven {
            name = "local"
            isAllowInsecureProtocol = true
            url = uri("http://localhost:8081/repository/maven-local")
        }
        maven {
            name = "sonatype"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = prop.getProperty("newOssrhUsername")
                password = prop.getProperty("newOssrhPassword")
            }
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])

            group = "com.casadetasha"
            artifactId = "annotation-parser"
            version = "2.1.0-alpha4"

            artifact(sourcesJar.get())
            artifact(javadocJar.get())

            pom {
                name.set("CDT Annotation Parser")
                description.set("An annotation parser to correlate all relevant data for code generation.")
                url.set("http://www.sproutes.io")

                scm {
                    connection.set("scm:git://github.com/Path-to-plunder/kotlin-annotation-processor.git")
                    developerConnection.set("scm:git:git@github.com:Path-to-plunder/kotlin-annotation-processor.git")
                    url.set("https://github.com/Path-to-plunder/kotlin-annotation-processor")
                }

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("gabriel")
                        name.set("Gabriel Spencer")
                        email.set("gabriel@casadetasha.dev")
                    }
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
