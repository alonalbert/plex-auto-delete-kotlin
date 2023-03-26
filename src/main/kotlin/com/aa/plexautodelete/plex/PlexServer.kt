package com.aa.plexautodelete.plex

import com.aa.plexautodelete.config.User
import com.aa.plexautodelete.util.isExcluded
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.File
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.logging.Logger
import javax.xml.parsers.DocumentBuilderFactory

internal class PlexServer(private val baseUrl: URL, private val logger: Logger) {
  private var documentBuilder =
    DocumentBuilderFactory.newInstance().newDocumentBuilder().apply { setErrorHandler(StandardErrorHandler(logger)) }

  constructor(baseUrl: String, logger: Logger) : this(URL(baseUrl), logger)

  fun getWatchedEpisodes(sections: Set<String>, user: User, daysAgo: Long): List<Episode> {
    val token = user.plexToken.ifEmpty {
      throw IllegalArgumentException("Missing user token (${user.name})")
    }
    val cutoffTime = Instant.now() - Duration.ofDays(daysAgo)
    val sectionKeys = getSections(token).filter { it.isShowSection() && it.getTitle() in sections }.map { it.getKey() }
    return sectionKeys.flatMap { sectionKey ->
      getDirectories("/library/sections/$sectionKey/all", token).flatMap { show ->
        val showIsExcluded = user.shows.isExcluded(show.getTitle())
        if (showIsExcluded) {
          markShowWatched(show, user.plexToken)
        }
        getDirectories(show.getKey(), token).filter { it.getTitle() != "All episodes" }.flatMap { season ->
          getVideos(season.getKey(), token).filter { it.isWatched(cutoffTime) || showIsExcluded }.map {
            Episode(it.getKey(), it.getTitle(), it.getViewedAt(), it.getParentIndex(), it.getIndex(), it.getGrandparentTitle())
          }
        }
      }
    }
  }

  fun markExcludedShowsWatched(sections: Set<String>, users: List<User>, interactive: Boolean) {
    var askUser = interactive
    users.forEach { user ->
      val userName = user.name
      val token = user.plexToken.ifEmpty {
        throw IllegalArgumentException("Missing user token ($userName)")
      }
      var userHeaderPrinted = false
      val sectionKeys = getSections(token).filter { it.isShowSection() && it.getTitle() in sections }.map { it.getKey() }
      sectionKeys.forEach { sectionKey ->
        getDirectories("/library/sections/$sectionKey/all", token).forEach show@{ show ->
          val showIsExcluded = user.shows.isExcluded(show.getTitle())
          if (showIsExcluded) {
            val numUnwatched = getDirectories(show.getKey(), token).sumOf { season ->
              getVideos(season.getKey(), token).count { video -> video.isUnWatched() }
            }
            if (numUnwatched == 0) {
              return@show
            }
            val markAsWatched = if (askUser) {
              if (!userHeaderPrinted) {
                logger.info("Marking unwatched shows for user $userName: ")
                userHeaderPrinted = true
              }
              print(" Mark ${show.getTitle()} as watched for $userName? [y]es, [n]o, [A]ll, [N]one: [n]: ")
              when (Scanner(System.`in`).nextLine()) {
                "y" -> true
                "A" -> true.apply { askUser = false }
                "N" -> return
                else -> false
              }
            } else {
              true
            }
            if (markAsWatched) {
              markShowWatched(show, user.plexToken)
              logger.info("   ${show.getTitle()} marked watched for $userName")
            }
          }
        }
      }
    }
  }

  private fun markShowWatched(show: Node, token: String) {
    val ratingKey = show.getRatingKey()
    val url = URL("$baseUrl/:/scrobble?key=$ratingKey&identifier=com.plexapp.plugins.library&X-Plex-Token=$token")
    url.content
  }

  fun isWatchedBy(key: String, userToken: String, daysAgo: Long): Boolean {
    if (userToken.isEmpty()) {
      throw IllegalArgumentException("Missing user token")
    }
    return getVideo(key, userToken).isWatched(Instant.now() - Duration.ofDays(daysAgo))
  }

  fun getFiles(key: String, token: String) = getParts(key, token).map { File(it.getFile()) }

  private fun getParts(key: String, token: String): List<Node> {
    val video = getVideo(key, token)
    val media = video.children().first { it.nodeName == "Media" }
    return media.children().filter { it.nodeName == "Part" }
  }

  private fun getSections(token: String) = getDirectories("/library/sections", token)

  private fun getDirectories(path: String, token: String) = loadDocument(path, token).getDirectories()

  private fun getVideos(path: String, token: String) = loadDocument(path, token).getVideos()

  private fun getVideo(path: String, token: String) = loadDocument(path, token).getVideos().first()

  private fun loadDocument(path: String, token: String): Document {
    val connection = URL("$baseUrl$path?X-Plex-Token=$token").openConnection()
    return documentBuilder.parse(connection.getInputStream())
  }
}

private fun Document.getDirectories(): List<Node> = getNamedNodes("Directory")

private fun Document.getVideos(): List<Node> = getNamedNodes("Video")

private fun Document.getNamedNodes(name: String): List<Node> = firstChild.children().filter { it.nodeName == name }

private fun Node.children() = (0 until childNodes.length).map { childNodes.item(it) }

private fun Node.isUnWatched() = !isWatched()

private fun Node.isWatched() = getIntAttr("viewCount") > 0

private fun Node.isWatched(time: Instant) = isWatched() && getViewedAt() < time && getAddedAt() < time

private fun Node.getKey() = getStringAttr("key")

private fun Node.getRatingKey() = getStringAttr("ratingKey")

private fun Node.getTitle() = getStringAttr("title")

private fun Node.getIndex() = getIntAttr("index")

private fun Node.getParentIndex() = getIntAttr("parentIndex")

private fun Node.getGrandparentTitle() = getStringAttr("grandparentTitle")

private fun Node.getFile() = getStringAttr("file")

private fun Node.getViewedAt(): Instant = Instant.ofEpochSecond(getIntAttr("lastViewedAt").toLong())

private fun Node.getAddedAt(): Instant = Instant.ofEpochSecond(getIntAttr("AddedAt").toLong())

private fun Node.isShowSection() = getStringAttr("type") == "show"

private fun Node.getStringAttr(name: String) = attributes.getNamedItem(name)?.textContent ?: ""

private fun Node.getIntAttr(name: String) = getStringAttr(name).toIntOrNull() ?: 0
