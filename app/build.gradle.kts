import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.takahirom.roborazzi")
}

// 서명 정보는 저장소 밖 파일(keystore.properties, .gitignore 처리)에서 읽는다
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasReleaseKey = keystoreProps.getProperty("storeFile") != null

// 버전: CI에서는 태그(v1.2.3)로 -PappVersion=1.2.3을 넘긴다. 로컬 기본값은 gradle.properties.
val appVersion = (findProperty("appVersion") as String?) ?: "0.0.0"
val appVersionCode = appVersion.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }.let {
    it.getOrElse(0) { 0 } * 10000 + it.getOrElse(1) { 0 } * 100 + it.getOrElse(2) { 0 }
}.coerceAtLeast(1)

android {
    namespace = "app.sureureuk"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.sureureuk"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion
        buildConfigField("String", "UPDATE_REPO", "\"happybrown/srr\"")
    }

    // github: GitHub Releases로 배포(개인용), 새 버전 확인 + 접근성 바로 잠그기
    // play:   플레이 스토어 배포, 인터넷 권한 없음 (스토어 정책상 자체 업데이트 금지)
    flavorDimensions += "store"
    productFlavors {
        create("github") {
            dimension = "store"
            buildConfigField("boolean", "UPDATE_CHECK", "true")
            buildConfigField("boolean", "A11Y_LOCK", "true")
        }
        create("play") {
            dimension = "store"
            buildConfigField("boolean", "UPDATE_CHECK", "false")
            buildConfigField("boolean", "A11Y_LOCK", "false")
        }
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseKey) signingConfigs.getByName("release") else null
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
        buildConfig = true
    }

    // 화면 스크린샷 테스트 (Robolectric + Roborazzi): ./gradlew recordRoborazziGithubDebug
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.36.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.36.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
