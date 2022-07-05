package com.aa.plexautodelete.plex

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.helpers.DefaultHandler
import java.net.URL
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory

private var DOCUMENT_BUILDER = DocumentBuilderFactory.newInstance().apply {
  isValidating = true
  isIgnoringElementContentWhitespace = true
}.newDocumentBuilder().apply {
  setErrorHandler(DefaultHandler())
}

class PlexServer(private val baseUrl: URL, private val token: String) {

  constructor(baseUrl: String, token: String) : this(URL(baseUrl), token)

  fun getWatchedEpisodes(sections: Set<String>, token: String, daysAgo: Long): List<Episode> {
    val cutoffTime = Instant.now() - java.time.Duration.ofDays(daysAgo)
    val sectionKeys = getSections(token).filter { it.isShowSection() && it.getTitle() in sections }.map { it.getKey() }
    return sectionKeys.flatMap { sectionKey ->
      getDirectories("/library/sections/$sectionKey/all", token).flatMap { show ->
        getDirectories(show.getKey(), token).filter { it.getTitle() != "All episodes" }.flatMap { season ->
          getVideos(season.getKey(), token).filter { it.isWatched(cutoffTime) }.map {
            Episode(it.getKey(), it.getTitle(), it.getViewedAt(), it.getParentIndex(), it.getIndex(), it.getGrandparentTitle())
          }
        }
      }
    }
  }

  fun isEpisodeWatchedBy(episodeKey: String, userToken: String) = getVideos(episodeKey, userToken)[0].isWatched()

  private fun getSections(token: String) = getDirectories("/library/sections", token)

  private fun getDirectories(path: String, token: String) = loadDocument(path, token).getDirectories()

  private fun getVideos(path: String, token: String) = loadDocument(path, token).getVideos()

  private fun loadDocument(path: String, token: String = this.token): Document {
    val connection = URL("$baseUrl$path?X-Plex-Token=$token").openConnection()
    return DOCUMENT_BUILDER.parse(connection.getInputStream())
  }
}

private fun Document.getDirectories(): List<Node> = getNamedNodes("Directory")

private fun Document.getVideos(): List<Node> = getNamedNodes("Video")

private fun Document.getNamedNodes(name: String): List<Node> = firstChild.childNodes.items().filter { it.nodeName == name }

private fun NodeList.items() = (0 until length).map { item(it) }

private fun Node.isWatched() = getIntAttr("viewCount") > 0

private fun Node.isWatched(time: Instant) = isWatched() && getViewedAt() < time

private fun Node.getKey() = getStringAttr("key")

private fun Node.getTitle() = getStringAttr("title")

private fun Node.getIndex() = getIntAttr("index")

private fun Node.getParentIndex() = getIntAttr("parentIndex")

private fun Node.getGrandparentTitle() = getStringAttr("grandparentTitle")

private fun Node.getViewedAt(): Instant = Instant.ofEpochSecond(getIntAttr("lastViewedAt").toLong())

private fun Node.isShowSection() = getStringAttr("type") == "show"

private fun Node.getStringAttr(name: String) = attributes.getNamedItem(name)?.textContent ?: ""

private fun Node.getIntAttr(name: String) = getStringAttr(name).toIntOrNull() ?: 0

