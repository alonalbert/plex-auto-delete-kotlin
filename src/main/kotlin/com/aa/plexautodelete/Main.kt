package com.aa.plexautodelete

import com.aa.plexautodelete.config.Config
import com.aa.plexautodelete.plex.Episode
import com.aa.plexautodelete.plex.PlexServer
import com.aa.plexautodelete.pushover.Pushover
import com.aa.plexautodelete.util.AppLogger
import com.aa.plexautodelete.util.isIncluded
import com.aa.plexautodelete.util.toFileSize
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.logging.Level
import java.util.logging.Level.ALL
import java.util.logging.Level.CONFIG
import java.util.logging.Level.FINE
import java.util.logging.Level.FINER
import java.util.logging.Level.FINEST
import java.util.logging.Level.INFO
import java.util.logging.Level.OFF
import java.util.logging.Level.SEVERE
import java.util.logging.Level.WARNING
import java.util.logging.Level.parse
import kotlin.system.exitProcess

val DEFAULT_CONFIG_FILE = "${System.getProperty("user.home")}/.plex-auto-delete-config.json"

private val logLevels = listOf(OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL)

fun main(vararg args: String) {
  val parser = ArgParser("Plex Auto Delete")
  val configFile by parser.option(ArgType.String, shortName = "c", description = "Config file").default(DEFAULT_CONFIG_FILE)
  val logLevel by parser.option(ArgType.Choice<Level>(logLevels, { parse(it) }, { it.name }), shortName = "v", description = "Log level").default(FINER)
  val logFile by parser.option(ArgType.String, shortName = "l", description = "Log file")
  val interactive by parser.option(ArgType.Boolean, shortName = "i", description = "Interactive mode").default(false)
  parser.parse(arrayOf(*args))

  val logger = AppLogger.createLogger(logLevel, logFile)
  if (!File(configFile).exists()) {
    println("Config file $configFile not found.")
    exitProcess(1)
  }
  val config = Config.loadFile(configFile)

  try {
    val server = PlexServer(config.plexUrl)

    val now = Instant.now()
    val watchedEpisodes = server.getWatchedEpisodes(config.tvSections, config.users.first(), config.days)
    val maxNameLen = watchedEpisodes.maxOf { it.name.length }
    val maxShowNameLen = watchedEpisodes.maxOf { it.showName.length }

    logger.fine("Deletion candidates:")
    val episodesToDelete = watchedEpisodes.groupBy { it.showName }.flatMap { (showName, episodes) ->
      logger.fine("    $showName:")
      episodes.mapNotNull { episode ->
        val unwatchedByUsers = config.users.filter { it.shows.isIncluded(episode.showName) }.map { user ->
          WatchedBy(user.name, server.isWatchedBy(episode.key, user.plexToken))
        }.filter { !it.watched }.map { it.user }

        val episodeString = "${episode.toEpisodeId()}: ${episode.name.padEnd(maxNameLen)}"
        if (unwatchedByUsers.isEmpty()) {
          logger.fine("        $episodeString Can be deleted")
          episode
        } else {
          logger.fine("        $episodeString Unwatched by ${unwatchedByUsers.joinToString()}")
          null
        }
      }
    }

    if (episodesToDelete.isEmpty()) {
      logger.info("Nothing to delete")
    } else {
      if (interactive) {
        println("Marked for deletion:")
        episodesToDelete.forEach {
          println("  ${it.toDisplayString(maxNameLen, maxShowNameLen, now)}")
        }
        println("OK to delete (y or n) [n]? ")
        val answer = Scanner(System.`in`).nextLine()
        if (answer.lowercase() != "y") {
          exitProcess(0)
        }
      }
      logger.info("Deleting episodes:")
      var totalFiles = 0
      var totalSize = 0L
      val plexToken = config.users.first().plexToken
      episodesToDelete.forEach { episode ->
        logger.info("  ${episode.toDisplayString(maxNameLen, maxShowNameLen, now)}")
        server.getFiles(episode.key, plexToken).forEach files@{ file ->
          if (!file.exists()) {
            logger.warning("File ${file.path} not found. Is the section directory mounted?")
            return@files
          }
          val fileSize = file.length()
          logger.finer("    ${fileSize.toFileSize().padEnd(8)} $file")
          if (file.delete()) {
            totalSize += fileSize
            totalFiles++
          } else {
            logger.warning("Failed to delete $file")
          }
        }
      }
      val message = "Deleted $totalFiles files (${totalSize.toFileSize()})"
      val (appToken, userToken) = config.pushoverConfig
      Pushover.send(appToken, userToken, message)

      logger.info(message)
    }
  } catch (e: Throwable) {
    logger.log(SEVERE, "Error", e)
  }
}

private class WatchedBy(val user: String, val watched: Boolean)

private fun Episode.toDisplayString(maxNameLen: Int, maxShowNameLen: Int, now: Instant): String {
  val show = showName.padEnd(maxShowNameLen)
  val episodeId = toEpisodeId()
  val episodeName = name.padEnd(maxNameLen)
  val status = when (lastViewed) {
    Instant.EPOCH -> ""
    else -> "  - Watched ${Duration.between(lastViewed, now).toDays()} days ago"
  }
  return "$show $episodeId - $episodeName$status"
}

private fun Episode.toEpisodeId() = "S${seasonNumber.pad()}E${episodeNumber.pad()}".padEnd(7)
private fun Int.pad(): String = toString().padStart(2, '0')