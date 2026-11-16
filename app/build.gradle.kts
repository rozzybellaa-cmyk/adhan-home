plugins {
    id("com.android.application")
}
android {
    namespace = "com.adhan.home"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.adhan.home"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
