plugins {
    id("com.android.application")
    // Firebase BoM (Bill of Materials) használata
    id("com.google.gms.google-services") // Firebase plugin
}

android {
    namespace = "com.example.artshop" // Módosítsd a saját csomagnevedre
    compileSdk = 35 // Vagy a legfrissebb stabil SDK (you had 35 in your example, using it here)

    defaultConfig {
        applicationId = "com.example.artshop" // Módosítsd a saját csomagnevedre
        minSdk = 24 // API Level 24 (Android 7.0) vagy magasabb ajánlott
        targetSdk = 35 // Matching compileSdk for consistency, you can adjust if needed
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // minifyEnabled -> isMinifyEnabled in Kotlin DSL
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") // Function call syntax is similar
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // JavaVersion.VERSION_1_8 is already a constant, no change needed
        targetCompatibility = JavaVersion.VERSION_1_8   // JavaVersion.VERSION_1_8 is already a constant, no change needed
    }
    buildFeatures {
        viewBinding = false // viewBinding -> viewBinding in Kotlin DSL, assignment with =
    }
}

dependencies {

    implementation(libs.appcompat.v161)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.recyclerview)

    // Firebase BoM (egységes verziókezelés)
    implementation(platform(libs.firebase.bom))

    // Firebase Authentication
    implementation(libs.firebase.auth)

    // Firebase Cloud Firestore (később kell a CRUD-hoz, de már most hozzáadhatod)
    implementation(libs.firebase.firestore)

    // Glide (Képbetöltéshez)
    implementation(libs.glide)
    // annotationProcessor("com.github.bumptech.glide:compiler:4.12.0") // Glide v4.12+ nem igényel külön processzort - Commented out as in original

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.v115)
    androidTestImplementation(libs.espresso.core.v351)
}