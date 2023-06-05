plugins {
  alias(libs.plugins.kotlin.kapt)
  kotlin("jvm")
}

group = "org.alonalbert"

dependencies {
}

tasks.test {
  useJUnitPlatform()
}
kotlin {
  jvmToolchain(11)
}