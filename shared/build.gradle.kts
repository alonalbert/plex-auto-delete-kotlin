plugins {
  alias(libs.plugins.kotlin.kapt)
  kotlin("jvm")
}

group = "org.alonalbert"

dependencies {
  implementation(libs.gson)
}
