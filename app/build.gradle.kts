plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.spotless)
}

android {
    namespace = "com.pico.spatial.sample.animation"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pico.spatial.sample.animation"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = libs.versions.spatialBom.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        ndk { abiFilters.add("arm64-v8a") }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
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
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }
    androidResources { noCompress += listOf(".usdz", ".glb") }
}

spotless {
    java {
        target("**/*.java")
        targetExclude("**/build/**/*.java", ".gradle/**", "**/.gradle/**")
        googleJavaFormat("1.17.0").aosp().reflowLongStrings()
    }
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt", ".gradle/**", "**/.gradle/**")
        ktfmt("0.50").kotlinlangStyle()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude(
            "**/build/**/*.gradle.kts",
            "**/build/**/*.gradle",
            ".gradle/**",
            "**/.gradle/**"
        )
        ktfmt("0.50").kotlinlangStyle()
    }
}

dependencies {
    // Spatial SDK dependencies
    implementation(platform(libs.spatial.bom))
    implementation(libs.spatial.core)
    implementation(libs.spatial.foundation)
    implementation(libs.spatial.tracking)
    implementation(libs.spatial.sense)
    implementation(libs.spatial.ui.foundation)
    implementation(libs.spatial.ui.platform)
    implementation(libs.spatial.ui.design)

    // AndroidX, Compose, and other dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)
    implementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling.preview)
}

configurations.all {
    resolutionStrategy {
        exclude("androidx.compose.ui", "ui")
        exclude("androidx.compose.ui", "ui-graphics")
        exclude("androidx.compose.ui", "ui-text")
        exclude("androidx.compose.foundation", "foundation")
    }
}
