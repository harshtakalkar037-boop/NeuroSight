plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.neurosight.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neurosight.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // TFLite models are often large / already compressed. Prevent the build
        // tool from trying to re-compress the .tflite file (which can corrupt it
        // or bloat build time). See the assets/ noCompress block below too.
        vectorDrawables { useSupportLibrary = true }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Keep in sync with the Kotlin plugin version above.
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // IMPORTANT: keep the .tflite file uncompressed in the APK/AAB so it can be
    // memory-mapped efficiently by the TFLite runtime at load time.
    androidResources {
        noCompress += listOf("tflite")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ---------- Jetpack Compose ----------
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ---------- CameraX ----------
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ---------- TensorFlow Lite ----------
    // Core runtime.
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    // GPU delegate (fallback acceleration path if Hexagon/NNAPI isn't available).
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")
    // Support library: image ops (resize/normalize) and TensorImage/TensorBuffer helpers.
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    // NOTE on Hexagon NPU delegate:
    // The classic "tensorflow-lite-hexagon" AAR (org.tensorflow:tensorflow-lite-hexagon)
    // is the delegate historically used to target Qualcomm Hexagon DSPs/NPUs. On recent
    // Snapdragon SoCs (as used in iQOO devices), NNAPI or Qualcomm's QNN delegate is the
    // more current path to the NPU. See ml/NeuroSightClassifier.kt for TODOs on wiring
    // whichever is available on the target device/SDK version you have access to during
    // the hackathon. The app is written to gracefully fall back to NNAPI -> GPU -> CPU
    // if a Hexagon-specific delegate cannot be initialized.
    // implementation("org.tensorflow:tensorflow-lite-hexagon:2.16.1") // TODO: uncomment if you add the AAR

    // ---------- Coroutines ----------
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // ---------- Core ----------
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // ---------- Testing ----------
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
