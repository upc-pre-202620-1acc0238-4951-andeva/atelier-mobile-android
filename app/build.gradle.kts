import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// FCM: el plugin google-services solo se aplica si existe app/google-services.json (no se versiona).
// Sin el archivo el proyecto compila igual, pero las notificaciones push no funcionan.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

// Valores locales (no versionados): ver docs/04-mobile-integration.md §1
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.andeva.atelier.mobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.andeva.atelier.mobile"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"${localProps.getProperty("API_BASE_URL", "http://10.0.2.2:8080/")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY", "")}\"")
        buildConfigField("String", "SUPABASE_BUCKET", "\"${localProps.getProperty("SUPABASE_BUCKET", "atelier")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// Esquema de Room versionado (docs/06-offline-sync.md §5)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Jetpack Compose y Core
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)

    // Navegación type-safe
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Carga de imágenes remotas (Coil 3)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Red y consumo de APIs (Retrofit + Gson + OkHttp)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Inyección de dependencias (Hilt + KSP)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Base de datos local (Room 3 + KSP)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // Almacenamiento clave-valor reactivo (DataStore Preferences)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.preferences.core)

    // Trabajo en segundo plano (cola offline)
    implementation(libs.androidx.work.runtime.ktx)

    // Notificaciones push (FCM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Ubicación (asistencia con geocerca)
    implementation(libs.play.services.location)

    // Cámara (evidencias)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)

    // Google Sign-In
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
}
