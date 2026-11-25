plugins {
    id("com.android.application") version "9.2.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("com.google.devtools.ksp") version "2.3.6"
    id("androidx.room") version "2.8.4"
}

android {
    namespace = "com.tazer.taskorganizer"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.tazer.taskorganizer"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(25)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.11.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    implementation("androidx.room:room-runtime:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
