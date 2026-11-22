plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.sonarqube)
}

// SonarQube Cloud analysis (run by CI when SONAR_TOKEN is set — see docs/QUALITY.md).
// GitHub passes unset repository variables as empty strings, so blank values fall back to the defaults.
fun envOrDefault(name: String, default: String): String = System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

sonar {
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.organization", envOrDefault("SONAR_ORGANIZATION", "b-badis"))
        property("sonar.projectKey", envOrDefault("SONAR_PROJECT_KEY", "b-badis_OpuntiaPad"))
        property("sonar.projectName", "OpuntiaPad")
    }
}
