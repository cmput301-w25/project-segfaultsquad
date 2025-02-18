plugins {
    alias(libs.plugins.android.application)
    // Add the dependency for the Google services Gradle plugin (For Firebase/Google Maps API)
    // id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.segfaultsquadapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.segfaultsquadapplication"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // I put this here to enable the vector support for icnos taken from our figma design
        vectorDrawables.useSupportLibrary = true
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // firebase
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-firestore:25.1.1")
    // TODO: Add the dependencies for Firebase products you want to use
    // Firebase user auth
    implementation("com.google.firebase:firebase-auth:21.0.1")
    implementation("com.google.firebase:firebase-database:20.0.3")

    // Android Navigation Components
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    // Google Play services dependencies
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // firebase storage
    implementation("com.google.firebase:firebase-storage:20.3.0")


}

