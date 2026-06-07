val firebase: Boolean = providers.gradleProperty("useFirebase").get().toBoolean()
val mapbox: Boolean = providers.gradleProperty("useMapbox").get().toBoolean()

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.google.ksp)

}
if (firebase) {
    apply<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsPlugin>()
    apply<com.google.gms.googleservices.GoogleServicesPlugin>()
}

android {
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        targetSdk = 37
        versionCode = 331
        versionName = "0.29.0.0021"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // flavorDimensions = ["version", "aaos"]
    flavorDimensions += listOf("version", "aaos")

    productFlavors {
        create("stable") {
            dimension = "version"
            resValue("string","useFirebase", firebase.toString())
        }

        create("legacy") {
            dimension = "aaos"
            applicationId = "com.ixam97.carStatsViewer"
        }

        create("carapp") {
            dimension = "aaos"
            applicationId = "de.ixam97.carStatsViewer.carApp"
        }

        create("dev") {
            dimension = "version"
            applicationId = "com.ixam97.carStatsViewer_dev"
            resValue("string","useFirebase", firebase.toString())
        }
    }

    // use a dummy if mapbox api is not configured
    sourceSets.named("main") {
        kotlin.directories.add(if (mapbox) "src/mapbox/java" else "src/mapboxdummy/java" )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        resValues = true
        compose = true
    }

    // android.car exists since Android 10 (API level 29) Revision 5.
    useLibrary("android.car")
    namespace = "com.ixam97.carStatsViewer"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        ignoreWarnings = false
        quiet = true
    }

    packaging {
        resources {
            pickFirsts += listOf("META-INF/LICENSE.md", "META-INF/NOTICE.md")
        }
    }
}

dependencies {
    // implementation fileTree(include: ['*.jar'], dir: 'libs')
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.window)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.google.gson)
    implementation(libs.google.gms.location)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.aboutlibraries.core)
    implementation(libs.airbnb.paris)
    implementation(libs.github.egm96)
    implementation(libs.github.scrollbar)
    implementation(libs.okhttp3)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    implementation(libs.androidx.car.app)
    implementation(libs.androidx.car.app.automotive)

    if (mapbox) {
        implementation("com.mapbox.maps:android:11.24.3")
        implementation("com.mapbox.extension:maps-compose-ndk27:11.24.3")
    }

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.room)
    ksp(libs.androidx.room.compiler)

    implementation(platform(libs.google.firebase.bom))
    implementation(libs.google.firebase.crashlytics)
    implementation(libs.google.firebase.analytics)

    debugImplementation(libs.squareup.leakcanary)

    implementation("com.github.jetradarmobile:android-snowfall:1.2.1")

    implementation(libs.github.carcompose)

    // to fix unresolved references to android.car
    // def sdkDir = project.android.sdkDirectory.canonicalPath
    // def androidCarJar = "$sdkDir/platforms/android-33/optional/android.car.jar"
    // implementation(files(androidCarJar))
}
