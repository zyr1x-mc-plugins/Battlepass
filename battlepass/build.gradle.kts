import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    kotlin("jvm")
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
    id("com.gradleup.shadow") version "8.3.5"
}

dependencies {
    implementation(project(":battlepass-api"))

    // CORE
    compileOnly(libs.leaf.api)

    // KOTLIN RUNTIME
    library(kotlin("stdlib"))

    // TOOLS
    library(libs.guice)
    library(libs.duration.serializer)

    // MINECRAFT TOOLS
    compileOnly(libs.placeholderapi)
    compileOnly(libs.kyori.minimessage)
    compileOnly(libs.invui)
    compileOnly(libs.worldedit)
    compileOnly(libs.worldguard)
    library(libs.litecommands)
    library(libs.sponge.yaml)
    library(libs.sponge.extra.kotlin)
    compileOnly(files("gradle/libs/FancyNpcs-2.11.0.jar"))

    // DATABASE
    library(libs.hibernate.core)
    library(libs.hibernate.hikari)
    library(libs.mariadb)

    // CACHE
    library(libs.redisson)
    library(libs.kryo)
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("kotlin/**")
        exclude("kotlinx/**")
    }
}

val assembleDistribution = tasks.register<Sync>("assembleDistribution") {
    group = "build"
    description = "Copies the plugin and API jars into build/dist"

    dependsOn(tasks.named("shadowJar"))
    dependsOn(tasks.named("jar"))
    dependsOn(project(":battlepass-api").tasks.named("jar"))

    into(layout.buildDirectory.dir("dist"))

    from(tasks.shadowJar.get().archiveFile) {
        rename { "Battlepass.jar" }
    }
    from(project(":battlepass-api").tasks.named("jar").map { it as org.gradle.jvm.tasks.Jar }) {
        rename { "battlepass-api.jar" }
    }
}

tasks.named("build") {
    dependsOn("shadowJar")
    dependsOn(assembleDistribution)
}

paper {
    name = "Battlepass"
    version = "1.0"
    main = "ru.lewis.battlepass.bootstrap.Bootstrap"
    loader = "ru.lewis.battlepass.BattlePassLoader"
    apiVersion = "1.21"
    author = "Lewis Carrol"

    generateLibrariesJson = true

    serverDependencies {
        register("WorldEdit") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("WorldGuard") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("FancyNpcs") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("PlaceholderAPI") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}
