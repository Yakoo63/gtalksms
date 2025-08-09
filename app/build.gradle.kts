plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.googlecode.gtalksms"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.googlecode.gtalksms"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += setOf("META-INF/NOTICE.md", "META-INF/LICENSE.md")
        }
    }
}

dependencies {

    implementation(libs.smack.android) {
        exclude(group = "xpp3", module = "xpp3")
        exclude(group = "xpp3", module = "xpp3_min")
        exclude(group = "xmlpull", module = "xmlpull")
    }
    implementation(libs.smack.tcp) {
        exclude(group = "xpp3", module = "xpp3")
        exclude(group = "xpp3", module = "xpp3_min")
        exclude(group = "xmlpull", module = "xmlpull")
    }
    implementation(libs.smack.im) {
        exclude(group = "xpp3", module = "xpp3")
        exclude(group = "xpp3", module = "xpp3_min")
        exclude(group = "xmlpull", module = "xmlpull")
    }
    implementation(libs.smack.extensions) {
        exclude(group = "xpp3", module = "xpp3")
        exclude(group = "xpp3", module = "xpp3_min")
        exclude(group = "xmlpull", module = "xmlpull")
    }

    implementation(libs.androidx.fragment)
    implementation(libs.androidx.viewpager)
    implementation(libs.androidx.appcompat)

    implementation(libs.android.mail)
    implementation(libs.mail.android.activation)
    implementation(libs.httpcore)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}