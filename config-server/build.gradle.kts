plugins {
  alias(libs.plugins.kotlin.kapt)
  kotlin("jvm")
}

group = "org.alonalbert"

dependencies {
  implementation(libs.gson)
  implementation(libs.kotlinx.cli)
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
  implementation(libs.log4j.api)
  implementation(libs.log4j.core)
  implementation(libs.log4j.slf4j.impl)
}
