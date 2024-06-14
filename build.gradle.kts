plugins {
    kotlin("jvm") version "1.9.23"
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "com.hjk321.secondwind"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
}

kotlin {
    jvmToolchain(21)
}

tasks.shadowJar {
    mergeServiceFiles()

    exclude("org/intellij/**")
    exclude("org/slf4j/**")

    relocate("kotlin", "com.hjk321.secondwind.deps.kotlin")
    relocate("org.jetbrains.annotations", "com.hjk321.secondwind.deps.annotations")
}
