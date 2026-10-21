pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val useMapbox: Boolean = providers.gradleProperty("useMapbox").get().toBoolean()

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        if (useMapbox) {
            maven {
                url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            }
        }
    }
}

rootProject.name = "CarStatsViewer"

include(":automotive")
