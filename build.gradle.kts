plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.sonarqube)
}

// SonarQube Cloud analysis (run by CI when SONAR_TOKEN is set — see docs/QUALITY.md).
sonar {
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.organization", System.getenv("SONAR_ORGANIZATION") ?: "b-badis")
        property("sonar.projectKey", System.getenv("SONAR_PROJECT_KEY") ?: "b-badis_OpuntiaPad")
        property("sonar.projectName", "OpuntiaPad")
    }
}
