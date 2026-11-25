plugins {
    id("com.android.application") version "9.2.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("com.google.devtools.ksp") version "2.3.6"
    id("androidx.room") version "2.8.4"
}

fun publicOrigin(name: String, allowMock: Boolean = false): String {
    val value = providers.gradleProperty(name).orElse(providers.environmentVariable(name))
        .orElse("").get().trim().removeSuffix("/")
    val https = Regex("https://[A-Za-z0-9.-]+(?::[0-9]{1,5})?")
    val mock = Regex("http://(?:127\\.0\\.0\\.1|10\\.0\\.2\\.2)(?::[0-9]{1,5})?")
    require(value.isEmpty() || https.matches(value) || (allowMock && mock.matches(value))) {
        "$name must be an HTTPS origin or an allowed debug loopback origin"
    }
    return value
}

val supabaseOrigin = publicOrigin("TAZER_SUPABASE_URL")
val backendOrigin = publicOrigin("TAZER_BACKEND_URL")
val publishableKey = providers.gradleProperty("TAZER_SUPABASE_PUBLISHABLE_KEY")
    .orElse(providers.environmentVariable("TAZER_SUPABASE_PUBLISHABLE_KEY"))
    .orElse("").get().trim()
require(publishableKey.isEmpty() || Regex("sb_publishable_[A-Za-z0-9_-]+").matches(publishableKey)) {
    "TAZER_SUPABASE_PUBLISHABLE_KEY must be a Supabase publishable key"
}
val mockEnabled = providers.gradleProperty("TAZER_MOCK_ENABLED")
    .orElse(providers.environmentVariable("TAZER_MOCK_ENABLED"))
    .orElse("false").get().equals("true", ignoreCase = true)
val mockOrigin = publicOrigin("TAZER_MOCK_URL", allowMock = true).ifEmpty { "http://127.0.0.1:8787" }
require(mockOrigin.startsWith("http://")) { "TAZER_MOCK_URL must be a loopback HTTP origin" }

android {
    namespace = "com.tazer.taskorganizer"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.tazer.taskorganizer"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseOrigin\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"$publishableKey\"")
        buildConfigField("String", "BACKEND_URL", "\"$backendOrigin\"")
        buildConfigField("boolean", "MOCK_ENABLED", "false")
        buildConfigField("String", "MOCK_URL", "\"\"")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "MOCK_ENABLED", mockEnabled.toString())
            buildConfigField("String", "MOCK_URL", "\"$mockOrigin\"")
        }
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
