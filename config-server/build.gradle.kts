plugins {
  alias(libs.plugins.kotlin.kapt)
  kotlin("jvm")
}

group = "org.alonalbert"

dependencies {
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
}
