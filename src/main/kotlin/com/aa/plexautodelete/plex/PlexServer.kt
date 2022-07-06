package com.aa.plexautodelete.plex

import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.File
import java.net.URL
import java.time.Instant
import java.util.logging.Logger
import javax.xml.parsers.DocumentBuilderFactory

internal class PlexServer(private val baseUrl: URL, private val token: String, val logger: Logger? = null) {
    private var documentBuilder =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().apply { setErrorHandler(StandardErrorHandler) }

    constructor(baseUrl: String, token: String) : this(URL(baseUrl), token)

    fun getWatchedEpisodes(sections: Set<String>, token: String, daysAgo: Long): List<Episode> {
        val cutoffTime = Instant.now() - java.time.Duration.ofDays(daysAgo)
        val sectionKeys =
            getSections(token).filter { it.isShowSection() && it.getTitle() in sections }.map { it.getKey() }
        return sectionKeys.flatMap { sectionKey ->
            getDirectories("/library/sections/$sectionKey/all", token).flatMap { show ->
                getDirectories(show.getKey(), token).filter { it.getTitle() != "All episodes" }.flatMap { season ->
                    getVideos(season.getKey(), token).filter { it.isWatched(cutoffTime) }.map {
                        Episode(
                            it.getKey(),
                            it.getTitle(),
                            it.getViewedAt(),
                            it.getParentIndex(),
                            it.getIndex(),
                            it.getGrandparentTitle()
                        )
                    }
                }
            }
        }
    }

    fun isWatchedBy(key: String, userToken: String): Boolean {
        if (userToken.isEmpty()) {
            throw IllegalArgumentException("Missing user token")
        }
        return getVideo(key, userToken).isWatched()
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

    private fun loadDocument(path: String, token: String = this.token): Document {
        val connection = URL("$baseUrl$path?X-Plex-Token=$token").openConnection()
        return documentBuilder.parse(connection.getInputStream())
    }

}

private fun Document.getDirectories(): List<Node> = getNamedNodes("Directory")

private fun Document.getVideos(): List<Node> = getNamedNodes("Video")

private fun Document.getNamedNodes(name: String): List<Node> = firstChild.children().filter { it.nodeName == name }

private fun Node.children() = (0 until childNodes.length).map { childNodes.item(it) }

private fun Node.isWatched() = getIntAttr("viewCount") > 0

private fun Node.isWatched(time: Instant) = isWatched() && getViewedAt() < time

private fun Node.getKey() = getStringAttr("key")

private fun Node.getTitle() = getStringAttr("title")

private fun Node.getIndex() = getIntAttr("index")

private fun Node.getParentIndex() = getIntAttr("parentIndex")

private fun Node.getGrandparentTitle() = getStringAttr("grandparentTitle")

private fun Node.getFile() = getStringAttr("file")

private fun Node.getViewedAt(): Instant = Instant.ofEpochSecond(getIntAttr("lastViewedAt").toLong())

private fun Node.isShowSection() = getStringAttr("type") == "show"

private fun Node.getStringAttr(name: String) = attributes.getNamedItem(name)?.textContent ?: ""

private fun Node.getIntAttr(name: String) = getStringAttr(name).toIntOrNull() ?: 0

