import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.9.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    val detektVersion = "1.23.1"
    id("io.gitlab.arturbosch.detekt") version detektVersion
    application
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {

    val kordVersion = "0.11.1"
    val slf4jVersion = "2.0.9"
    val ktorVersion = "2.3.4"

    val exposedVersion = "0.43.0"
    val hikariVersion = "5.0.1"
    val sqliteVersion = "3.42.0.1"
    val mariadbVersion = "3.2.0"

    val junitVersion = "5.10.0"

    implementation("dev.kord", "kord-core", kordVersion)
    implementation("org.slf4j", "slf4j-api", slf4jVersion)
    implementation("org.slf4j", "slf4j-jdk14", slf4jVersion)

    implementation("io.ktor", "ktor-server-core", ktorVersion)
    implementation("io.ktor", "ktor-server-cio", ktorVersion)
    implementation("io.ktor", "ktor-server-content-negotiation", ktorVersion)
    implementation("io.ktor", "ktor-serialization-kotlinx-json", ktorVersion)

    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-kotlin-datetime", exposedVersion)

    implementation("com.zaxxer", "HikariCP", hikariVersion)

    implementation("org.xerial", "sqlite-jdbc", sqliteVersion)
    implementation("org.mariadb.jdbc", "mariadb-java-client", mariadbVersion)

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", junitVersion)

    detektPlugins("io.gitlab.arturbosch.detekt", "detekt-rules-libraries", detekt.toolVersion)
}

detekt {
    buildUponDefaultConfig = true
}

application {
    // Define the main class for the application.
    mainClass.set("de.gianttree.discord.w2g.WatchTogetherKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<Detekt>() {
    jvmTarget = "1.8"
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
    }
}
