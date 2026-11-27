import java.util.Properties

plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Release signing is loaded from a local properties file or env, never committed.
 * Search order:
 *   1. MI5G_DEBLOAT_SIGNING_PROPERTIES (path to a properties file)
 *   2. ~/.gradle/mi5g-debloat-signing.properties
 * Keys: storeFile, storePassword, keyAlias, keyPassword.
 * Absent → release builds are left unsigned (CI / other machines).
 */
val signingProps: Properties? = listOfNotNull(
    System.getenv("MI5G_DEBLOAT_SIGNING_PROPERTIES")?.let { file(it) },
    file("${System.getProperty("user.home")}/.gradle/mi5g-debloat-signing.properties"),
).firstOrNull { it.isFile }?.let { f ->
    Properties().apply { f.inputStream().use { load(it) } }
}?.takeIf { p ->
    listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all { !p.getProperty(it).isNullOrBlank() } &&
        file(p.getProperty("storeFile")).isFile
}

android {
    namespace = "com.midroid.debloat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.midroid.debloat"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.1.4"
    }

    signingConfigs {
        if (signingProps != null) create("release") {
            storeFile = file(signingProps.getProperty("storeFile"))
            storePassword = signingProps.getProperty("storePassword")
            keyAlias = signingProps.getProperty("keyAlias")
            keyPassword = signingProps.getProperty("keyPassword")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            // Minify left off: Shizuku uses reflection (newProcess) and this is a first release.
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.findByName("release")
            if (signingProps == null) logger.warn("Mi5G Debloat: no signing.properties found — release APK will be UNSIGNED")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    androidResources { localeFilters += listOf("ar", "en") }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.core)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.vm)
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.tooling.preview)
    implementation(libs.material3)
    implementation(libs.icons)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
