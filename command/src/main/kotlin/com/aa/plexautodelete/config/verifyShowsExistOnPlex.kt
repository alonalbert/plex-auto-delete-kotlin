package com.aa.plexautodelete.config

import com.aa.plexautodelete.DEFAULT_CONFIG_FILE
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.io.File
import java.sql.DriverManager
import kotlin.system.exitProcess

private val STATEMENT1 = """ 
  SELECT DISTINCT 
    grandparent_title as show
  FROM metadata_item_views 
  WHERE metadata_type = 4 
""".trimIndent()

private val STATEMENT2 = """
  SELECT
    D.path as show
  FROM library_sections AS S
  INNER JOIN directories AS d ON D.parent_directory_id = S.id
   WHERE S.section_type = 2 AND S.name in ('TV', '')
""".trimIndent()

private val STATEMENTS = listOf(STATEMENT1, STATEMENT2)

fun main(vararg args: String) {
  val parser = ArgParser("Plex Auto Delete")
  val configFile by parser.option(ArgType.String, shortName = "c", description = "Config file").default(DEFAULT_CONFIG_FILE)
  val plexDatabaseFile by parser.option(ArgType.String, shortName = "d", description = "Plex database file").required()
  parser.parse(arrayOf(*args))
  if (!File(configFile).exists()) {
    println("Config file $configFile not found.")
    exitProcess(1)
  }
  val config = Config.loadFile(configFile)

  val plexShows = mutableSetOf<String>()
  DriverManager.getConnection("jdbc:sqlite:$plexDatabaseFile").use { connection ->
    connection.createStatement().use { statement ->
      STATEMENTS.forEach {
        statement.executeQuery(it.trimIndent()).use { result ->
          while (result.next()) {
            plexShows.add(result.getString("show"))
          }
        }
      }
    }
  }

  val usersShows = config.users.flatMapTo(mutableSetOf()) { it.shows.titles }
  val missing = usersShows subtract plexShows
  if (missing.isNotEmpty()) {
    println("Missing:")
    missing.forEach { println("  $it") }

    println("All Plex shows:")
    (plexShows subtract usersShows).forEach { println("  $it") }
  }
}