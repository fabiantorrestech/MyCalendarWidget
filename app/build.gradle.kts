plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.fabiantorrestech.mycalendarwidget"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.fabiantorrestech.mycalendarwidget"
        minSdk = 26
        targetSdk = 36
        // Commit count: grows with every commit, the same on the PC and in CI (full clone),
        // so an update never looks like a downgrade.
        versionCode = providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }
            .standardOutput.asText.get().trim().toInt()
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Signed with the release key, so new builds install over old ones. Set in
    // ~/.gradle/gradle.properties on the PC and as ORG_GRADLE_PROJECT_* secrets in CI.
    // Without it, release builds fall back to the debug key.
    val releaseKeystore = providers.gradleProperty("MYCALENDARWIDGET_KEYSTORE_FILE").orNull
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.gradleProperty("MYCALENDARWIDGET_KEYSTORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("MYCALENDARWIDGET_KEYSTORE_ALIAS").get()
                keyPassword = storePassword
            }
        } else {
            logger.warn("MYCALENDARWIDGET_KEYSTORE_FILE not set: signing release with the debug key (won't install over the released app).")
        }
    }

    buildTypes {
        debug {
            // A separate app next to the released one, signed with the same key (ADR 7 in android-apps).
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}