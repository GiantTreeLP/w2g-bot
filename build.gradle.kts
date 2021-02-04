import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.4.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    application
}

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://dl.bintray.com/kordlib/Kord")
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {

    val kordVersion = "0.7.0-SNAPSHOT"
    val slf4jVersion = "1.7.30"

    implementation("dev.kord", "kord-core", kordVersion)
    implementation("org.slf4j", "slf4j-api", slf4jVersion)
    implementation("org.slf4j", "slf4j-jdk14", slf4jVersion)

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
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
