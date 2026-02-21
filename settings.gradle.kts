pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
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
                // Do not change the username below. It should always be "mapbox" (not your username).
                // credentials.username = "mapbox"
                // Use the secret token stored in local.properties (not tracked by git) as the password

                // val properties = Properties()
                // properties.load(file("local.properties").newDataInputStream())

                // credentials.password = properties.getProperty("MAPBOX_DOWNLOADS_TOKEN")
                // authentication { basic(BasicAuthentication) }
            }
        }
    }
}

rootProject.name = "CarStatsViewer"

include(":automotive")
