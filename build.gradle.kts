plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.21" apply false
}

allprojects {
    group = "ru.lewis.battlepass"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.xenondevs.xyz/releases/")
        maven("https://repo.panda-lang.org/releases")
        maven("https://jitpack.io")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://maven.enginehub.org/repo/")
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/zyr1x-mc-plugins/Leaf")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    configurations.all {
        resolutionStrategy {
            force(libs.gson)
            force(libs.guava)
        }
    }
}
