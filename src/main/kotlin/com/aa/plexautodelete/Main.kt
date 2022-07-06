package com.aa.plexautodelete

import com.aa.plexautodelete.config.Config
import com.aa.plexautodelete.plex.Episode
import com.aa.plexautodelete.plex.PlexServer
import com.aa.plexautodelete.pushover.Pushover
import com.aa.plexautodelete.util.AppLogger
import com.aa.plexautodelete.util.toFileSize
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging.Level
import java.util.logging.Level.FINE
import java.util.logging.Level.INFO
import java.util.logging.Level.SEVERE
import kotlin.system.exitProcess

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
val DEFAULT_CONFIG_FILE = "${System.getProperty("user.home")}/.plex-auto-delete-config.json"

fun main(vararg args: String) {
  val parser = ArgParser("example")
  val configFile by parser.option(ArgType.String, shortName = "c", description = "Config file")
    .default(DEFAULT_CONFIG_FILE)
  val logLevel by parser.option(ArgType.Choice<Level>(listOf(INFO), { Level.parse(it) }, { it.name }), shortName = "v", description = "Log level")
    .default(FINE)
  val logFile by parser.option(ArgType.String, shortName = "l", description = "Log file")
  parser.parse(arrayOf(*args))

  val logger = AppLogger.createLogger(logLevel, logFile)
  if (!File(configFile).exists()) {
    println("Config file $configFile not found.")
    exitProcess(1)
  }
  val config = Config.loadFile(configFile)

  try {
    val server = PlexServer(config.plexUrl, config.plexToken)

    val now = Instant.now()
    val watchedEpisodes = server.getWatchedEpisodes(config.tvSections, config.plexToken, config.days)
    val maxNameLen = watchedEpisodes.maxOf { it.name.length }
    val maxShowNameLen = watchedEpisodes.maxOf { it.showName.length }
    logger.fine("Deletion candidates:")
    val episodesToDelete = watchedEpisodes.mapNotNull { episode ->
      logger.fine("  ${episode.toDisplayString(maxNameLen, maxShowNameLen, now)}")

      val unwatchedBy = config.users.filter { it.shows.contains(episode.showName) }.map { user ->
        WatchedBy(user.name, server.isWatchedBy(episode.key, user.plexToken))
      }.filter { !it.watched }.map { it.user }
      if (unwatchedBy.isNotEmpty()) {
        logger.fine("    Unwatched by ${unwatchedBy.joinToString()}")
        null
      } else {
        episode
      }
    }

    if (episodesToDelete.isEmpty()) {
      logger.fine("Nothing to delete")
    } else {
      logger.fine("Deleting episodes:")
      var totalFiles = 0
      var totalSize = 0L
      episodesToDelete.forEach { episode ->
        logger.fine("  ${episode.toDisplayString(maxNameLen, maxShowNameLen, now)}")
        server.getFiles(episode.key, config.plexToken).forEach files@{
          if (!it.exists()) {
            logger.warning("File ${it.path} not found. Is the section directory mounted?")
            return@files
          }
          val fileSize = it.length()
          totalSize += fileSize
          totalFiles++
          logger.finer("    ${fileSize.toFileSize().padEnd(8)} $it")
        }
      }
      val message = "Deleted $totalFiles files (${totalSize.toFileSize()})"
      val (appToken, userToken) = config.pushoverConfig
      Pushover.send(appToken, userToken, message)

      logger.fine(message)
    }
  } catch (e: Throwable) {
    logger.log(SEVERE, "Error", e)
  }
}

private class WatchedBy(val user: String, val watched: Boolean)

private fun Episode.toDisplayString(maxNameLen: Int, maxShowNameLen: Int, now: Instant): String {
  val time = DATE_FORMATTER.format(lastViewed)
  val show = showName.padEnd(maxShowNameLen)
  val episode = "S${seasonNumber.pad()}E${episodeNumber.pad()}".padEnd(7)
  val episodeName = name.padEnd(maxNameLen)
  val daysAgo = Duration.between(lastViewed, now).toDays()
  return "$time: $show $episode - $episodeName  - Watched $daysAgo days ago"
}

private fun Int.pad(): String = toString().padStart(2, '0')