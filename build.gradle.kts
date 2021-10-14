import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val compileKotlin: KotlinCompile by tasks

val kotlinVersion: String by project
val ktorVersion: String by project
val kotlinpoetVersion: String by project
val googleAutoServiceVersion: String by project

plugins {
    `java-library`
    kotlin("jvm")
    kotlin("kapt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation ("com.squareup:kotlinpoet-classinspector-elements:$kotlinpoetVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.3.0")
    implementation("com.squareup:kotlinpoet:$kotlinpoetVersion")
    implementation("com.squareup:kotlinpoet-metadata:$kotlinpoetVersion")
    implementation("com.squareup:kotlinpoet-metadata-specs:$kotlinpoetVersion")
}

compileKotlin.kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
