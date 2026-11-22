import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release signing: read from environment variables (CI) or from keystore.properties (local).
// Nothing secret is ever committed; see docs/RELEASING.md.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use(::load)
}
fun signingValue(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: keystoreProps.getProperty(prop)

val releaseStoreFile = signingValue("OPUNTIAPAD_KEYSTORE_FILE", "storeFile")
val hasReleaseSigning = releaseStoreFile != null && rootProject.file(releaseStoreFile).exists()

android {
    namespace = "io.github.bbadis.opuntiapad"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        applicationId = "io.github.bbadis.opuntiapad"
        minSdk = 26
        targetSdk = 37
        versionName = providers.gradleProperty("opuntiapad.versionName").get()
        versionCode = providers.gradleProperty("opuntiapad.versionCode").get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Only ship the translations we actually maintain (keeps library strings consistent).
    androidResources {
        localeFilters += listOf("en", "fr", "ar", "es", "de")
        generateLocaleConfig = true
    }

    signingConfigs {
        // Shared, public debug key (not secret) so every dev build can update the previous one,
        // whichever machine or CI runner built it.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = signingValue("OPUNTIAPAD_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("OPUNTIAPAD_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("OPUNTIAPAD_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/*.kotlin_module", "DebugProbesKt.bin")
    }

    dependenciesInfo {
        // Not useful for an open-source app distributed outside Play, and blocked by F-Droid.
        includeInApk = false
        includeInBundle = false
    }

    testOptions {
        // android.* calls (e.g. Log in the TextMate engine) become no-ops in JVM unit tests.
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true
        disable += setOf("MissingTranslation")
    }
}

// The JVM tests validate the bundled TextMate assets: re-run them whenever the assets change.
tasks.withType<Test>().configureEach {
    inputs.dir("src/main/assets/textmate").withPathSensitivity(PathSensitivity.RELATIVE)
}

sonar {
    properties {
        // Source and test folders are detected from the Android variants by the plugin itself.
        property("sonar.androidLint.reportPaths", layout.buildDirectory.file("reports/lint-results-debug.xml").get().asFile.path)
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            layout.buildDirectory.file("reports/coverage/test/debug/report.xml").get().asFile.path,
        )
        // Generated TextMate assets are third-party data, not code to analyse.
        property("sonar.exclusions", "src/main/assets/**")
        // Unit-test coverage is measured on the pure-Kotlin logic. Code bound to the Android framework
        // (Compose UI, Activity/Application, the Sora editor view, ContentResolver, DataStore, assets)
        // cannot run in JVM unit tests and is exercised by the instrumented tests instead.
        property(
            "sonar.coverage.exclusions",
            listOf(
                "src/main/java/**/ui/**",
                "src/main/java/**/editor/**",
                "src/main/java/**/MainActivity.kt",
                "src/main/java/**/OpuntiaPadApp.kt",
                "src/main/java/**/core/files/**",
                "src/main/java/**/core/settings/**",
                "src/main/java/**/core/language/LanguageCatalog.kt",
            ).joinToString(","),
        )
    }
}

base {
    archivesName.set("opuntiapad")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(platform(libs.sora.bom))
    implementation(libs.sora.editor)
    implementation(libs.sora.language.textmate)

    testImplementation(libs.junit)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
