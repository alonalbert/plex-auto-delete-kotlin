package com.aa.plexautodelete

import com.aa.plexautodelete.config.Config
import com.aa.plexautodelete.plex.Episode
import com.aa.plexautodelete.plex.PlexServer
import com.aa.plexautodelete.util.AppLogger.CONSOLE_LOGGER
import com.aa.plexautodelete.util.AppLogger.createLogger
import com.aa.plexautodelete.util.toFileSize
import com.google.gson.Gson
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.FileReader
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
val DEFAULT_CONFIG_FILE = "${System.getProperty("user.home")}/.plex-auto-delete-config"

fun main(vararg args: String) {
  val parser = ArgParser("example")
  val configFile by parser.option(ArgType.String, shortName = "c", description = "Config file").default(DEFAULT_CONFIG_FILE)
  val logFile by parser.option(ArgType.String, shortName = "l", description = "Log file")
  parser.parse(arrayOf(*args))

  val logger = logFile?.let { createLogger(it) } ?: CONSOLE_LOGGER
  val config = Gson().fromJson(FileReader(configFile), Config::class.java)

  val server = PlexServer(config.plexUrl, config.plexToken)
  val watchedEpisodes = server.getWatchedEpisodes(config.tvSections, config.plexToken, config.days)

  val now = Instant.now()
  logger.fine("Deletion candidates:")
  val episodesToDelete = watchedEpisodes.mapNotNull { episode ->
    logger.fine("  ${episode.toDisplayString(now)}")

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
    var totalSize = 0L
    episodesToDelete.forEach { episode ->
      logger.fine("  ${episode.toDisplayString(now)}")
      server.getFiles(episode.key, config.plexToken).forEach {
        val fileSize = it.length()
        totalSize += fileSize
        logger.fine("    ${fileSize.toFileSize().padEnd(8)} $it")
      }
      logger.fine("Deleted ${totalSize.toFileSize()}")
    }
  }
}

private class WatchedBy(val user: String, val watched: Boolean)

private fun Episode.toDisplayString(now: Instant): String {
  val time = DATE_FORMATTER.format(lastViewed)
  val show = showName.padEnd(30)
  val episode = "S${seasonNumber.pad()}E${episodeNumber.pad()}"
  val episodeName = name.padEnd(30)
  val daysAgo = Duration.between(lastViewed, now).toDays()
  return "$time: $show $episode - $episodeName  - Watched $daysAgo days ago"
}

private fun Int.pad(): String = toString().padStart(2, '0')