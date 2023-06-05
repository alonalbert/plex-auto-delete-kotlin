import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.io.FileOutputStream

plugins {
//  application
  kotlin("jvm") version "1.8.21"
}

//application {
//  mainClass.set("com.aa.plexautodelete.MainKt")
//}

group = "org.example"
version = "1.0-SNAPSHOT"

dependencies {
  implementation(libs.gson)
  implementation(libs.kotlinx.cli)
  implementation(libs.sqlite.jdbc)

  testImplementation(kotlin("test"))
}

tasks {
  val executableJar = register<ExecutableJar>("executableJar", "com.aa.plexautodelete.MainKt")

  build {
    dependsOn(executableJar) // Trigger fat jar creation during build
  }
}

tasks.test {
  useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "17"
}

abstract class ExecutableJar @Inject constructor(private val mainClass: String) : DefaultTask() {
  init {
    super.dependsOn(listOf("compileJava", "compileKotlin", "processResources"))
  }

  @TaskAction
  fun build() {
    val jar = project.tasks.register<Jar>("jar-with-dependencies") {
      archiveClassifier.set("with-dependencies")
      duplicatesStrategy = DuplicatesStrategy.EXCLUDE
      manifest { attributes(mapOf("Main-Class" to mainClass)) }
      val sourcesOutput = project.sourceSets.main.get().output
      val content = project.configurations.runtimeClasspath.get().map { if (it.isDirectory) it else project.zipTree(it) } + sourcesOutput
      from(content)
    }.get()

    jar.actions.forEach {
      it.execute(jar)
    }

    val inputJarFile = jar.archiveFile.get().asFile
    val outputFile = File("${project.buildDir}/distributions", project.name)
    outputFile.ensureParentDirsCreated()
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
      it.write(FileInputStream(inputJarFile).readBytes())
    }
    outputFile.setExecutable(true)
  }
}
