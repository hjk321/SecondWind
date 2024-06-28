plugins {
    kotlin("jvm") version "1.9.23"
    id("io.github.goooler.shadow") version "8.1.7"
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

group = "com.hjk321.secondwind"
version = "1.0-SNAPSHOT"
description = "Do not go gentle into that good night."
project.ext["author"] = "hjk321"
project.ext["url"] = "https://hangar.papermc.io/hjk321/SecondWind"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
    implementation("io.papermc:paper-trail:1.0.1")
}

kotlin {
    jvmToolchain(21)
}

paper {
    apiVersion = "1.20.6"
    main = "com.hjk321.secondwind.SecondWind"
    bootstrapper = "com.hjk321.secondwind.SecondWindBootstrap"
    loader = "com.hjk321.secondwind.SecondWindLoader"
}

bukkit {
    apiVersion = "1.13"
    main = "com.hjk321.secondwind.papertrail.RequiresPaperPlugins"
}

tasks.shadowJar {
    mergeServiceFiles()

    exclude("org/intellij/**")
    exclude("org/slf4j/**")
    exclude("org/jetbrains/annotations/**")

    relocate("kotlin", "com.hjk321.secondwind.kotlin")
    relocate("io.papermc.papertrail", "com.hjk321.secondwind.papertrail")
}
