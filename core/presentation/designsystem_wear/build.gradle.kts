plugins {
    alias(libs.plugins.runique.android.library.compose)
}

android {
    namespace = "com.example.core.presentation.designsystem_wear"
    defaultConfig {
        minSdk = 30
    }
}

dependencies {
    api(projects.core.presentation.designsystem)
    api(libs.androidx.wear.compose.material)
}