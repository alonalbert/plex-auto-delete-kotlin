package com.aa.plexautodelete

import com.aa.plexautodelete.config.Config
import com.aa.plexautodelete.plex.Episode
import com.aa.plexautodelete.plex.PlexServer
import com.google.gson.Gson
import java.io.FileReader
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())


fun main(vararg args: String) {
  val configFile = if (args.isNotEmpty()) args[0] else "${System.getProperty("user.home")}/.plex-auto-delete-config"
  val config = Gson().fromJson(FileReader(configFile), Config::class.java)

  val server = PlexServer(config.plexUrl, config.plexToken)
  val watchedEpisodes = server.getWatchedEpisodes(config.tvSections, config.plexToken, config.days)

  val now = Instant.now()
  val episodesToDelete = watchedEpisodes.mapNotNull { episode ->
    println("Deletion candidates:")
    println("  ${episode.toDisplayString()} - Watched ${Duration.between(now, episode.lastViewed).toDays()} days ago")

    val unwatchedBy = config.users.filter { it.shows.contains(episode.showName) }.map { user ->
      WatchedBy(user.name, server.isEpisodeWatchedBy(episode.key, user.plexToken))
    }.filter { !it.watched }.map { it.user }
    if (unwatchedBy.isNotEmpty()) {
      println("    Unwatched by ${unwatchedBy.joinToString()}")
      null
    } else {
      episode
    }
  }

  if (episodesToDelete.isEmpty()) {
    println("Nothing to delete")
  } else {
    println("Deleting episodes:")
    episodesToDelete.forEach {
      println("  ${it.toDisplayString()} - Watched ${Duration.between(now, it.lastViewed).toDays()} days ago")
    }
  }
}

private class WatchedBy(val user: String, val watched: Boolean)

private fun Episode.toDisplayString() = "${DATE_FORMATTER.format(lastViewed)}: S${seasonNumber.pad()}E${episodeNumber.pad()} - $name"

private fun Int.pad(): String = toString().padStart(2, '0')