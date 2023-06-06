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
  implementation(libs.ktor.server.status.pages)
  implementation(libs.log4j.api)
  implementation(libs.log4j.core)
  implementation(libs.log4j.slf4j.impl)
}


tasks.withType<Jar> {
  enabled = true
  isZip64 = true
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  manifest {
    attributes(mapOf("Main-Class" to "com/aa/plexautodelete/configserver/ConfigServerKt"))
  }
  archiveFileName.set("${project.name}.jar")
  dependsOn(configurations.compileClasspath)
  val source = sourceSets.main.get().output
  val libs = configurations.runtimeClasspath.get().map {
    when {
      it.isDirectory -> it
      else -> zipTree(it)
    }
  }
  from(source + libs)
}
