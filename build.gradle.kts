import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.5.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    val detektVersion = "1.18.0-RC3"
    id("io.gitlab.arturbosch.detekt") version detektVersion
    application
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {

    val kordVersion = "0.7.4"
    val slf4jVersion = "1.7.32"

    val junitVersion = "5.7.2"

    implementation("dev.kord", "kord-core", kordVersion)
    implementation("org.slf4j", "slf4j-api", slf4jVersion)
    implementation("org.slf4j", "slf4j-jdk14", slf4jVersion)

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", junitVersion)
}

detekt {
    buildUponDefaultConfig = true

    reports {
        html.enabled = true
        xml.enabled = true
        txt.enabled = true
        sarif.enabled = true
    }
}

application {
    // Define the main class for the application.
    mainClass.set("de.gianttree.discord.w2g.WatchTogetherKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>() {
    jvmTarget = "1.8"
}
