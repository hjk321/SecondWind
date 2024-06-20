plugins {
    kotlin("jvm") version "1.9.23"
    id("io.github.goooler.shadow") version "8.1.7"
    id("io.papermc.paperweight.userdev") version "1.7.1"
}

group = "com.hjk321.secondwind"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
    implementation("io.papermc:paper-trail:0.0.1-SNAPSHOT")
}

kotlin {
    jvmToolchain(21)
}

tasks.shadowJar {
    mergeServiceFiles()

    exclude("org/intellij/**")
    exclude("org/slf4j/**")
    exclude("org/jetbrains/annotations/**")

    relocate("kotlin", "com.hjk321.secondwind.deps.kotlin")
    relocate("io.papermc.papertrail", "com.hjk321.secondwind.deps.papertrail")
}
