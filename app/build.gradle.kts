plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.artshop"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.artshop"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")// Function call syntax is similar
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = false
    }
}

dependencies {

    implementation(libs.appcompat.v161)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.recyclerview)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.google.firebase.storage)
    // Glide (Képbetöltéshez)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    implementation("com.github.chrisbanes:PhotoView:2.0.0")

    // Android Lifecycle (ViewModelhez, LiveDatahoz - bár most nem használjuk közvetlenül ViewModelt, jó ha itt van)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)

    implementation(libs.core.ktx)  // Permissions (Android 13+ Notifications) - Ha API 33+ a target

    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.v115)
    androidTestImplementation(libs.espresso.core.v351)
}