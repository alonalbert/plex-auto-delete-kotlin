import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.io.FileOutputStream

plugins {
  application
  kotlin("jvm") version "1.6.21"
}

application {
  mainClass.set("com.aa.plexautodelete.MainKt")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.google.code.gson:gson:2.9.0")
  implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
  implementation("org.xerial:sqlite-jdbc:3.36.0.3")

  testImplementation(kotlin("test"))
}

tasks {
  val fatJar = register<FatJar>("fatJar")
  val executableJar = register<ExecutableJar>("executableJar", fatJar)

  build {
    dependsOn(executableJar) // Trigger fat jar creation during build
  }
}

tasks.test {
  useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}

abstract class FatJar : Jar() {
  init {
    dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources"))
    archiveClassifier.set("standalone")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    super.manifest { attributes(mapOf("Main-Class" to project.application.mainClass.get())) }

    val sourcesOutput = project.sourceSets.main.get().output
    val content = project.configurations.runtimeClasspath.get().map { if (it.isDirectory) it else project.zipTree(it) } + sourcesOutput
    super.from(content)
  }
}

abstract class ExecutableJar @Inject constructor(jarProvider: TaskProvider<Jar>) : DefaultTask() {
  private val jar = jarProvider.get()

  init {
    super.dependsOn(jar)
  }

  @TaskAction
  fun build() {
    val inputJarFile = jar.archiveFile.get().asFile
    val outputFile = "${project.buildDir}/distributions/${project.name}"
    FileOutputStream(outputFile).use {
      it.write(
        """
        #!/bin/sh

        MYSELF=`which "${'$'}0" 2>/dev/null`
        [ ${'$'}? -gt 0 -a -f "${'$'}0" ] && MYSELF="./${'$'}0"
        java=java
        if test -n "${'$'}JAVA_HOME"; then
            java="${'$'}JAVA_HOME/bin/java"
        fi
        java_args=-Xmx1g
        exec "${'$'}java" ${'$'}java_args -jar ${'$'}MYSELF "${'$'}@"
        exit 1 
      """.trimIndent().toByteArray()
      )
      it.write(FileInputStream(inputJarFile).readAllBytes())
    }
    File(outputFile).setExecutable(true)
  }
}
